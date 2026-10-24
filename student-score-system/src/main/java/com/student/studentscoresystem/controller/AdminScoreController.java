package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.common.ScoreCalculator;
import com.student.studentscoresystem.annotation.RequireRole;
import com.student.studentscoresystem.dto.ScoreRecordOperationDTO;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.ScoreRecordOperationLog;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.ScoreModifyLog;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.ScoreRecordOperationLogMapper;
import com.student.studentscoresystem.mapper.ScoreModifyLogMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.ScoreProjectNameResolver;
import com.student.studentscoresystem.service.ScoreManagerPermissionService;
import com.student.studentscoresystem.service.ScoreRecordManageService;
import com.student.studentscoresystem.vo.AdminScoreDetailVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/score")
public class AdminScoreController {

    private final ScoreRecordMapper scoreRecordMapper;
    private final ScoreProjectNameResolver scoreProjectNameResolver;
    private final SysUserMapper sysUserMapper;
    private final ScoreRecordOperationLogMapper operationLogMapper;
    private final ScoreModifyLogMapper scoreModifyLogMapper;
    private final ScoreManagerPermissionService scoreManagerPermissionService;
    private final ScoreRecordManageService scoreRecordManageService;


    public AdminScoreController(
            ScoreRecordMapper scoreRecordMapper,
            ScoreProjectNameResolver scoreProjectNameResolver,
            SysUserMapper sysUserMapper,
            ScoreRecordOperationLogMapper operationLogMapper,
            ScoreModifyLogMapper scoreModifyLogMapper,
            ScoreManagerPermissionService scoreManagerPermissionService,
            ScoreRecordManageService scoreRecordManageService
    ) {
        this.scoreRecordMapper = scoreRecordMapper;
        this.scoreProjectNameResolver = scoreProjectNameResolver;
        this.sysUserMapper = sysUserMapper;
        this.operationLogMapper = operationLogMapper;
        this.scoreModifyLogMapper = scoreModifyLogMapper;
        this.scoreManagerPermissionService = scoreManagerPermissionService;
        this.scoreRecordManageService = scoreRecordManageService;
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        return userId == null ? null : Long.valueOf(userId.toString());
    }

    /**
     * 是否有权查看 / 维护全部学生成绩
     *
     * 管理员 / 档案部部长 / 副部长 /
     * 档案部负责辅导员 / 高级辅导员
     */
    private boolean canManageStudentScores(Long userId) {
        return scoreManagerPermissionService.canManageStudentScores(userId);
    }


    /**
     * 查询某个学生的全部成绩明细
     *
     * 管理员 / 档案部部长 / 档案部副部长可用
     *
     * 注意：这里不过滤adminHidden，管理员需要看到已经隐藏 / 删除的记录
     */
    @GetMapping("/student/{studentId}")
    public Result<List<AdminScoreDetailVO>> studentScore(
            @PathVariable Long studentId,
            HttpServletRequest request
    ) {

        Long operatorId = getCurrentUserId(request);

        if (operatorId == null) {
            return Result.fail("请先登录");
        }

        if (!canManageStudentScores(operatorId)) {
            return Result.fail("没有权限查看学生成绩");
        }

        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null) {
            return Result.error("学生不存在");
        }

        List<ScoreRecord> records = scoreRecordMapper.selectList(
                new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getStudentId, studentId)
                        .orderByDesc(ScoreRecord::getCreateTime)
        );

        ScoreProjectNameResolver.PreloadedNames names =
                scoreProjectNameResolver.preload(records);

        List<AdminScoreDetailVO> result = new ArrayList<>();

        for (ScoreRecord record : records) {
            AdminScoreDetailVO vo = new AdminScoreDetailVO();

            vo.setId(record.getId());
            vo.setStudentId(student.getId());
            vo.setStudentName(student.getRealName());
            vo.setStudentNo(student.getStudentNo());
            vo.setClassName(student.getClassName());

            vo.setRuleName(names.resolve(record));

            vo.setScore(record.getScore());
            vo.setSourceType(record.getSourceType());
            vo.setSourceName(names.resolveSourceLabel(record));
            vo.setSourceId(record.getSourceId());
            vo.setStatus(record.getStatus());
            vo.setAdminHidden(record.getAdminHidden());
            vo.setCreateTime(record.getCreateTime());

            result.add(vo);
        }
        return Result.success(result);
    }


    /**
     * 隐藏某一条成绩记录
     * 管理员专用，同时写入操作日志
     */
    @PutMapping("/hide/{id}")
    @RequireRole("管理员")
    public Result<Void> hide(
            @PathVariable Long id,
            @RequestBody(required = false) ScoreRecordOperationDTO dto,
            HttpServletRequest request
    ) {
        ScoreRecord record = scoreRecordMapper.selectById(id);
        if (record == null) {
            return Result.error("成绩记录不存在");
        }
        if (record.getAdminHidden() != null && record.getAdminHidden() == 1) {
            return Result.error("该成绩已经隐藏");
        }

        Long operatorId = getCurrentUserId(request);
        if (operatorId == null) {
            return Result.error("请先登录");
        }
        ScoreRecordOperationLog log = new ScoreRecordOperationLog();
        log.setScoreRecordId(id);
        log.setOperatorId(operatorId);
        log.setOperation("HIDE");
        if (dto != null) {
            log.setReason(dto.getReason());
        }
        log.setCreateTime(LocalDateTime.now());
        record.setAdminHidden((short) 1);
        scoreRecordMapper.updateById(record);
        operationLogMapper.insert(log);

        return Result.success(null);
    }


    /**
     * 恢复某一条成绩记录
     * 管理员专用，同时写入操作日志
     */
    @PutMapping("/show/{id}")
    @RequireRole("管理员")
    public Result<Void> show(
            @PathVariable Long id,
            @RequestBody(required = false) ScoreRecordOperationDTO dto,
            HttpServletRequest request
    ) {
        ScoreRecord record = scoreRecordMapper.selectById(id);
        if (record == null) {
            return Result.error("成绩记录不存在");
        }
        if (record.getAdminHidden() != null && record.getAdminHidden() == 0) {
            return Result.error("该成绩已经是正常状态");
        }

        Long operatorId = getCurrentUserId(request);
        if (operatorId == null) {
            return Result.error("请先登录");
        }
        ScoreRecordOperationLog log = new ScoreRecordOperationLog();
        log.setScoreRecordId(id);
        log.setOperatorId(operatorId);
        log.setOperation("RESTORE");
        if (dto != null) {
            log.setReason(dto.getReason());
        }
        log.setCreateTime(LocalDateTime.now());
        record.setAdminHidden((short) 0);
        scoreRecordMapper.updateById(record);
        operationLogMapper.insert(log);

        return Result.success(null);
    }


    /**
     * =========================================================
     * 删除某一条学生成绩记录
     * =========================================================
     *
     * DELETE /admin/score/record/{id}
     *
     * 权限：
     *   管理员 / 档案部部长 / 档案部副部长
     *
     * 删除 = 逻辑删除（status = 0，admin_hidden = 1）：
     *   学生端、辅导员端、统计、导出均不再包含该加减分记录，
     *   同时保留审计日志与历史数据，可恢复。
     *
     * 直接删除权限：
     *   管理员 / 高级辅导员 / 档案部负责辅导员。
     *
     * 档案部部长 / 副部长不再直接删除，
     * 需通过 /scoreDeleteApply/submit 提交删除申请，
     * 由上述辅导员审核通过后执行。
     */
    @DeleteMapping("/record/{id}")
    public Result<Void> deleteRecord(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            HttpServletRequest request
    ) {

        Long operatorId = getCurrentUserId(request);

        if (operatorId == null) {
            return Result.fail("请先登录");
        }

        if (!scoreManagerPermissionService.canDeleteScoreDirectly(operatorId)) {
            return Result.fail(
                    "档案部成员删除学生成绩需要提交删除申请，经辅导员审核通过后执行"
            );
        }

        try {

            scoreRecordManageService.deleteRecord(
                    id,
                    operatorId,
                    reason
            );

            return Result.success(null);

        } catch (SecurityException | IllegalArgumentException e) {

            return Result.fail(e.getMessage());

        } catch (IllegalStateException e) {

            return Result.error(e.getMessage());
        }
    }


    /**
     * =========================================================
     * 恢复某一条被删除 / 隐藏的学生成绩记录
     * =========================================================
     *
     * PUT /admin/score/record/{id}/restore
     *
     * 权限：
     *   管理员 / 档案部部长 / 档案部副部长
     */
    @PutMapping("/record/{id}/restore")
    public Result<Void> restoreRecord(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            HttpServletRequest request
    ) {

        Long operatorId = getCurrentUserId(request);

        try {

            scoreRecordManageService.restoreRecord(
                    id,
                    operatorId,
                    reason
            );

            return Result.success(null);

        } catch (SecurityException | IllegalArgumentException e) {

            return Result.fail(e.getMessage());

        } catch (IllegalStateException e) {

            return Result.error(e.getMessage());
        }
    }


    /**
     * 查询学生当前总成绩
     *
     * 管理员 / 档案部部长 / 档案部副部长可用
     *
     * 看到的是全部未隐藏记录，
     * 已删除 / 已隐藏记录不参与统计。
     */
    @GetMapping("/student/{studentId}/total")
    public Result<BigDecimal> total(
            @PathVariable Long studentId,
            HttpServletRequest request
    ) {

        Long operatorId = getCurrentUserId(request);

        if (operatorId == null) {
            return Result.fail("请先登录");
        }

        if (!canManageStudentScores(operatorId)) {
            return Result.fail("没有权限查看学生成绩");
        }

        List<ScoreRecord> records = scoreRecordMapper.selectList(
                new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getStudentId, studentId)
                        .eq(ScoreRecord::getAdminHidden, (short) 0)
        );

        ScoreCalculator.Summary summary =
                ScoreCalculator.summarize(
                        records.stream()
                                .map(ScoreRecord::getScore)
                                .toList()
                );

        return Result.success(summary.getTotalScore());
    }

    @PutMapping("/correction/{recordId}")
    @RequireRole("管理员")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> correctScore(
            @PathVariable Long recordId,
            @RequestBody ScoreCorrectionRequest correction,
            HttpServletRequest request
    ) {
        if (correction == null || correction.getNewScore() == null) {
            return Result.error("新成绩不能为空");
        }
        if (correction.getReason() == null || correction.getReason().trim().isEmpty()) {
            return Result.error("更正原因不能为空");
        }
        Long operatorId = getCurrentUserId(request);
        if (operatorId == null) {
            return Result.error("请先登录");
        }

        ScoreRecord record = scoreRecordMapper.selectById(recordId);
        if (record == null) {
            return Result.error("成绩记录不存在");
        }
        if (record.getScore() == null) {
            return Result.error("原成绩无效，无法更正");
        }

        ScoreModifyLog log = new ScoreModifyLog();
        log.setRecordId(recordId);
        log.setOldScore(record.getScore());
        log.setNewScore(correction.getNewScore());
        log.setModifierId(operatorId);
        log.setReason(correction.getReason().trim());
        log.setCreateTime(LocalDateTime.now());

        record.setScore(correction.getNewScore());
        if (scoreRecordMapper.updateById(record) != 1) {
            throw new IllegalStateException("成绩更正失败");
        }
        if (scoreModifyLogMapper.insert(log) != 1) {
            throw new IllegalStateException("成绩更正审计日志写入失败");
        }
        return Result.success(null);
    }

    public static class ScoreCorrectionRequest {
        private BigDecimal newScore;
        private String reason;

        public BigDecimal getNewScore() { return newScore; }
        public void setNewScore(BigDecimal newScore) { this.newScore = newScore; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
