package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.ScoreRecordOperationLog;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.ScoreRecordOperationLogMapper;
import com.student.studentscoresystem.mapper.ScoreRuleMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.IScoreRecordService;
import com.student.studentscoresystem.utils.JwtUtil;
import com.student.studentscoresystem.vo.ScoreDetailVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/scoreRecord")
public class ScoreRecordController {

    private final IScoreRecordService scoreRecordService;

    private final ScoreRuleMapper scoreRuleMapper;

    private final SysUserMapper sysUserMapper;

    private final SysPositionMapper positionMapper;

    private final SysUserPositionMapper userPositionMapper;

    private final ScoreRecordOperationLogMapper operationLogMapper;


    public ScoreRecordController(
            IScoreRecordService scoreRecordService,
            ScoreRuleMapper scoreRuleMapper,
            SysUserMapper sysUserMapper,
            SysPositionMapper positionMapper,
            SysUserPositionMapper userPositionMapper,
            ScoreRecordOperationLogMapper operationLogMapper
    ) {

        this.scoreRecordService = scoreRecordService;

        this.scoreRuleMapper = scoreRuleMapper;

        this.sysUserMapper = sysUserMapper;

        this.positionMapper = positionMapper;

        this.userPositionMapper = userPositionMapper;

        this.operationLogMapper = operationLogMapper;
    }


    /**
     * =====================================================
     * 获取当前登录用户ID
     * =====================================================
     */
    private Long getUserId(
            HttpServletRequest request
    ) {

        String token =
                request.getHeader("Authorization");

        if (
                token == null ||
                        !token.startsWith("Bearer ")
        ) {
            return null;
        }

        token =
                token.substring(7);

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
     * =====================================================
     * 判断是否管理员
     * =====================================================
     */
    private boolean isAdmin(
            Long userId
    ) {

        if (userId == null) {
            return false;
        }

        SysPosition adminPosition =
                positionMapper.selectOne(
                        new LambdaQueryWrapper<SysPosition>()
                                .eq(
                                        SysPosition::getName,
                                        "管理员"
                                )
                );

        if (adminPosition == null) {
            return false;
        }

        Long count =
                userPositionMapper.selectCount(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .eq(
                                        SysUserPosition::getUserId,
                                        userId
                                )
                                .eq(
                                        SysUserPosition::getPositionId,
                                        adminPosition.getId()
                                )
                );

        return count != null && count > 0;
    }


    /**
     * =====================================================
     * 管理员查看全部成绩记录
     * =====================================================
     */
    @GetMapping("/list")
    public Result<List<ScoreDetailVO>> list(
            HttpServletRequest request
    ) {

        Long adminId =
                getUserId(request);

        if (adminId == null) {
            return Result.error("请先登录");
        }

        if (!isAdmin(adminId)) {
            return Result.error("没有管理员权限");
        }

        List<ScoreRecord> records =
                scoreRecordService.list(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .orderByDesc(
                                        ScoreRecord::getCreateTime
                                )
                );

        return Result.success(
                convertVO(records)
        );
    }


    /**
     * =====================================================
     * 查看指定学生的成绩记录
     * =====================================================
     */
    @GetMapping("/student/{studentId}")
    public Result<List<ScoreDetailVO>> student(
            @PathVariable Long studentId
    ) {

        SysUser student =
                sysUserMapper.selectById(studentId);

        if (student == null) {
            return Result.fail("学生不存在");
        }

        List<ScoreRecord> records =
                scoreRecordService.list(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .eq(
                                        ScoreRecord::getStudentId,
                                        studentId
                                )
                                .eq(
                                        ScoreRecord::getAdminHidden,
                                        (short) 0
                                )
                                .orderByDesc(
                                        ScoreRecord::getCreateTime
                                )
                );

        return Result.success(
                convertVO(records)
        );
    }


    /**
     * =====================================================
     * 管理员查看指定学生全部成绩
     * =====================================================
     *
     * 管理员可以看到：
     *
     * 1. 正常成绩
     * 2. 已隐藏成绩
     */
    @GetMapping("/admin/student/{studentId}")
    public Result<List<ScoreDetailVO>> adminStudent(
            @PathVariable Long studentId,
            HttpServletRequest request
    ) {

        Long adminId =
                getUserId(request);

        if (adminId == null) {
            return Result.error("请先登录");
        }

        if (!isAdmin(adminId)) {
            return Result.error("没有管理员权限");
        }

        SysUser student =
                sysUserMapper.selectById(studentId);

        if (student == null) {
            return Result.fail("学生不存在");
        }

        List<ScoreRecord> records =
                scoreRecordService.list(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .eq(
                                        ScoreRecord::getStudentId,
                                        studentId
                                )
                                .orderByDesc(
                                        ScoreRecord::getCreateTime
                                )
                );

        return Result.success(
                convertVO(records)
        );
    }


    /**
     * =====================================================
     * 管理员隐藏成绩记录
     * =====================================================
     */
    @PutMapping("/admin/hide/{id}")
    public Result<Void> hide(
            @PathVariable Long id,
            @RequestParam(
                    required = false
            ) String reason,
            HttpServletRequest request
    ) {

        Long adminId =
                getUserId(request);

        if (adminId == null) {
            return Result.error("请先登录");
        }

        if (!isAdmin(adminId)) {
            return Result.error("没有管理员权限");
        }

        ScoreRecord record =
                scoreRecordService.getById(id);

        if (record == null) {
            return Result.fail("成绩记录不存在");
        }

        /*
         * 已经隐藏
         */
        if (
                Short.valueOf((short) 1)
                        .equals(record.getAdminHidden())
        ) {
            return Result.fail("该成绩记录已经隐藏");
        }

        /*
         * 修改隐藏状态
         */
        record.setAdminHidden(
                (short) 1
        );

        scoreRecordService.updateById(record);


        /*
         * 写入操作日志
         */
        ScoreRecordOperationLog log =
                new ScoreRecordOperationLog();

        log.setScoreRecordId(id);

        log.setOperatorId(adminId);

        log.setOperation("HIDE");

        log.setReason(reason);

        log.setCreateTime(
                LocalDateTime.now()
        );

        operationLogMapper.insert(log);


        return Result.success(null);
    }


    /**
     * =====================================================
     * 管理员恢复成绩记录
     * =====================================================
     */
    @PutMapping("/admin/restore/{id}")
    public Result<Void> restore(
            @PathVariable Long id,
            @RequestParam(
                    required = false
            ) String reason,
            HttpServletRequest request
    ) {

        Long adminId =
                getUserId(request);

        if (adminId == null) {
            return Result.error("请先登录");
        }

        if (!isAdmin(adminId)) {
            return Result.error("没有管理员权限");
        }

        ScoreRecord record =
                scoreRecordService.getById(id);

        if (record == null) {
            return Result.fail("成绩记录不存在");
        }

        /*
         * 当前没有隐藏
         */
        if (
                !Short.valueOf((short) 1)
                        .equals(record.getAdminHidden())
        ) {
            return Result.fail("该成绩记录当前未隐藏");
        }

        /*
         * 恢复
         */
        record.setAdminHidden(
                (short) 0
        );

        scoreRecordService.updateById(record);


        /*
         * 写入恢复日志
         */
        ScoreRecordOperationLog log =
                new ScoreRecordOperationLog();

        log.setScoreRecordId(id);

        log.setOperatorId(adminId);

        log.setOperation("RESTORE");

        log.setReason(reason);

        log.setCreateTime(
                LocalDateTime.now()
        );

        operationLogMapper.insert(log);


        return Result.success(null);
    }


    /**
     * =====================================================
     * 管理员查看某条成绩的操作日志
     * =====================================================
     */
    @GetMapping("/admin/log/{scoreRecordId}")
    public Result<List<ScoreRecordOperationLog>> logs(
            @PathVariable Long scoreRecordId,
            HttpServletRequest request
    ) {

        Long adminId =
                getUserId(request);

        if (adminId == null) {
            return Result.error("请先登录");
        }

        if (!isAdmin(adminId)) {
            return Result.error("没有管理员权限");
        }

        ScoreRecord record =
                scoreRecordService.getById(
                        scoreRecordId
                );

        if (record == null) {
            return Result.fail("成绩记录不存在");
        }

        List<ScoreRecordOperationLog> logs =
                operationLogMapper.selectList(
                        new LambdaQueryWrapper<ScoreRecordOperationLog>()
                                .eq(
                                        ScoreRecordOperationLog::getScoreRecordId,
                                        scoreRecordId
                                )
                                .orderByDesc(
                                        ScoreRecordOperationLog::getCreateTime
                                )
                );

        return Result.success(logs);
    }


    /**
     * =====================================================
     * ScoreRecord -> ScoreDetailVO
     * =====================================================
     */
    private List<ScoreDetailVO> convertVO(
            List<ScoreRecord> records
    ) {

        return records.stream()
                .map(record -> {

                    ScoreDetailVO vo =
                            new ScoreDetailVO();

                    vo.setScore(
                            record.getScore()
                    );

                    vo.setSourceType(
                            record.getSourceType()
                    );

                    vo.setCreateTime(
                            record.getCreateTime()
                    );

                    vo.setAdminHidden(
                            record.getAdminHidden()
                    );


                    /*
                     * 查询加分规则
                     */
                    if (
                            record.getRuleId() != null
                    ) {

                        ScoreRule rule =
                                scoreRuleMapper.selectById(
                                        record.getRuleId()
                                );

                        if (rule != null) {

                            vo.setRuleName(
                                    rule.getName()
                            );
                        }
                    }

                    return vo;

                })
                .toList();
    }
}