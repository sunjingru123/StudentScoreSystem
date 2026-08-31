package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysSemester;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysSemesterMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
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


    public ScoreExportController(
            IScoreExportService scoreExportService,
            SysSemesterMapper sysSemesterMapper,
            SysUserDepartmentMapper userDepartmentMapper,
            DepartmentMapper departmentMapper) {

        this.scoreExportService =
                scoreExportService;

        this.sysSemesterMapper =
                sysSemesterMapper;

        this.userDepartmentMapper =
                userDepartmentMapper;

        this.departmentMapper =
                departmentMapper;
    }


    /* =========================================================
       获取当前用户ID
       ========================================================= */

    private Long getCurrentUserId(
            HttpServletRequest request) {

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
       获取当前用户角色
       ========================================================= */

    private String getCurrentUserRole(
            HttpServletRequest request) {

        /*
         * 登录过滤器如果已经把角色放进 request，
         * 优先使用。
         */

        Object roleAttr =
                request.getAttribute(
                        "userRole"
                );


        if (roleAttr != null) {

            return roleAttr.toString();
        }


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

            return null;
        }


        try {

            Claims claims =
                    JwtUtil.parseToken(
                            token.substring(7)
                    );


            /*
             * 登录接口目前返回 LoginVO.role，
             * JWT 中一般也会保存 role。
             */

            Object role =
                    claims.get("role");


            if (role == null) {

                /*
                 * 兼容 userRole
                 */

                role =
                        claims.get(
                                "userRole"
                        );
            }


            if (role == null) {
                return null;
            }


            return role.toString();


        } catch (Exception e) {

            return null;
        }
    }


    /* =========================================================
       判断管理员
       ========================================================= */

    private boolean isAdmin(
            HttpServletRequest request) {

        String role =
                getCurrentUserRole(
                        request
                );


        return "管理员".equals(role)
                || "ADMIN".equalsIgnoreCase(role)
                || "admin".equalsIgnoreCase(role);
    }


    /* =========================================================
       判断是否为档案部部长/副部长
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


            if (
                    !Short.valueOf((short) 1)
                            .equals(
                                    department.getStatus()
                            )
            ) {

                continue;
            }


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
       导出权限
       ========================================================= */

    private boolean canExport(
            Long userId,
            HttpServletRequest request) {

        /*
         * 管理员
         */

        if (
                isAdmin(request)
        ) {

            return true;
        }


        /*
         * 档案部部长 / 副部长
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
                        request
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


        if (
                !canExport(
                        userId,
                        request
                )
        ) {

            return Result.fail(
                    "你没有加减分导出权限"
            );
        }


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
       导出
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
         * 权限检查
         */

        if (
                !canExport(
                        userId,
                        request
                )
        ) {

            throw new IllegalArgumentException(
                    "你没有加减分导出权限"
            );
        }


        /*
         * 参数检查
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
         * 检查学期是否存在
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
         * 开始导出
         */

        scoreExportService.export(
                semesterId,
                className.trim(),
                response
        );
    }
}