package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.ScoreAdminAdjustmentAddDTO;
import com.student.studentscoresystem.entity.ScoreAdminAdjustment;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.IScoreAdminAdjustmentService;
import com.student.studentscoresystem.utils.JwtUtil;
import com.student.studentscoresystem.vo.ScoreAdminAdjustmentVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/admin/scoreAdjustment")
public class ScoreAdminAdjustmentController {

    private final IScoreAdminAdjustmentService adjustmentService;

    private final SysUserMapper sysUserMapper;

    private final SysUserPositionMapper userPositionMapper;

    private final SysPositionMapper positionMapper;

    public ScoreAdminAdjustmentController(
            IScoreAdminAdjustmentService adjustmentService,
            SysUserMapper sysUserMapper,
            SysUserPositionMapper userPositionMapper,
            SysPositionMapper positionMapper
    ) {
        this.adjustmentService = adjustmentService;
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

        Claims claims = JwtUtil.parseToken(token);

        return claims.get("userId", Long.class);
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
     * 管理员新增成绩调整
     */
    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody ScoreAdminAdjustmentAddDTO dto,
            HttpServletRequest request
    ) {

        Long adminId = getUserId(request);

        if (adminId == null) {
            return Result.error("请先登录");
        }

        if (!isAdmin(adminId)) {
            return Result.error("没有管理员权限");
        }

        if (dto.getStudentId() == null) {
            return Result.error("请选择学生");
        }

        if (dto.getAdjustType() == null) {
            return Result.error("请选择调整类型");
        }

        if (
                dto.getAdjustType() != 1 &&
                        dto.getAdjustType() != -1
        ) {
            return Result.error("调整类型错误");
        }

        if (
                dto.getScore() == null ||
                        dto.getScore().compareTo(BigDecimal.ZERO) <= 0
        ) {
            return Result.error("调整分数必须大于0");
        }

        SysUser student =
                sysUserMapper.selectById(dto.getStudentId());

        if (student == null) {
            return Result.error("学生不存在");
        }

        ScoreAdminAdjustment adjustment =
                new ScoreAdminAdjustment();

        adjustment.setStudentId(dto.getStudentId());

        adjustment.setAdminId(adminId);

        adjustment.setAdjustType(dto.getAdjustType());

        adjustment.setScore(dto.getScore());

        adjustment.setReason(dto.getReason());

        adjustmentService.save(adjustment);

        return Result.success(null);
    }

    /**
     * 管理员查看全部成绩调整记录
     */
    @GetMapping("/list")
    public Result<List<ScoreAdminAdjustmentVO>> list(
            HttpServletRequest request
    ) {

        Long adminId = getUserId(request);

        if (adminId == null) {
            return Result.error("请先登录");
        }

        if (!isAdmin(adminId)) {
            return Result.error("没有管理员权限");
        }

        List<ScoreAdminAdjustment> list =
                adjustmentService.list(
                        new LambdaQueryWrapper<ScoreAdminAdjustment>()
                                .orderByDesc(
                                        ScoreAdminAdjustment::getCreateTime
                                )
                );

        List<ScoreAdminAdjustmentVO> result =
                list.stream()
                        .map(item -> {

                            ScoreAdminAdjustmentVO vo =
                                    new ScoreAdminAdjustmentVO();

                            vo.setId(item.getId());

                            vo.setStudentId(item.getStudentId());

                            vo.setAdminId(item.getAdminId());

                            vo.setAdjustType(item.getAdjustType());

                            vo.setScore(item.getScore());

                            vo.setReason(item.getReason());

                            vo.setCreateTime(item.getCreateTime());

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

        return Result.success(result);
    }

    /**
     * 管理员查看某个学生的成绩调整记录
     */
    @GetMapping("/student/{studentId}")
    public Result<List<ScoreAdminAdjustmentVO>> student(
            @PathVariable Long studentId,
            HttpServletRequest request
    ) {

        Long adminId = getUserId(request);

        if (adminId == null) {
            return Result.error("请先登录");
        }

        if (!isAdmin(adminId)) {
            return Result.error("没有管理员权限");
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

        List<ScoreAdminAdjustmentVO> result =
                list.stream()
                        .map(item -> {

                            ScoreAdminAdjustmentVO vo =
                                    new ScoreAdminAdjustmentVO();

                            vo.setId(item.getId());

                            vo.setStudentId(item.getStudentId());

                            vo.setAdminId(item.getAdminId());

                            vo.setAdjustType(item.getAdjustType());

                            vo.setScore(item.getScore());

                            vo.setReason(item.getReason());

                            vo.setCreateTime(item.getCreateTime());

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

        return Result.success(result);
    }
}