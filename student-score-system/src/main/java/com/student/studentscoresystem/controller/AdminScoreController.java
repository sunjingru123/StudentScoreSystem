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


    public AdminScoreController(
            ScoreRecordMapper scoreRecordMapper,
            ScoreProjectNameResolver scoreProjectNameResolver,
            SysUserMapper sysUserMapper,
            ScoreRecordOperationLogMapper operationLogMapper,
            ScoreModifyLogMapper scoreModifyLogMapper
    ) {
        this.scoreRecordMapper = scoreRecordMapper;
        this.scoreProjectNameResolver = scoreProjectNameResolver;
        this.sysUserMapper = sysUserMapper;
        this.operationLogMapper = operationLogMapper;
        this.scoreModifyLogMapper = scoreModifyLogMapper;
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        return userId == null ? null : Long.valueOf(userId.toString());
    }


    /**
     * 查询某个学生的全部成绩明细
     * 管理员专用
     * 注意：这里不过滤adminHidden，管理员需要看到已经隐藏的记录
     */
    @GetMapping("/student/{studentId}")
    @RequireRole("管理员")
    public Result<List<AdminScoreDetailVO>> studentScore(
            @PathVariable Long studentId
    ) {

        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null) {
            return Result.error("学生不存在");
        }

        List<ScoreRecord> records = scoreRecordMapper.selectList(
                new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getStudentId, studentId)
                        .orderByDesc(ScoreRecord::getCreateTime)
        );

        List<AdminScoreDetailVO> result = new ArrayList<>();

        for (ScoreRecord record : records) {
            AdminScoreDetailVO vo = new AdminScoreDetailVO();

            vo.setId(record.getId());
            vo.setStudentId(student.getId());
            vo.setStudentName(student.getRealName());
            vo.setStudentNo(student.getStudentNo());
            vo.setClassName(student.getClassName());

            vo.setRuleName(scoreProjectNameResolver.resolve(record));

            vo.setScore(record.getScore());
            vo.setSourceType(record.getSourceType());
            vo.setSourceName(scoreProjectNameResolver.resolveSourceLabel(record));
            vo.setSourceId(record.getSourceId());
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
     * 查询学生当前总成绩
     * 管理员看到的是全部记录，包括已经隐藏的记录
     */
    @GetMapping("/student/{studentId}/total")
    @RequireRole("管理员")
    public Result<BigDecimal> total(
            @PathVariable Long studentId
    ) {
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
