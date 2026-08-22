package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.ScoreAdminAdjustmentAddDTO;
import com.student.studentscoresystem.entity.ScoreAdminAdjustment;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.IScoreAdminAdjustmentService;
import com.student.studentscoresystem.service.IScoreRecordService;
import com.student.studentscoresystem.utils.JwtUtil;
import com.student.studentscoresystem.vo.ScoreAdminAdjustmentVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/scoreAdjustment")
public class ScoreAdminAdjustmentController {

    private final IScoreAdminAdjustmentService adjustmentService;

    private final IScoreRecordService scoreRecordService;

    private final ScoreRecordMapper scoreRecordMapper;

    private final SysUserMapper sysUserMapper;

    private final SysUserPositionMapper userPositionMapper;

    private final SysPositionMapper positionMapper;

    public ScoreAdminAdjustmentController(
            IScoreAdminAdjustmentService adjustmentService,
            IScoreRecordService scoreRecordService,
            ScoreRecordMapper scoreRecordMapper,
            SysUserMapper sysUserMapper,
            SysUserPositionMapper userPositionMapper,
            SysPositionMapper positionMapper
    ) {
        this.adjustmentService = adjustmentService;
        this.scoreRecordService = scoreRecordService;
        this.scoreRecordMapper = scoreRecordMapper;
        this.sysUserMapper = sysUserMapper;
        this.userPositionMapper = userPositionMapper;
        this.positionMapper = positionMapper;
    }

    /**
     * 从 Token 获取当前登录用户 ID
     */
    private Long getUserId(HttpServletRequest request) {

        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }

        token = token.substring(7);

        try {
            Claims claims = JwtUtil.parseToken(token);

            return claims.get(
                    "userId",
                    Long.class
            );

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断当前用户是否为管理员
     */
    private boolean isAdmin(Long userId) {

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
     * =========================================================
     * 管理员新增成绩调整
     * =========================================================
     *
     * adjustType：
     *
     * 1  = 加分
     * -1 = 扣分
     *
     * 保存两份数据：
     *
     * 1. ScoreAdminAdjustment
     *    管理员操作日志
     *
     * 2. ScoreRecord
     *    真正进入综合测评统计的数据
     */
    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody ScoreAdminAdjustmentAddDTO dto,
            HttpServletRequest request
    ) {

        /*
         * =====================================================
         * 1. 获取管理员
         * =====================================================
         */

        Long adminId = getUserId(request);

        if (adminId == null) {
            return Result.fail("请先登录");
        }

        /*
         * =====================================================
         * 2. 管理员权限
         * =====================================================
         */

        if (!isAdmin(adminId)) {
            return Result.fail("没有管理员权限");
        }

        /*
         * =====================================================
         * 3. 参数校验
         * =====================================================
         */

        if (dto.getStudentId() == null) {
            return Result.fail("请选择学生");
        }

        if (dto.getAdjustType() == null) {
            return Result.fail("请选择调整类型");
        }

        if (
                dto.getAdjustType() != 1
                        && dto.getAdjustType() != -1
        ) {
            return Result.fail("调整类型错误");
        }

        if (
                dto.getScore() == null
                        || dto.getScore()
                        .compareTo(BigDecimal.ZERO) <= 0
        ) {
            return Result.fail("调整分数必须大于0");
        }

        if (
                dto.getReason() == null
                        || dto.getReason()
                        .trim()
                        .isEmpty()
        ) {
            return Result.fail("请填写调整原因");
        }

        /*
         * =====================================================
         * 4. 查询学生
         * =====================================================
         */

        SysUser student =
                sysUserMapper.selectById(
                        dto.getStudentId()
                );

        if (student == null) {
            return Result.fail("学生不存在");
        }

        /*
         * =====================================================
         * 5. 创建管理员调整记录
         * =====================================================
         */

        ScoreAdminAdjustment adjustment =
                new ScoreAdminAdjustment();

        adjustment.setStudentId(
                dto.getStudentId()
        );

        adjustment.setAdminId(
                adminId
        );

        adjustment.setAdjustType(
                dto.getAdjustType()
        );

        adjustment.setScore(
                dto.getScore()
        );

        adjustment.setReason(
                dto.getReason().trim()
        );

        adjustmentService.save(adjustment);

        /*
         * =====================================================
         * 6. 生成 ScoreRecord
         * =====================================================
         *
         * ScoreStatisticsController
         * 最终只认 ScoreRecord。
         *
         * 所以管理员调整必须进入 ScoreRecord。
         */

        BigDecimal realScore =
                dto.getScore();

        /*
         * 扣分转换成负数
         */

        if (dto.getAdjustType() == -1) {

            realScore =
                    realScore.negate();
        }

        ScoreRecord record =
                new ScoreRecord();

        record.setStudentId(
                dto.getStudentId()
        );

        /*
         * 管理员手动调整没有 ScoreRule
         */

        record.setRuleId(null);

        record.setScore(
                realScore
        );

        record.setSourceType(
                "ADMIN_ADJUSTMENT"
        );

        /*
         * sourceId 对应管理员调整记录
         */

        record.setSourceId(
                adjustment.getId()
        );

        record.setAdminHidden(
                (short) 0
        );

        record.setCreateTime(
                LocalDateTime.now()
        );

        scoreRecordMapper.insert(record);

        return Result.success(null);
    }

    /**
     * =========================================================
     * 管理员查看全部成绩调整记录
     * =========================================================
     */
    @GetMapping("/list")
    public Result<List<ScoreAdminAdjustmentVO>> list(
            HttpServletRequest request
    ) {

        Long adminId = getUserId(request);

        if (adminId == null) {
            return Result.fail("请先登录");
        }

        if (!isAdmin(adminId)) {
            return Result.fail("没有管理员权限");
        }

        List<ScoreAdminAdjustment> list =
                adjustmentService.list(
                        new LambdaQueryWrapper<ScoreAdminAdjustment>()
                                .orderByDesc(
                                        ScoreAdminAdjustment::getCreateTime
                                )
                );

        return Result.success(
                convertVO(list)
        );
    }

    /**
     * =========================================================
     * 管理员查看某个学生的调整记录
     * =========================================================
     */
    @GetMapping("/student/{studentId}")
    public Result<List<ScoreAdminAdjustmentVO>> student(
            @PathVariable Long studentId,
            HttpServletRequest request
    ) {

        Long adminId = getUserId(request);

        if (adminId == null) {
            return Result.fail("请先登录");
        }

        if (!isAdmin(adminId)) {
            return Result.fail("没有管理员权限");
        }

        SysUser student =
                sysUserMapper.selectById(
                        studentId
                );

        if (student == null) {
            return Result.fail("学生不存在");
        }

        List<ScoreAdminAdjustment> list =
                adjustmentService.list(
                        new LambdaQueryWrapper<ScoreAdminAdjustment>()
                                .eq(
                                        ScoreAdminAdjustment::getStudentId,
                                        studentId
                                )
                                .orderByDesc(
                                        ScoreAdminAdjustment::getCreateTime
                                )
                );

        return Result.success(
                convertVO(list)
        );
    }

    /**
     * =========================================================
     * 转换 VO
     * =========================================================
     */
    private List<ScoreAdminAdjustmentVO> convertVO(
            List<ScoreAdminAdjustment> list
    ) {

        return list.stream()
                .map(item -> {

                    ScoreAdminAdjustmentVO vo =
                            new ScoreAdminAdjustmentVO();

                    vo.setId(
                            item.getId()
                    );

                    vo.setStudentId(
                            item.getStudentId()
                    );

                    vo.setAdminId(
                            item.getAdminId()
                    );

                    vo.setAdjustType(
                            item.getAdjustType()
                    );

                    vo.setScore(
                            item.getScore()
                    );

                    vo.setReason(
                            item.getReason()
                    );

                    vo.setCreateTime(
                            item.getCreateTime()
                    );

                    /*
                     * 学生信息
                     */
                    SysUser student =
                            sysUserMapper.selectById(
                                    item.getStudentId()
                            );

                    if (student != null) {

                        vo.setStudentName(
                                student.getRealName()
                        );

                        vo.setStudentNo(
                                student.getStudentNo()
                        );
                    }

                    /*
                     * 管理员信息
                     */
                    SysUser adminUser =
                            sysUserMapper.selectById(
                                    item.getAdminId()
                            );

                    if (adminUser != null) {

                        vo.setAdminName(
                                adminUser.getRealName()
                        );
                    }

                    return vo;
                })
                .toList();
    }
}