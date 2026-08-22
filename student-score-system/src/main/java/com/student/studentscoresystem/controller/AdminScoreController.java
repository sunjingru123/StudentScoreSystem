package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.ScoreRecordOperationDTO;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.ScoreRecordOperationLog;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.ScoreRecordOperationLogMapper;
import com.student.studentscoresystem.mapper.ScoreRuleMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.vo.AdminScoreDetailVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/score")
public class AdminScoreController {

    private final ScoreRecordMapper scoreRecordMapper;
    private final ScoreRuleMapper scoreRuleMapper;
    private final SysUserMapper sysUserMapper;
    private final ScoreRecordOperationLogMapper operationLogMapper;


    public AdminScoreController(
            ScoreRecordMapper scoreRecordMapper,
            ScoreRuleMapper scoreRuleMapper,
            SysUserMapper sysUserMapper,
            ScoreRecordOperationLogMapper operationLogMapper
    ) {
        this.scoreRecordMapper = scoreRecordMapper;
        this.scoreRuleMapper = scoreRuleMapper;
        this.sysUserMapper = sysUserMapper;
        this.operationLogMapper = operationLogMapper;
    }


    /**
     * 查询某个学生的全部成绩明细
     * 管理员专用
     * 注意：这里不过滤adminHidden，管理员需要看到已经隐藏的记录
     */
    @GetMapping("/student/{studentId}")
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

            ScoreRule rule = scoreRuleMapper.selectById(record.getRuleId());
            if (rule != null) {
                vo.setRuleName(rule.getName());
            }

            vo.setScore(record.getScore());
            vo.setSourceType(record.getSourceType());
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

        record.setAdminHidden((short) 1);
        scoreRecordMapper.updateById(record);

        // 写入操作日志
        ScoreRecordOperationLog log = new ScoreRecordOperationLog();
        log.setScoreRecordId(id);
        // 后续替换为token解析出来的真实管理员ID，当前占位1L
        log.setOperatorId(1L);
        log.setOperation("HIDE");
        if (dto != null) {
            log.setReason(dto.getReason());
        }
        log.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(log);

        return Result.success(null);
    }


    /**
     * 恢复某一条成绩记录
     * 管理员专用，同时写入操作日志
     */
    @PutMapping("/show/{id}")
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

        record.setAdminHidden((short) 0);
        scoreRecordMapper.updateById(record);

        // 写入操作日志
        ScoreRecordOperationLog log = new ScoreRecordOperationLog();
        log.setScoreRecordId(id);
        // 后续替换为token解析出来的真实管理员ID，当前占位1L
        log.setOperatorId(1L);
        log.setOperation("RESTORE");
        if (dto != null) {
            log.setReason(dto.getReason());
        }
        log.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(log);

        return Result.success(null);
    }


    /**
     * 查询学生当前总成绩
     * 管理员看到的是全部记录，包括已经隐藏的记录
     */
    @GetMapping("/student/{studentId}/total")
    public Result<BigDecimal> total(
            @PathVariable Long studentId
    ) {
        List<ScoreRecord> records = scoreRecordMapper.selectList(
                new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getStudentId, studentId)
                        .eq(ScoreRecord::getAdminHidden, (short) 0)
        );

        BigDecimal bonusScore = records.stream()
                .map(ScoreRecord::getScore)
                .filter(score -> score != null)
                .filter(score -> score.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal deductScore = records.stream()
                .map(ScoreRecord::getScore)
                .filter(score -> score != null)
                .filter(score -> score.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal actualLimit = com.student.studentscoresystem.common.ScoreConstants.MAX_SCORE
                .subtract(deductScore);

        if (actualLimit.compareTo(BigDecimal.ZERO) < 0) {
            actualLimit = BigDecimal.ZERO;
        }

        BigDecimal finalScore = bonusScore.min(actualLimit);
        return Result.success(finalScore);
    }
}