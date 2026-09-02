package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.ScoreAdminAdjustment;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.ScoreAdminAdjustmentMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.IScoreAdminAdjustmentService;
import com.student.studentscoresystem.utils.JwtUtil;
import com.student.studentscoresystem.vo.ScoreAdminAdjustmentVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员成绩调整
 */
@RestController
@RequestMapping("/admin/scoreAdjustment")
public class ScoreAdminAdjustmentController {

    private final ScoreAdminAdjustmentMapper adjustmentMapper;

    private final SysUserMapper sysUserMapper;

    private final IScoreAdminAdjustmentService adjustmentService;


    public ScoreAdminAdjustmentController(
            ScoreAdminAdjustmentMapper adjustmentMapper,
            SysUserMapper sysUserMapper,
            IScoreAdminAdjustmentService adjustmentService
    ) {

        this.adjustmentMapper =
                adjustmentMapper;

        this.sysUserMapper =
                sysUserMapper;

        this.adjustmentService =
                adjustmentService;
    }


    /**
     * =========================================================
     * 获取当前登录用户 ID
     * =========================================================
     *
     * 优先从 JWT 过滤器放入的 request attribute 中获取。
     *
     * 如果没有，再从 Authorization Bearer Token 中解析。
     */
    private Long getCurrentUserId(
            HttpServletRequest request
    ) {

        /*
         * =====================================================
         * 1. 优先从 request attribute 获取
         * =====================================================
         */

        Object userIdAttr =
                request.getAttribute("userId");

        if (userIdAttr != null) {

            try {

                return Long.valueOf(
                        userIdAttr.toString()
                );

            } catch (Exception ignored) {
            }
        }


        /*
         * =====================================================
         * 2. 从 JWT 获取
         * =====================================================
         */

        String token =
                request.getHeader("Authorization");


        if (
                token == null
                        ||
                        !token.startsWith("Bearer ")
        ) {

            throw new IllegalArgumentException(
                    "请先登录"
            );
        }


        try {

            Claims claims =
                    JwtUtil.parseToken(
                            token.substring(7)
                    );


            Object userId =
                    claims.get("userId");


            if (userId == null) {

                throw new IllegalArgumentException(
                        "登录状态无效"
                );
            }


            return Long.valueOf(
                    userId.toString()
            );


        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "登录状态无效，请重新登录"
            );
        }
    }


    /**
     * =========================================================
     * 获取管理员成绩调整记录
     * =========================================================
     */
    @GetMapping("/list")
    public Result<Page<ScoreAdminAdjustmentVO>> list(
            @RequestParam(
                    defaultValue = "1"
            )
            long pageNum,

            @RequestParam(
                    defaultValue = "10"
            )
            long pageSize
    ) {

        if (pageNum < 1) {

            pageNum = 1;
        }


        if (pageSize < 1) {

            pageSize = 10;
        }


        if (pageSize > 100) {

            pageSize = 100;
        }


        Page<ScoreAdminAdjustment> page =
                new Page<>(
                        pageNum,
                        pageSize
                );


        LambdaQueryWrapper<ScoreAdminAdjustment> wrapper =
                new LambdaQueryWrapper<>();


        wrapper.orderByDesc(
                ScoreAdminAdjustment::getCreateTime
        );


        Page<ScoreAdminAdjustment> adjustmentPage =
                adjustmentMapper.selectPage(
                        page,
                        wrapper
                );


        Page<ScoreAdminAdjustmentVO> voPage =
                new Page<>(
                        adjustmentPage.getCurrent(),
                        adjustmentPage.getSize(),
                        adjustmentPage.getTotal()
                );


        List<ScoreAdminAdjustmentVO> voList =
                new ArrayList<>();


        for (
                ScoreAdminAdjustment adjustment
                :
                adjustmentPage.getRecords()
        ) {

            if (adjustment == null) {

                continue;
            }


            ScoreAdminAdjustmentVO vo =
                    new ScoreAdminAdjustmentVO();


            vo.setId(
                    adjustment.getId()
            );


            vo.setStudentId(
                    adjustment.getStudentId()
            );


            /*
             * Short → Integer
             */
            if (
                    adjustment.getAdjustType()
                            != null
            ) {

                vo.setAdjustType(
                        adjustment
                                .getAdjustType()
                                .intValue()
                );
            }


            vo.setScore(
                    adjustment.getScore()
            );


            vo.setReason(
                    adjustment.getReason()
            );


            vo.setAdminId(
                    adjustment.getAdminId()
            );


            vo.setCreateTime(
                    adjustment.getCreateTime()
            );


            /*
             * =================================================
             * 学生信息
             * =================================================
             */

            if (
                    adjustment.getStudentId()
                            != null
            ) {

                SysUser student =
                        sysUserMapper.selectById(
                                adjustment.getStudentId()
                        );


                if (student != null) {

                    vo.setStudentName(
                            student.getRealName()
                    );

                    vo.setStudentNo(
                            student.getStudentNo()
                    );
                }
            }


            /*
             * =================================================
             * 管理员信息
             * =================================================
             */

            if (
                    adjustment.getAdminId()
                            != null
            ) {

                SysUser admin =
                        sysUserMapper.selectById(
                                adjustment.getAdminId()
                        );


                if (admin != null) {

                    vo.setAdminName(
                            admin.getRealName()
                    );
                }
            }


            voList.add(vo);
        }


        voPage.setRecords(
                voList
        );


        return Result.success(
                voPage
        );
    }


    /**
     * =========================================================
     * 新增管理员成绩调整
     * =========================================================
     */
    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody ScoreAdminAdjustment adjustment,
            HttpServletRequest request
    ) {

        try {

            /*
             * =================================================
             * 1. 基础参数检查
             * =================================================
             */

            if (adjustment == null) {

                return Result.error(
                        "参数不能为空"
                );
            }


            if (
                    adjustment.getStudentId()
                            == null
            ) {

                return Result.error(
                        "请选择学生"
                );
            }


            if (
                    adjustment.getAdjustType()
                            == null
            ) {

                return Result.error(
                        "请选择调整类型"
                );
            }


            int type =
                    adjustment
                            .getAdjustType()
                            .intValue();


            if (
                    type != 1
                            &&
                            type != -1
            ) {

                return Result.error(
                        "调整类型只能是加分(1)或减分(-1)"
                );
            }


            if (
                    adjustment.getScore()
                            == null
                            ||
                            adjustment
                                    .getScore()
                                    .doubleValue()
                                    <= 0
            ) {

                return Result.error(
                        "调整分数必须大于0"
                );
            }


            if (
                    adjustment.getReason()
                            == null
                            ||
                            adjustment
                                    .getReason()
                                    .trim()
                                    .isEmpty()
            ) {

                return Result.error(
                        "请输入调整原因"
                );
            }


            /*
             * =================================================
             * 2. 获取当前登录管理员
             * =================================================
             */

            Long adminId =
                    getCurrentUserId(
                            request
                    );


            /*
             * =================================================
             * 3. 检查管理员是否存在
             * =================================================
             */

            SysUser admin =
                    sysUserMapper.selectById(
                            adminId
                    );


            if (admin == null) {

                return Result.error(
                        "当前管理员不存在"
                );
            }


            /*
             * =================================================
             * 4. 检查学生是否存在
             * =================================================
             */

            SysUser student =
                    sysUserMapper.selectById(
                            adjustment.getStudentId()
                    );


            if (student == null) {

                return Result.error(
                        "学生不存在"
                );
            }


            /*
             * =================================================
             * 5. 真正执行管理员成绩调整
             * =================================================
             *
             * 注意：
             *
             * 这里不能再直接：
             *
             * adjustmentMapper.insert()
             *
             * 而应该调用 Service。
             *
             * Service 会：
             *
             * ① 保存 score_admin_adjustment
             * ② 创建 score_record
             * ③ 创建 score_flow
             *
             * 三个操作在同一个事务中。
             */

            adjustmentService.createAdjustment(

                    adminId,

                    adjustment.getStudentId(),

                    adjustment.getAdjustType(),

                    adjustment.getScore(),

                    adjustment.getReason().trim()
            );


            /*
             * =================================================
             * 6. 成功
             * =================================================
             */

            return Result.success(
                    null
            );


        } catch (
                IllegalArgumentException e
        ) {

            return Result.error(
                    e.getMessage()
            );


        } catch (Exception e) {

            e.printStackTrace();

            return Result.error(
                    "成绩调整失败：" +
                            e.getMessage()
            );
        }
    }
}