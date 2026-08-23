package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.ScoreRecordOperationLog;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.ScoreRecordOperationLogMapper;
import com.student.studentscoresystem.mapper.ScoreRuleMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/scoreRecord")
public class ScoreRecordAdminController {

    private final ScoreRecordMapper scoreRecordMapper;

    private final ScoreRuleMapper scoreRuleMapper;

    private final SysUserMapper sysUserMapper;

    private final ScoreRecordOperationLogMapper operationLogMapper;


    public ScoreRecordAdminController(
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


        /*
         * 修改成绩状态
         */
        record.setStatus((short) 0);

        scoreRecordMapper.updateById(record);


        /*
         * 保存操作日志
         */
        ScoreRecordOperationLog log =
                new ScoreRecordOperationLog();

        log.setScoreRecordId(id);

        log.setOperatorId(adminId);

        log.setOperation("HIDE");

        log.setReason(reason);

        log.setCreateTime(LocalDateTime.now());

        operationLogMapper.insert(log);


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


        /*
         * 恢复成绩
         */
        record.setStatus((short) 1);

        scoreRecordMapper.updateById(record);


        /*
         * 保存操作日志
         */
        ScoreRecordOperationLog log =
                new ScoreRecordOperationLog();

        log.setScoreRecordId(id);

        log.setOperatorId(adminId);

        log.setOperation("RESTORE");

        log.setReason(reason);

        log.setCreateTime(LocalDateTime.now());

        operationLogMapper.insert(log);


        return Result.success(null);
    }


    /**
     * 查询某条成绩的操作记录
     */
    @GetMapping("/logs/{id}")
    public Result<List<ScoreRecordOperationLog>> logs(
            @PathVariable Long id
    ) {

        List<ScoreRecordOperationLog> list =
                operationLogMapper.selectList(
                        new LambdaQueryWrapper<ScoreRecordOperationLog>()
                                .eq(
                                        ScoreRecordOperationLog::getScoreRecordId,
                                        id
                                )
                                .orderByDesc(
                                        ScoreRecordOperationLog::getCreateTime
                                )
                );

        return Result.success(list);
    }


    /**
     * 从 JWT 中获取当前登录用户 ID
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
                    JwtUtil.parseToken(token);

            return claims.get(
                    "userId",
                    Long.class
            );

        } catch (Exception e) {

            return null;
        }
    }
}