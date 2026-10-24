package com.student.studentscoresystem.controller;

import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.ScoreDeleteApplyAuditDTO;
import com.student.studentscoresystem.dto.ScoreDeleteApplySubmitDTO;
import com.student.studentscoresystem.service.ScoreDeleteApplyService;
import com.student.studentscoresystem.service.ScoreManagerPermissionService;
import com.student.studentscoresystem.vo.ScoreDeleteApplyVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * =========================================================
 * 成绩删除申请
 * =========================================================
 *
 * 档案部部长 / 副部长删除学生加减分时，先提交删除申请，
 * 由档案部负责辅导员 / 高级辅导员审核通过后才真正删除。
 *
 * 所有权限均以后端判定为准（不依赖前端路由或按钮显示）。
 * =========================================================
 */
@RestController
@RequestMapping("/scoreDeleteApply")
public class ScoreDeleteApplyController {

    private final ScoreDeleteApplyService scoreDeleteApplyService;

    private final ScoreManagerPermissionService permissionService;


    public ScoreDeleteApplyController(
            ScoreDeleteApplyService scoreDeleteApplyService,
            ScoreManagerPermissionService permissionService
    ) {

        this.scoreDeleteApplyService = scoreDeleteApplyService;

        this.permissionService = permissionService;
    }


    private Long currentUserId(HttpServletRequest request) {

        Object userId = request.getAttribute("userId");

        return userId == null
                ? null
                : Long.valueOf(userId.toString());
    }


    /**
     * 当前用户与成绩删除相关的权限
     */
    @GetMapping("/permission")
    public Result<Map<String, Object>> permission(
            HttpServletRequest request) {

        Long userId = currentUserId(request);


        if (userId == null) {

            return Result.fail("请先登录");
        }


        Map<String, Object> data = new HashMap<>();

        data.put(
                "canSubmit",
                permissionService.canSubmitScoreDeleteApply(userId)
        );

        data.put(
                "canReview",
                permissionService.canReviewScoreDeleteApply(userId)
        );

        data.put(
                "canDirectDelete",
                permissionService.canDeleteScoreDirectly(userId)
        );


        return Result.success(data);
    }


    /**
     * 提交删除申请（档案部部长 / 副部长）
     */
    @PostMapping("/submit")
    public Result<Long> submit(
            @RequestBody ScoreDeleteApplySubmitDTO dto,
            HttpServletRequest request) {

        Long userId = currentUserId(request);


        if (userId == null) {

            return Result.fail("请先登录");
        }


        if (dto == null || dto.getRecordId() == null) {

            return Result.fail("请选择要删除的成绩");
        }


        try {

            Long applyId =
                    scoreDeleteApplyService.submitApply(
                            dto.getRecordId(),
                            userId,
                            dto.getReason()
                    );


            return Result.success(applyId);


        } catch (DuplicateKeyException e) {

            return Result.fail("该成绩已有一条待审核的删除申请");

        } catch (SecurityException | IllegalArgumentException e) {

            return Result.fail(e.getMessage());

        } catch (IllegalStateException e) {

            return Result.error(e.getMessage());
        }
    }


    /**
     * 我的删除申请
     */
    @GetMapping("/mine")
    public Result<List<ScoreDeleteApplyVO>> mine(
            HttpServletRequest request) {

        Long userId = currentUserId(request);


        if (userId == null) {

            return Result.fail("请先登录");
        }


        try {

            return Result.success(
                    scoreDeleteApplyService.listMine(userId)
            );

        } catch (SecurityException | IllegalArgumentException e) {

            return Result.fail(e.getMessage());
        }
    }


    /**
     * 待我审核
     */
    @GetMapping("/pending")
    public Result<List<ScoreDeleteApplyVO>> pending(
            HttpServletRequest request) {

        Long userId = currentUserId(request);


        if (userId == null) {

            return Result.fail("请先登录");
        }


        try {

            return Result.success(
                    scoreDeleteApplyService.listPending(userId)
            );

        } catch (SecurityException | IllegalArgumentException e) {

            return Result.fail(e.getMessage());
        }
    }


    /**
     * 我已处理
     */
    @GetMapping("/processed")
    public Result<List<ScoreDeleteApplyVO>> processed(
            HttpServletRequest request) {

        Long userId = currentUserId(request);


        if (userId == null) {

            return Result.fail("请先登录");
        }


        try {

            return Result.success(
                    scoreDeleteApplyService.listProcessed(userId)
            );

        } catch (SecurityException | IllegalArgumentException e) {

            return Result.fail(e.getMessage());
        }
    }


    /**
     * 审核通过（执行删除）
     */
    @PutMapping("/approve/{id}")
    public Result<Void> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ScoreDeleteApplyAuditDTO dto,
            HttpServletRequest request) {

        Long userId = currentUserId(request);


        if (userId == null) {

            return Result.fail("请先登录");
        }


        try {

            scoreDeleteApplyService.approve(
                    id,
                    userId,
                    dto == null ? null : dto.getRemark()
            );


            return Result.success(null);

        } catch (SecurityException | IllegalArgumentException e) {

            return Result.fail(e.getMessage());

        } catch (IllegalStateException e) {

            return Result.error(e.getMessage());
        }
    }


    /**
     * 审核驳回
     */
    @PutMapping("/reject/{id}")
    public Result<Void> reject(
            @PathVariable Long id,
            @RequestBody(required = false) ScoreDeleteApplyAuditDTO dto,
            HttpServletRequest request) {

        Long userId = currentUserId(request);


        if (userId == null) {

            return Result.fail("请先登录");
        }


        try {

            scoreDeleteApplyService.reject(
                    id,
                    userId,
                    dto == null ? null : dto.getRemark()
            );


            return Result.success(null);

        } catch (SecurityException | IllegalArgumentException e) {

            return Result.fail(e.getMessage());

        } catch (IllegalStateException e) {

            return Result.error(e.getMessage());
        }
    }
}
