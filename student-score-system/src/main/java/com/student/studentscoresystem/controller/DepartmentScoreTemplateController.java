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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
     * =========================================================
     * 新增 / 维护部门自己的非固定活动
     *
     * 使用场景：
     *
     * 部门的临时活动（例如「秋季运动会」）没有固定模板，
     * 第一次提交申报时把活动名称、类型、分值填写一次，
     * 保存成该部门自己的活动模板，
     * 之后同部门再次提交就可以直接选择，不用重复输入。
     *
     * 说明：
     *
     * 1. 必须是该部门在职成员
     * 2. 同部门同名活动已存在时直接复用，
     *    分值 / 类型以本次填写的为准
     *    （已经提交的申报不受影响，
     *    申报单里保存的是提交当时的分值）
     * =========================================================
     */
    @PostMapping("/add")
    public Result<DepartmentScoreTemplate> add(
            @RequestBody DepartmentScoreTemplate request,
            HttpServletRequest httpRequest) {

        Long currentUserId;

        try {

            currentUserId =
                    getCurrentUserId(httpRequest);

        } catch (Exception e) {

            return Result.fail(e.getMessage());
        }

        if (request == null
                || request.getDepartmentId() == null) {

            return Result.fail("请选择部门");
        }

        /*
         * 必须是该部门在职成员
         */
        SysUserDepartment relation =
                userDepartmentMapper.selectOne(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getUserId,
                                        currentUserId
                                )
                                .eq(
                                        SysUserDepartment::getDepartmentId,
                                        request.getDepartmentId()
                                )
                                .eq(
                                        SysUserDepartment::getStatus,
                                        (short) 1
                                )
                );

        if (relation == null) {

            return Result.fail(
                    "你不是该部门成员，不能维护该部门的活动"
            );
        }

        /*
         * 活动名称
         */
        String name =
                request.getName() == null
                        ? ""
                        : request.getName().trim();

        if (name.isEmpty()) {

            return Result.fail("请输入活动名称");
        }

        if (name.length() > 200) {

            return Result.fail("活动名称不能超过 200 个字符");
        }

        /*
         * 加减分类型：1 加分，-1 减分
         */
        Short scoreType =
                request.getScoreType();

        if (scoreType == null
                || (scoreType != 1 && scoreType != -1)) {

            return Result.fail("请选择加分或减分");
        }

        /*
         * 分值
         */
        if (request.getScore() == null
                || request.getScore()
                .compareTo(BigDecimal.ZERO) <= 0) {

            return Result.fail("请输入大于 0 的分值");
        }

        BigDecimal score =
                request.getScore().setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        /*
         * 活动说明
         */
        String description =
                request.getDescription() == null
                        ? null
                        : request.getDescription().trim();

        if (description != null
                && description.length() > 1000) {

            return Result.fail("活动说明不能超过 1000 个字符");
        }

        LocalDateTime now =
                LocalDateTime.now();

        /*
         * 同部门同名活动：直接复用，
         * 分值 / 类型以本次填写的为准
         */
        DepartmentScoreTemplate exist =
                templateService.getOne(
                        new LambdaQueryWrapper<DepartmentScoreTemplate>()
                                .eq(
                                        DepartmentScoreTemplate::getDepartmentId,
                                        request.getDepartmentId()
                                )
                                .eq(
                                        DepartmentScoreTemplate::getName,
                                        name
                                )
                                .last("LIMIT 1")
                );

        if (exist != null) {

            exist.setScoreType(scoreType);

            exist.setScore(score);

            if (description != null) {

                exist.setDescription(description);
            }

            exist.setStatus((short) 1);

            exist.setUpdateTime(now);

            templateService.updateById(exist);

            return Result.success(exist);
        }

        DepartmentScoreTemplate template =
                new DepartmentScoreTemplate();

        template.setDepartmentId(
                request.getDepartmentId()
        );

        template.setName(name);

        template.setDescription(description);

        template.setScoreType(scoreType);

        template.setScore(score);

        template.setStatus((short) 1);

        template.setCreateTime(now);

        template.setUpdateTime(now);

        templateService.save(template);

        return Result.success(template);
    }

    /**
     * =========================================================
     * 部门活动列表（管理用，包含已停用）
     *
     * 用于提交页面上的「管理本部门活动」，
     * 部门可以自己把不再使用的临时活动停用。
     * =========================================================
     */
    @GetMapping("/manage")
    public Result<List<DepartmentScoreTemplate>> manage(
            @RequestParam Long departmentId,
            HttpServletRequest httpRequest) {

        Long currentUserId;

        try {

            currentUserId =
                    getCurrentUserId(httpRequest);

        } catch (Exception e) {

            return Result.fail(e.getMessage());
        }

        if (departmentId == null) {

            return Result.fail("请选择部门");
        }

        if (!isDepartmentMember(currentUserId, departmentId)) {

            return Result.fail(
                    "你不是该部门成员，不能管理该部门的活动"
            );
        }

        List<DepartmentScoreTemplate> list =
                templateService.list(
                        new LambdaQueryWrapper<DepartmentScoreTemplate>()
                                .eq(
                                        DepartmentScoreTemplate::getDepartmentId,
                                        departmentId
                                )
                                .orderByDesc(
                                        DepartmentScoreTemplate::getStatus
                                )
                                .orderByDesc(
                                        DepartmentScoreTemplate::getId
                                )
                );

        return Result.success(list);
    }

    /**
     * =========================================================
     * 停用 / 启用部门活动
     *
     * status = 0 停用
     * status = 1 启用
     * =========================================================
     */
    @PutMapping("/status/{id}")
    public Result<DepartmentScoreTemplate> updateStatus(
            @PathVariable Long id,
            @RequestParam Short status,
            HttpServletRequest httpRequest) {

        Long currentUserId;

        try {

            currentUserId =
                    getCurrentUserId(httpRequest);

        } catch (Exception e) {

            return Result.fail(e.getMessage());
        }

        if (id == null) {

            return Result.fail("活动ID不能为空");
        }

        if (status == null
                || (status != 0 && status != 1)) {

            return Result.fail("状态无效");
        }

        DepartmentScoreTemplate template =
                templateService.getById(id);

        if (template == null) {

            return Result.fail("活动不存在");
        }

        if (!isDepartmentMember(
                currentUserId,
                template.getDepartmentId()
        )) {

            return Result.fail(
                    "你不是该部门成员，不能修改该部门的活动"
            );
        }

        template.setStatus(status);

        template.setUpdateTime(LocalDateTime.now());

        templateService.updateById(template);

        return Result.success(template);
    }

    /**
     * =========================================================
     * 是否为该部门在职成员
     * =========================================================
     */
    private boolean isDepartmentMember(
            Long userId,
            Long departmentId) {

        if (userId == null || departmentId == null) {

            return false;
        }

        Long count =
                userDepartmentMapper.selectCount(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getUserId,
                                        userId
                                )
                                .eq(
                                        SysUserDepartment::getDepartmentId,
                                        departmentId
                                )
                                .eq(
                                        SysUserDepartment::getStatus,
                                        (short) 1
                                )
                );

        return count != null && count > 0;
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
