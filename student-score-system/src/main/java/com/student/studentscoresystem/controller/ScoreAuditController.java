package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.annotation.RequireRole;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.ScoreApply;
import com.student.studentscoresystem.entity.ScoreAudit;
import com.student.studentscoresystem.service.IScoreApplyService;
import com.student.studentscoresystem.service.IScoreAuditService;
import com.student.studentscoresystem.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/scoreAudit")
public class ScoreAuditController {

    private final IScoreAuditService scoreAuditService;

    private final IScoreApplyService scoreApplyService;

    public ScoreAuditController(
            IScoreAuditService scoreAuditService,
            IScoreApplyService scoreApplyService
    ) {

        this.scoreAuditService =
                scoreAuditService;

        this.scoreApplyService =
                scoreApplyService;
    }

    /**
     * =========================================================
     * 获取当前登录用户 ID
     * =========================================================
     */
    private Long getCurrentUserId(
            HttpServletRequest request
    ) {

        String token =
                request.getHeader("Authorization");

        if (
                token == null
                        || !token.startsWith("Bearer ")
        ) {
            return null;
        }

        token = token.substring(7);

        try {

            Claims claims =
                    JwtUtil.parseToken(token);

            return claims.get(
                    "userId",
                    Long.class
            );

        } catch (Exception e) {

            return null;
        }
    }

    /**
     * =========================================================
     * 查询待审核列表
     *
     * 只查询 status = 0 的申请。
     * =========================================================
     */
    @GetMapping("/pending")
    @RequireRole({"管理员", "辅导员", "部长", "副部长", "干事"})
    public Result<List<ScoreApply>> pending(
            HttpServletRequest request
    ) {

        Long currentUserId =
                getCurrentUserId(request);

        if (currentUserId == null) {
            return Result.fail("请先登录");
        }

        List<ScoreApply> list =
                scoreApplyService.list(
                        new LambdaQueryWrapper<ScoreApply>()
                                .eq(
                                        ScoreApply::getStatus,
                                        (short) 0
                                )
                                .orderByDesc(
                                        ScoreApply::getCreateTime
                                )
                );

        return Result.success(list);
    }

    /**
     * =========================================================
     * 审核申请
     *
     * 真正的多表写操作已下沉到 IScoreAuditService，
     * 由其在同一个事务内完成并回滚。
     *
     * 这里只负责：
     *
     * 1. 获取当前审核人
     * 2. 调用服务
     * 3. 把业务提示转换成统一响应
     * =========================================================
     */
    @PutMapping("/audit")
    @RequireRole({"管理员", "辅导员", "部长", "副部长", "干事"})
    public Result<Void> audit(
            @RequestBody ScoreAudit dto,
            HttpServletRequest request
    ) {

        Long auditorId =
                getCurrentUserId(request);

        if (auditorId == null) {

            return Result.fail("请先登录");
        }

        try {

            String message =
                    scoreAuditService.auditApply(
                            dto,
                            auditorId
                    );

            if (message != null) {

                return Result.fail(message);
            }

            return Result.success(null);

        } catch (DuplicateKeyException e) {

            /*
             * 并发场景：
             *
             * 另一个请求已经为同一申请生成成绩记录，
             * 唯一索引 uk_score_record_source 拦截了重复写入，
             * 事务整体回滚，这里做幂等提示。
             */
            return Result.fail(
                    "该申请已生成成绩，请勿重复审核"
            );
        }
    }

}
