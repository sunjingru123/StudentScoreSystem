package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.ScoreAdminAdjustmentAddDTO;
import com.student.studentscoresystem.entity.ScoreAdminAdjustment;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.service.IScoreAdminAdjustmentService;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.vo.ScoreAdminAdjustmentVO;
import com.student.studentscoresystem.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/admin/scoreAdjustment")
public class ScoreAdminAdjustmentController {

    /**
     * 管理员成绩调整 Service
     */
    private final IScoreAdminAdjustmentService adjustmentService;

    /**
     * 用户
     */
    private final SysUserMapper sysUserMapper;

    /**
     * 用户职位
     */
    private final SysUserPositionMapper userPositionMapper;

    /**
     * 职位
     */
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
     * =========================================================
     * 获取当前登录用户 ID
     * =========================================================
     */
    private Long getUserId(
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
     * =========================================================
     * 判断当前用户是否为管理员
     * =========================================================
     */
    private boolean isAdmin(
            Long userId
    ) {

        if (userId == null) {
            return false;
        }

        /*
         * 查询管理员职位
         */
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

        /*
         * 查询当前用户是否拥有管理员职位
         */
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

        return count != null
                && count > 0;
    }


    /**
     * =========================================================
     * 管理员新增成绩调整
     * =========================================================
     *
     * 流程：
     *
     * 管理员
     *      ↓
     * 登录校验
     *      ↓
     * 管理员权限校验
     *      ↓
     * 参数校验
     *      ↓
     * 学生校验
     *      ↓
     * Service
     *      ↓
     * ┌──────────────────────────────┐
     * │ score_admin_adjustment       │
     * │ score_record                 │
     * │ score_flow                   │
     * └──────────────────────────────┘
     *
     * 三张表由 Service 事务统一处理。
     */
    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody ScoreAdminAdjustmentAddDTO dto,
            HttpServletRequest request
    ) {

        /*
         * =====================================================
         * 1. 获取当前登录管理员
         * =====================================================
         */
        Long adminId =
                getUserId(request);

        if (adminId == null) {
            return Result.error("请先登录");
        }


        /*
         * =====================================================
         * 2. 管理员权限校验
         * =====================================================
         */
        if (!isAdmin(adminId)) {
            return Result.error("没有管理员权限");
        }


        /*
         * =====================================================
         * 3. 参数校验
         * =====================================================
         */
        if (dto == null) {
            return Result.error("调整参数不能为空");
        }

        if (dto.getStudentId() == null) {
            return Result.error("请选择学生");
        }

        if (dto.getAdjustType() == null) {
            return Result.error("请选择调整类型");
        }

        /*
         * 1 = 加分
         * -1 = 减分
         */
        if (
                dto.getAdjustType() != 1
                        && dto.getAdjustType() != -1
        ) {
            return Result.error("调整类型错误");
        }

        /*
         * 调整分值必须大于 0
         */
        if (
                dto.getScore() == null
                        || dto.getScore()
                        .compareTo(BigDecimal.ZERO) <= 0
        ) {
            return Result.error("调整分数必须大于0");
        }


        /*
         * =====================================================
         * 4. 检查学生是否存在
         * =====================================================
         */
        SysUser student =
                sysUserMapper.selectById(
                        dto.getStudentId()
                );

        if (student == null) {
            return Result.error("学生不存在");
        }


        /*
         * =====================================================
         * 5. 检查学生状态
         * =====================================================
         *
         * status = 1
         * 表示正常状态
         */
        if (
                student.getStatus() != null
                        && !Short.valueOf((short) 1)
                        .equals(student.getStatus())
        ) {
            return Result.error("该学生当前不是正常状态");
        }


        /*
         * =====================================================
         * 6. 调用 Service
         * =====================================================
         *
         * Service 内部统一完成：
         *
         * ① 保存 ScoreAdminAdjustment
         *
         * ② 创建 ScoreRecord
         *
         *     sourceType = ADMIN_ADJUSTMENT
         *     sourceId   = adjustment.id
         *
         * ③ 创建 ScoreFlow
         *
         * 并且三个操作处于同一个事务中。
         */
        adjustmentService.createAdjustment(
                adminId,
                dto.getStudentId(),
                dto.getAdjustType(),
                dto.getScore(),
                dto.getReason()
        );


        /*
         * =====================================================
         * 7. 返回
         * =====================================================
         */
        return Result.success(null);
    }


    /**
     * =========================================================
     * 管理员查看全部成绩调整
     * =========================================================
     */
    @GetMapping("/list")
    public Result<List<ScoreAdminAdjustmentVO>> list(
            HttpServletRequest request
    ) {

        /*
         * 当前管理员
         */
        Long adminId =
                getUserId(request);

        if (adminId == null) {
            return Result.error("请先登录");
        }


        /*
         * 管理员权限
         */
        if (!isAdmin(adminId)) {
            return Result.error("没有管理员权限");
        }


        /*
         * 查询全部调整记录
         */
        List<ScoreAdminAdjustment> list =
                adjustmentService.list(
                        new LambdaQueryWrapper<ScoreAdminAdjustment>()
                                .orderByDesc(
                                        ScoreAdminAdjustment::getCreateTime
                                )
                );


        /*
         * Entity → VO
         */
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

        /*
         * 当前管理员
         */
        Long adminId =
                getUserId(request);

        if (adminId == null) {
            return Result.error("请先登录");
        }


        /*
         * 管理员权限
         */
        if (!isAdmin(adminId)) {
            return Result.error("没有管理员权限");
        }


        /*
         * 检查学生
         */
        SysUser student =
                sysUserMapper.selectById(
                        studentId
                );

        if (student == null) {
            return Result.error("学生不存在");
        }


        /*
         * 查询该学生的调整记录
         */
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


        /*
         * Entity → VO
         */
        return Result.success(
                convertVO(list)
        );
    }


    /**
     * =========================================================
     * Entity → VO
     * =========================================================
     */
    private List<ScoreAdminAdjustmentVO> convertVO(
            List<ScoreAdminAdjustment> list
    ) {

        return list.stream()
                .map(item -> {

                    ScoreAdminAdjustmentVO vo =
                            new ScoreAdminAdjustmentVO();


                    /*
                     * 基础信息
                     */
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
                     * =================================================
                     * 学生信息
                     * =================================================
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
                     * =================================================
                     * 管理员信息
                     * =================================================
                     */
                    SysUser admin =
                            sysUserMapper.selectById(
                                    item.getAdminId()
                            );

                    if (admin != null) {

                        vo.setAdminName(
                                admin.getRealName()
                        );
                    }


                    return vo;

                })
                .toList();
    }
}