package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/archive")
public class ArchiveController {

    private final SysUserDepartmentMapper userDepartmentMapper;
    private final DepartmentMapper departmentMapper;

    public ArchiveController(
            SysUserDepartmentMapper userDepartmentMapper,
            DepartmentMapper departmentMapper) {

        this.userDepartmentMapper = userDepartmentMapper;
        this.departmentMapper = departmentMapper;
    }


    /* =========================================================
       获取当前登录用户 ID
       ========================================================= */

    private Long getCurrentUserId(
            HttpServletRequest request) {

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


        String token =
                request.getHeader("Authorization");


        if (token == null
                || !token.startsWith("Bearer ")) {

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
       判断是否为档案部部长 / 副部长
       ========================================================= */

    private boolean isArchiveLeader(
            Long userId) {

        if (userId == null) {

            return false;
        }


        /*
         * 查询当前用户所有在职部门关系
         */

        List<SysUserDepartment> relations =
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
                );


        if (relations == null
                || relations.isEmpty()) {

            return false;
        }


        /*
         * 判断：
         *
         * 部门 = 档案部
         *
         * 并且
         *
         * 职位 = 部长 / 副部长
         */

        for (SysUserDepartment relation :
                relations) {

            String position =
                    relation.getPosition();


            if (!"部长".equals(position)
                    && !"副部长".equals(position)) {

                continue;
            }


            Department department =
                    departmentMapper.selectById(
                            relation.getDepartmentId()
                    );


            if (department == null) {

                continue;
            }


            if (!Short.valueOf((short) 1)
                    .equals(department.getStatus())) {

                continue;
            }


            if ("档案部".equals(
                    department.getName()
            )) {

                return true;
            }
        }


        return false;
    }


    /* =========================================================
       档案导出权限
       ========================================================= */

    @GetMapping("/export-permission")
    public Result<Map<String, Object>>
    exportPermission(
            HttpServletRequest request) {

        Long currentUserId;


        try {

            currentUserId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(
                    e.getMessage()
            );
        }


        boolean canExport =
                isArchiveLeader(
                        currentUserId
                );


        Map<String, Object> data =
                new HashMap<>();


        data.put(
                "canExport",
                canExport
        );


        return Result.success(data);
    }
}