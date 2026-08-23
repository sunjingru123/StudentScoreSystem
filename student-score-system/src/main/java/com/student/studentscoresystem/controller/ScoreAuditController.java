package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.NoticeMessage;
import com.student.studentscoresystem.entity.ScoreApply;
import com.student.studentscoresystem.entity.ScoreAudit;
import com.student.studentscoresystem.entity.ScoreFlow;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.mapper.NoticeMessageMapper;
import com.student.studentscoresystem.service.IScoreApplyService;
import com.student.studentscoresystem.service.IScoreAuditService;
import com.student.studentscoresystem.service.IScoreFlowService;
import com.student.studentscoresystem.service.IScoreRecordService;
import com.student.studentscoresystem.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/scoreAudit")
public class ScoreAuditController {

    private final IScoreAuditService scoreAuditService;

    private final IScoreApplyService scoreApplyService;

    private final IScoreRecordService scoreRecordService;

    private final IScoreFlowService scoreFlowService;

    private final NoticeMessageMapper noticeMessageMapper;

    public ScoreAuditController(
            IScoreAuditService scoreAuditService,
            IScoreApplyService scoreApplyService,
            IScoreRecordService scoreRecordService,
            IScoreFlowService scoreFlowService,
            NoticeMessageMapper noticeMessageMapper
    ) {
        this.scoreAuditService = scoreAuditService;
        this.scoreApplyService = scoreApplyService;
        this.scoreRecordService = scoreRecordService;
        this.scoreFlowService = scoreFlowService;
        this.noticeMessageMapper = noticeMessageMapper;
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
     * =========================================================
     *
     * 只查询：
     *
     * status = 0
     *
     * 的申请。
     */
    @GetMapping("/pending")
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
     * =========================================================
     *
     * status：
     *
     * 1 = 通过
     * 2 = 驳回
     *
     * 审核人：
     *
     * 从 JWT 中获取。
     *
     * 不再写死 1L。
     */
    @PutMapping("/audit")
    public Result<Void> audit(
            @RequestBody ScoreAudit dto,
            HttpServletRequest request
    ) {

        /*
         * =====================================================
         * 1. 获取当前审核人
         * =====================================================
         */

        Long auditorId =
                getCurrentUserId(request);

        if (auditorId == null) {
            return Result.fail("请先登录");
        }

        /*
         * =====================================================
         * 2. 参数校验
         * =====================================================
         */

        if (dto == null) {
            return Result.fail("审核参数不能为空");
        }

        if (dto.getApplyId() == null) {
            return Result.fail("申请ID不能为空");
        }

        if (
                dto.getAuditStatus() == null
                        || (
                        dto.getAuditStatus() != 1
                                && dto.getAuditStatus() != 2
                )
        ) {
            return Result.fail("审核状态错误");
        }

        /*
         * =====================================================
         * 3. 查询申请
         * =====================================================
         */

        ScoreApply apply =
                scoreApplyService.getById(
                        dto.getApplyId()
                );

        if (apply == null) {
            return Result.fail("申请不存在");
        }

        /*
         * =====================================================
         * 4. 防止重复审核
         * =====================================================
         */

        if (
                !Short.valueOf((short) 0)
                        .equals(apply.getStatus())
        ) {
            return Result.fail(
                    "该申请已经审核，不可重复操作"
            );
        }

        /*
         * =====================================================
         * 5. 禁止自己审核自己的申请
         * =====================================================
         */

        if (
                auditorId.equals(
                        apply.getStudentId()
                )
        ) {
            return Result.fail(
                    "不能审核自己提交的申请"
            );
        }

        /*
         * =====================================================
         * 6. 保存审核记录
         * =====================================================
         */

        ScoreAudit audit =
                new ScoreAudit();

        audit.setApplyId(
                apply.getId()
        );

        /*
         * 当前登录审核人
         */
        audit.setAuditorId(
                auditorId
        );

        audit.setAuditStatus(
                dto.getAuditStatus()
        );

        audit.setAuditComment(
                dto.getAuditComment()
        );

        audit.setAuditTime(
                LocalDateTime.now()
        );

        scoreAuditService.save(audit);

        /*
         * =====================================================
         * 7. 修改申请状态
         * =====================================================
         */

        apply.setStatus(
                dto.getAuditStatus()
        );

        apply.setUpdateTime(
                LocalDateTime.now()
        );

        scoreApplyService.updateById(
                apply
        );

        /*
         * =====================================================
         * 8. 驳回
         * =====================================================
         */

        if (dto.getAuditStatus() == 2) {

            sendNotice(
                    apply,
                    false,
                    dto.getAuditComment(),
                    auditorId
            );

            return Result.success(null);
        }

        /*
         * =====================================================
         * 9. 审核通过
         * =====================================================
         */

        Long existRecord =
                scoreRecordService.count(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .eq(
                                        ScoreRecord::getSourceType,
                                        "SCORE_APPLY"
                                )
                                .eq(
                                        ScoreRecord::getSourceId,
                                        apply.getId()
                                )
                );

        /*
         * 如果已经生成过成绩记录
         * 直接返回，防止重复加分。
         */
        if (existRecord > 0) {

            sendNotice(
                    apply,
                    true,
                    null,
                    auditorId
            );

            return Result.success(null);
        }

        /*
         * =====================================================
         * 10. 创建 ScoreRecord
         * =====================================================
         */

        ScoreRecord record = new ScoreRecord();

        record.setStudentId(
                apply.getStudentId()
        );

        record.setRuleId(
                apply.getRuleId()
        );

        record.setScore(
                apply.getApplyScore()
        );

        record.setSemesterId(
                1L
        );

/**
 * 默认有效
 */
        record.setStatus(
                (short) 1
        );

/**
 * 默认不隐藏
 */
        record.setAdminHidden(
                (short) 0
        );

        record.setSourceType(
                "APPLY"
        );

        record.setSourceId(
                apply.getId()
        );

        record.setCreateTime(
                LocalDateTime.now()
        );

        scoreRecordService.save(record);
        /*
         * =====================================================
         * 11. 计算 ScoreFlow
         * =====================================================
         */

        BigDecimal beforeScore =
                scoreRecordService.list(
                                new LambdaQueryWrapper<ScoreRecord>()
                                        .eq(
                                                ScoreRecord::getStudentId,
                                                apply.getStudentId()
                                        )
                                        .eq(
                                                ScoreRecord::getAdminHidden,
                                                (short) 0
                                        )
                        )
                        .stream()
                        .map(
                                ScoreRecord::getScore
                        )
                        .filter(
                                score -> score != null
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        )
                        .subtract(
                                apply.getApplyScore()
                        );

        BigDecimal changeScore =
                apply.getApplyScore();

        BigDecimal afterScore =
                beforeScore.add(
                        changeScore
                );

        ScoreFlow flow =
                new ScoreFlow();

        flow.setStudentId(
                apply.getStudentId()
        );

        flow.setBeforeScore(
                beforeScore
        );

        flow.setChangeScore(
                changeScore
        );

        flow.setAfterScore(
                afterScore
        );

        flow.setChangeType(
                "SCORE_APPLY"
        );

        flow.setDescription(
                "自主申报审核通过"
        );

        scoreFlowService.save(
                flow
        );

        /*
         * =====================================================
         * 12. 发送通知
         * =====================================================
         */

        sendNotice(
                apply,
                true,
                null,
                auditorId
        );

        return Result.success(null);
    }

    /**
     * =========================================================
     * 发送审核通知
     * =========================================================
     */
    private void sendNotice(
            ScoreApply apply,
            boolean pass,
            String remark,
            Long senderId
    ) {

        NoticeMessage message =
                new NoticeMessage();

        message.setTitle(
                "综合测评审核通知"
        );

        if (pass) {

            message.setContent(
                    "你的综合测评申请已通过，获得"
                            + apply.getApplyScore()
                            + "分"
            );

        } else {

            String content =
                    "你的综合测评申请未通过";

            if (
                    remark != null
                            && !remark.trim().isEmpty()
            ) {

                content +=
                        "：" + remark.trim();
            }

            message.setContent(
                    content
            );
        }

        /*
         * 当前审核人
         */
        message.setSenderId(
                senderId
        );

        /*
         * 申请人
         */
        message.setReceiverId(
                apply.getStudentId()
        );

        // 修复：去掉(short)强转，传Integer类型0
        message.setReadStatus(0);

        noticeMessageMapper.insert(
                message
        );
    }
}