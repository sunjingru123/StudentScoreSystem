package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysSemester;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysSemesterMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.IScoreExportService;
import com.student.studentscoresystem.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/scoreExport")
public class ScoreExportController {

    private final IScoreExportService scoreExportService;

    private final SysSemesterMapper sysSemesterMapper;

    private final SysUserDepartmentMapper userDepartmentMapper;

    private final DepartmentMapper departmentMapper;

    /*
     * =========================================================
     * 用户岗位
     * =========================================================
     *
     * 管理员身份实际上保存在：
     *
     * sys_user_position
     *       ↓
     * sys_position
     *
     * JWT 里面目前没有 role，
     * 所以管理员权限直接从数据库判断。
     *
     * =========================================================
     */
    private final SysUserPositionMapper sysUserPositionMapper;

    private final SysPositionMapper sysPositionMapper;


    public ScoreExportController(
            IScoreExportService scoreExportService,
            SysSemesterMapper sysSemesterMapper,
            SysUserDepartmentMapper userDepartmentMapper,
            DepartmentMapper departmentMapper,
            SysUserPositionMapper sysUserPositionMapper,
            SysPositionMapper sysPositionMapper) {

        this.scoreExportService =
                scoreExportService;

        this.sysSemesterMapper =
                sysSemesterMapper;

        this.userDepartmentMapper =
                userDepartmentMapper;

        this.departmentMapper =
                departmentMapper;

        this.sysUserPositionMapper =
                sysUserPositionMapper;

        this.sysPositionMapper =
                sysPositionMapper;
    }


    /* =========================================================
       获取当前用户ID
       ========================================================= */

    private Long getCurrentUserId(
            HttpServletRequest request) {

        /*
         * JWT 拦截器已经把 userId 放进 request
         */

        Object userIdAttr =
                request.getAttribute(
                        "userId"
                );


        if (userIdAttr != null) {

            try {

                return Long.valueOf(
                        userIdAttr.toString()
                );

            } catch (Exception ignored) {
            }
        }


        /*
         * 如果 request 中没有，
         * 再从 Authorization 中解析。
         */

        String token =
                request.getHeader(
                        "Authorization"
                );


        if (
                token == null
                        || !token.startsWith(
                        "Bearer "
                )
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


    /* =========================================================
       判断当前用户是不是管理员
       ========================================================= */

    private boolean isAdmin(
            Long userId) {

        if (userId == null) {

            return false;
        }


        /*
         * 查询当前用户所有岗位
         */

        List<SysUserPosition>
                userPositions =
                sysUserPositionMapper.selectList(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .eq(
                                        SysUserPosition::getUserId,
                                        userId
                                )
                );


        if (
                userPositions == null
                        || userPositions.isEmpty()
        ) {

            return false;
        }


        /*
         * 逐个查询岗位
         */

        for (
                SysUserPosition userPosition
                : userPositions
        ) {

            if (userPosition == null) {

                continue;
            }


            if (
                    userPosition.getPositionId()
                            == null
            ) {

                continue;
            }


            SysPosition position =
                    sysPositionMapper.selectById(
                            userPosition.getPositionId()
                    );


            if (position == null) {

                continue;
            }


            /*
             * 岗位必须是启用状态
             */

            if (
                    position.getStatus() != null
                            && position.getStatus() != 1
            ) {

                continue;
            }


            /*
             * 管理员
             */

            if (
                    "管理员".equals(
                            position.getName()
                    )
                            ||
                            "ADMIN".equalsIgnoreCase(
                                    position.getName()
                            )
            ) {

                return true;
            }
        }


        return false;
    }


    /* =========================================================
       判断是否为档案部部长 / 副部长
       ========================================================= */

    private boolean isArchiveLeader(
            Long userId) {

        if (userId == null) {

            return false;
        }


        /*
         * 查询当前用户所有有效部门关系
         */

        List<SysUserDepartment>
                relations =
                userDepartmentMapper.selectList(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getUserId,
                                        userId
                                )
                                .eq(
                                        SysUserDepartment::getStatus,
                                        (short) 1
                                )
                                .in(
                                        SysUserDepartment::getPosition,
                                        List.of(
                                                "副部长",
                                                "部长"
                                        )
                                )
                );


        if (
                relations == null
                        || relations.isEmpty()
        ) {

            return false;
        }


        /*
         * 判断这些部门中是否存在档案部
         */

        for (
                SysUserDepartment relation
                : relations
        ) {

            if (
                    relation.getDepartmentId()
                            == null
            ) {

                continue;
            }


            Department department =
                    departmentMapper.selectById(
                            relation.getDepartmentId()
                    );


            if (department == null) {

                continue;
            }


            /*
             * 部门必须启用
             */

            if (
                    !Short.valueOf((short) 1)
                            .equals(
                                    department.getStatus()
                            )
            ) {

                continue;
            }


            /*
             * 必须是档案部
             */

            if (
                    "档案部".equals(
                            department.getName()
                    )
            ) {

                return true;
            }
        }


        return false;
    }


    /* =========================================================
       判断是否拥有导出权限
       ========================================================= */

    private boolean canExport(
            Long userId) {

        /*
         * 1. 系统管理员
         *
         * 管理员拥有最高权限，
         * 不需要绑定档案部。
         */

        if (
                isAdmin(userId)
        ) {

            return true;
        }


        /*
         * 2. 档案部部长 / 副部长
         */

        return isArchiveLeader(
                userId
        );
    }


    /* =========================================================
       获取导出权限
       ========================================================= */

    @GetMapping("/permission")
    public Result<Map<String, Object>>
    permission(
            HttpServletRequest request) {

        Long userId;


        try {

            userId =
                    getCurrentUserId(
                            request
                    );

        } catch (Exception e) {

            return Result.fail(
                    e.getMessage()
            );
        }


        boolean admin =
                isAdmin(
                        userId
                );


        boolean archiveLeader =
                isArchiveLeader(
                        userId
                );


        Map<String, Object> data =
                new HashMap<>();


        data.put(
                "canExport",
                admin || archiveLeader
        );


        data.put(
                "isAdmin",
                admin
        );


        data.put(
                "isArchiveLeader",
                archiveLeader
        );


        return Result.success(
                data
        );
    }


    /* =========================================================
       获取学期
       ========================================================= */

    @GetMapping("/semesters")
    public Result<List<SysSemester>>
    semesters(
            HttpServletRequest request) {

        Long userId;


        try {

            userId =
                    getCurrentUserId(
                            request
                    );

        } catch (Exception e) {

            return Result.fail(
                    e.getMessage()
            );
        }


        /*
         * 权限检查
         */

        if (
                !canExport(
                        userId
                )
        ) {

            return Result.fail(
                    "你没有加减分导出权限"
            );
        }


        /*
         * 查询全部学期
         */

        List<SysSemester> list =
                sysSemesterMapper.selectList(
                        new LambdaQueryWrapper<SysSemester>()
                                .orderByDesc(
                                        SysSemester::getStartDate
                                )
                );


        return Result.success(
                list
        );
    }


    /* =========================================================
       Excel 导出
       ========================================================= */

    @GetMapping("/department")
    public void export(
            @RequestParam Long semesterId,
            @RequestParam String className,
            HttpServletRequest request,
            HttpServletResponse response) {

        Long userId;


        try {

            userId =
                    getCurrentUserId(
                            request
                    );

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    e.getMessage()
            );
        }


        /*
         * =====================================================
         * 权限检查
         * =====================================================
         */

        if (
                !canExport(
                        userId
                )
        ) {

            throw new IllegalArgumentException(
                    "你没有加减分导出权限"
            );
        }


        /*
         * =====================================================
         * 参数检查
         * =====================================================
         */

        if (semesterId == null) {

            throw new IllegalArgumentException(
                    "请选择学期"
            );
        }


        if (
                className == null
                        || className.trim().isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "请输入班级"
            );
        }


        /*
         * =====================================================
         * 检查学期
         * =====================================================
         */

        SysSemester semester =
                sysSemesterMapper.selectById(
                        semesterId
                );


        if (semester == null) {

            throw new IllegalArgumentException(
                    "学期不存在"
            );
        }


        /*
         * =====================================================
         * 开始导出
         * =====================================================
         */

        scoreExportService.export(
                semesterId,
                className.trim(),
                response
        );
    }
}