package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.DepartmentScoreTemplate;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.service.IDepartmentScoreTemplateService;
import com.student.studentscoresystem.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departmentScoreTemplate")
public class DepartmentScoreTemplateController {

    private final IDepartmentScoreTemplateService templateService;
    private final SysUserDepartmentMapper userDepartmentMapper;

    public DepartmentScoreTemplateController(
            IDepartmentScoreTemplateService templateService,
            SysUserDepartmentMapper userDepartmentMapper) {

        this.templateService = templateService;
        this.userDepartmentMapper = userDepartmentMapper;
    }

    /**
     * 获取当前用户指定部门的加减分模板
     *
     * 注意：
     * 1. 当前用户必须是该部门在职成员
     * 2. 只返回该部门自己的模板
     * 3. 只返回启用模板
     */
    @GetMapping("/list")
    public Result<List<DepartmentScoreTemplate>> list(
            @RequestParam Long departmentId,
            HttpServletRequest request) {

        Long currentUserId;

        try {
            currentUserId = getCurrentUserId(request);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }

        if (departmentId == null) {
            return Result.fail("请选择部门");
        }

        /*
         * 校验当前用户是否属于这个部门
         */
        SysUserDepartment relation =
                userDepartmentMapper.selectOne(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(SysUserDepartment::getUserId, currentUserId)
                                .eq(SysUserDepartment::getDepartmentId, departmentId)
                                .eq(SysUserDepartment::getStatus, (short) 1)
                );

        if (relation == null) {
            return Result.fail("你不是该部门成员，不能查看该部门申报模板");
        }

        /*
         * 只查询当前部门自己的启用模板
         *
         * 这里就是部门模板隔离的核心。
         */
        List<DepartmentScoreTemplate> list =
                templateService.list(
                        new LambdaQueryWrapper<DepartmentScoreTemplate>()
                                .eq(
                                        DepartmentScoreTemplate::getDepartmentId,
                                        departmentId
                                )
                                .eq(
                                        DepartmentScoreTemplate::getStatus,
                                        (short) 1
                                )
                                .orderByAsc(
                                        DepartmentScoreTemplate::getId
                                )
                );

        return Result.success(list);
    }

    /**
     * 当前登录用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {

        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("请先登录");
        }

        Claims claims =
                JwtUtil.parseToken(token.substring(7));

        return claims.get("userId", Long.class);
    }
}