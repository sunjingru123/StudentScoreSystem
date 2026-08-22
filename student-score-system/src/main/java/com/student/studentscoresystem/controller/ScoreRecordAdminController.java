package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.ScoreRecordAdminLog;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.ScoreRecordAdminLogMapper;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.ScoreRuleMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/scoreRecord")
public class ScoreRecordAdminController {

    private final ScoreRecordMapper scoreRecordMapper;

    private final ScoreRecordAdminLogMapper logMapper;

    private final ScoreRuleMapper scoreRuleMapper;

    private final SysUserMapper sysUserMapper;

    public ScoreRecordAdminController(
            ScoreRecordMapper scoreRecordMapper,
            ScoreRecordAdminLogMapper logMapper,
            ScoreRuleMapper scoreRuleMapper,
            SysUserMapper sysUserMapper
    ) {
        this.scoreRecordMapper = scoreRecordMapper;
        this.logMapper = logMapper;
        this.scoreRuleMapper = scoreRuleMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 查询学生全部成绩
     *
     * 管理员专用
     *
     * status = 1 有效
     * status = 0 已作废
     */
    @GetMapping("/student/{studentId}")
    public Result<List<ScoreRecord>> studentRecords(
            @PathVariable Long studentId
    ) {

        SysUser student =
                sysUserMapper.selectById(studentId);

        if (student == null) {
            return Result.error("学生不存在");
        }

        List<ScoreRecord> list =
                scoreRecordMapper.selectList(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .eq(
                                        ScoreRecord::getStudentId,
                                        studentId
                                )
                                .orderByDesc(
                                        ScoreRecord::getCreateTime
                                )
                );

        return Result.success(list);
    }

    /**
     * 作废成绩
     */
    @PutMapping("/void/{id}")
    public Result<Void> voidRecord(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            HttpServletRequest request
    ) {

        ScoreRecord record =
                scoreRecordMapper.selectById(id);

        if (record == null) {
            return Result.error("成绩记录不存在");
        }

        if (record.getStatus() != null
                && record.getStatus() == 0) {

            return Result.error("该成绩已经作废");
        }

        Long adminId =
                getUserId(request);

        if (adminId == null) {
            return Result.error("请先登录");
        }

        record.setStatus((short) 0);

        scoreRecordMapper.updateById(record);

        // 保存操作日志
        ScoreRecordAdminLog log =
                new ScoreRecordAdminLog();

        log.setScoreRecordId(id);

        log.setAdminId(adminId);

        log.setOperation("VOID");

        log.setReason(reason);

        logMapper.insert(log);

        return Result.success(null);
    }

    /**
     * 恢复成绩
     */
    @PutMapping("/restore/{id}")
    public Result<Void> restoreRecord(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            HttpServletRequest request
    ) {

        ScoreRecord record =
                scoreRecordMapper.selectById(id);

        if (record == null) {
            return Result.error("成绩记录不存在");
        }

        if (record.getStatus() != null
                && record.getStatus() == 1) {

            return Result.error("该成绩当前已经有效");
        }

        Long adminId =
                getUserId(request);

        if (adminId == null) {
            return Result.error("请先登录");
        }

        record.setStatus((short) 1);

        scoreRecordMapper.updateById(record);

        // 保存操作日志
        ScoreRecordAdminLog log =
                new ScoreRecordAdminLog();

        log.setScoreRecordId(id);

        log.setAdminId(adminId);

        log.setOperation("RESTORE");

        log.setReason(reason);

        logMapper.insert(log);

        return Result.success(null);
    }

    /**
     * 查询某条成绩的操作记录
     */
    @GetMapping("/logs/{id}")
    public Result<List<ScoreRecordAdminLog>> logs(
            @PathVariable Long id
    ) {

        List<ScoreRecordAdminLog> list =
                logMapper.selectList(
                        new LambdaQueryWrapper<ScoreRecordAdminLog>()
                                .eq(
                                        ScoreRecordAdminLog::getScoreRecordId,
                                        id
                                )
                                .orderByDesc(
                                        ScoreRecordAdminLog::getCreateTime
                                )
                );

        return Result.success(list);
    }

    /**
     * 从 JWT 中获取当前管理员 ID
     */
    private Long getUserId(
            HttpServletRequest request
    ) {

        String token =
                request.getHeader("Authorization");

        if (token == null
                || !token.startsWith("Bearer ")) {

            return null;
        }

        token = token.substring(7);

        try {

            Claims claims =
                    com.student.studentscoresystem.utils.JwtUtil
                            .parseToken(token);

            return claims.get(
                    "userId",
                    Long.class
            );

        } catch (Exception e) {

            return null;
        }
    }
}