package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.DepartmentScoreApplyAddDTO;
import com.student.studentscoresystem.dto.DepartmentScoreApplyAuditDTO;
import com.student.studentscoresystem.dto.DepartmentScoreFinalAuditDTO;
import com.student.studentscoresystem.entity.*;
import com.student.studentscoresystem.mapper.*;
import com.student.studentscoresystem.service.IDepartmentScoreApplyService;
import com.student.studentscoresystem.service.IDepartmentScoreTemplateService;
import com.student.studentscoresystem.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/departmentScoreApply")
public class DepartmentScoreApplyController {

    private final IDepartmentScoreApplyService applyService;
    private final IDepartmentScoreTemplateService templateService;
    private final SysUserDepartmentMapper userDepartmentMapper;
    private final ScoreRecordMapper scoreRecordMapper;
    private final DepartmentMapper departmentMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserPositionMapper sysUserPositionMapper;
    private final SysPositionMapper sysPositionMapper;

    public DepartmentScoreApplyController(
            IDepartmentScoreApplyService applyService,
            IDepartmentScoreTemplateService templateService,
            SysUserDepartmentMapper userDepartmentMapper,
            ScoreRecordMapper scoreRecordMapper,
            DepartmentMapper departmentMapper,
            SysUserMapper sysUserMapper,
            SysUserPositionMapper sysUserPositionMapper,
            SysPositionMapper sysPositionMapper) {

        this.applyService = applyService;
        this.templateService = templateService;
        this.userDepartmentMapper = userDepartmentMapper;
        this.scoreRecordMapper = scoreRecordMapper;
        this.departmentMapper = departmentMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysUserPositionMapper = sysUserPositionMapper;
        this.sysPositionMapper = sysPositionMapper;
    }

    /**
     * 【优化】获取当前登录用户ID
     * 增加属性检查和更安全的类型转换
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        // 优先从拦截器塞入的 request 属性中获取
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr != null) {
            return Long.valueOf(userIdAttr.toString());
        }

        // 兜底：手动解析 Header 里的 Token
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("请先登录");
        }

        try {
            Claims claims = JwtUtil.parseToken(token.substring(7));
            Object userId = claims.get("userId");
            return userId != null ? Long.valueOf(userId.toString()) : null;
        } catch (Exception e) {
            throw new IllegalArgumentException("登录状态无效，请重新登录");
        }
    }

    private boolean canApply(String position) {
        return "干事".equals(position) || "副部长".equals(position) || "部长".equals(position);
    }

    private boolean canAudit(String position) {
        return "副部长".equals(position) || "部长".equals(position);
    }

    /**
     * 提交部门加减分申请
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody DepartmentScoreApplyAddDTO dto, HttpServletRequest request) {
        Long currentUserId;
        try {
            currentUserId = getCurrentUserId(request);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }

        if (dto.getDepartmentId() == null) return Result.fail("请选择申报部门");
        if (dto.getStudentId() == null) return Result.fail("请选择需要加减分的学生");
        if (dto.getTemplateId() == null) return Result.fail("请选择加减分项目");

        // 校验申请人部门关系
        SysUserDepartment applicantRelation = userDepartmentMapper.selectOne(
                new LambdaQueryWrapper<SysUserDepartment>()
                        .eq(SysUserDepartment::getUserId, currentUserId)
                        .eq(SysUserDepartment::getDepartmentId, dto.getDepartmentId())
                        .eq(SysUserDepartment::getStatus, (short) 1)
        );

        if (applicantRelation == null || !canApply(applicantRelation.getPosition())) {
            return Result.fail("你没有权限提交该部门的加减分申报");
        }

        // 校验被申报学生
        SysUser student = sysUserMapper.selectById(dto.getStudentId());
        if (student == null || !Short.valueOf((short) 1).equals(student.getStatus())) {
            return Result.fail("被加减分学生不存在或状态异常");
        }

        // 校验模板
        DepartmentScoreTemplate template = templateService.getById(dto.getTemplateId());
        if (template == null || !Short.valueOf((short) 1).equals(template.getStatus())) {
            return Result.fail("所选加减分项目无效");
        }

        if (!dto.getDepartmentId().equals(template.getDepartmentId())) {
            return Result.fail("该项目不属于当前部门");
        }

        // 查重逻辑保持不变
        Long duplicateCount = applyService.count(new LambdaQueryWrapper<DepartmentScoreApply>()
                .eq(DepartmentScoreApply::getApplicantId, currentUserId)
                .eq(DepartmentScoreApply::getStudentId, dto.getStudentId())
                .eq(DepartmentScoreApply::getDepartmentId, dto.getDepartmentId())
                .eq(DepartmentScoreApply::getTemplateId, dto.getTemplateId())
                .and(w -> w.eq(DepartmentScoreApply::getStatus, (short) 0)
                        .or().eq(DepartmentScoreApply::getStatus, (short) 1)));

        if (duplicateCount > 0) return Result.fail("请勿重复提交相同申报");

        // 创建申请
        DepartmentScoreApply apply = new DepartmentScoreApply();
        apply.setStudentId(dto.getStudentId());
        apply.setApplicantId(currentUserId);
        apply.setDepartmentId(dto.getDepartmentId());
        apply.setTemplateId(dto.getTemplateId());
        apply.setScoreType(template.getScoreType());
        apply.setScore(template.getScore());
        apply.setTitle(template.getName().trim());
        apply.setDescription(template.getDescription());
        apply.setEvidenceUrl(dto.getEvidenceUrl());
        apply.setStatus((short) 0);
        apply.setFinalStatus((short) 0);
        apply.setCreateTime(LocalDateTime.now());
        apply.setUpdateTime(LocalDateTime.now());

        applyService.save(apply);
        return Result.success(null);
    }

    @PostMapping("/apply")
    public Result<Void> apply(@RequestBody DepartmentScoreApplyAddDTO dto, HttpServletRequest request) {
        return add(dto, request);
    }

    @GetMapping("/my-permissions")
    public Result<Map<String, Object>> myPermissions(HttpServletRequest request) {
        Long currentUserId;
        try {
            currentUserId = getCurrentUserId(request);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }

        List<SysUserDepartment> relations = userDepartmentMapper.selectList(
                new LambdaQueryWrapper<SysUserDepartment>()
                        .eq(SysUserDepartment::getUserId, currentUserId)
                        .eq(SysUserDepartment::getStatus, (short) 1)
                        .in(SysUserDepartment::getPosition, List.of("干事", "副部长", "部长"))
        );

        List<Map<String, Object>> departments = new ArrayList<>();
        boolean canDepartmentAudit = false;

        for (SysUserDepartment relation : relations) {
            Department department = departmentMapper.selectById(relation.getDepartmentId());
            if (department != null && Short.valueOf((short) 1).equals(department.getStatus())) {
                Map<String, Object> item = new HashMap<>();
                item.put("departmentId", relation.getDepartmentId());
                item.put("departmentName", department.getName());
                item.put("position", relation.getPosition());
                departments.add(item);
                if (canAudit(relation.getPosition())) canDepartmentAudit = true;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("canDepartmentApply", !departments.isEmpty());
        data.put("canDepartmentAudit", canDepartmentAudit);
        data.put("departments", departments);
        return Result.success(data);
    }

    @GetMapping("/my")
    public Result<List<DepartmentScoreApply>> my(HttpServletRequest request) {
        Long currentUserId;
        try {
            currentUserId = getCurrentUserId(request);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }

        List<DepartmentScoreApply> list = applyService.list(new LambdaQueryWrapper<DepartmentScoreApply>()
                .eq(DepartmentScoreApply::getApplicantId, currentUserId)
                .orderByDesc(DepartmentScoreApply::getCreateTime));

        fillNames(list);
        return Result.success(list);
    }

    @GetMapping("/my-list")
    public Result<List<DepartmentScoreApply>> myList(HttpServletRequest request) {
        return my(request);
    }

    @GetMapping("/audit/list")
    public Result<List<DepartmentScoreApply>> auditList(HttpServletRequest request) {
        Long currentUserId;
        try {
            currentUserId = getCurrentUserId(request);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }

        List<SysUserDepartment> myPositions = userDepartmentMapper.selectList(new LambdaQueryWrapper<SysUserDepartment>()
                .eq(SysUserDepartment::getUserId, currentUserId)
                .eq(SysUserDepartment::getStatus, (short) 1)
                .in(SysUserDepartment::getPosition, List.of("部长", "副部长")));

        if (myPositions.isEmpty()) return Result.success(List.of());

        List<Long> departmentIds = myPositions.stream().map(SysUserDepartment::getDepartmentId).collect(Collectors.toList());

        List<DepartmentScoreApply> list = applyService.list(new LambdaQueryWrapper<DepartmentScoreApply>()
                .in(DepartmentScoreApply::getDepartmentId, departmentIds)
                .eq(DepartmentScoreApply::getStatus, (short) 0)
                .ne(DepartmentScoreApply::getApplicantId, currentUserId) // 不看自己的
                .orderByDesc(DepartmentScoreApply::getCreateTime));

        fillNames(list);
        return Result.success(list);
    }

    @PutMapping("/audit/{id}")
    public Result<Void> audit(@PathVariable Long id, @RequestBody DepartmentScoreApplyAuditDTO dto, HttpServletRequest request) {
        Long currentUserId;
        try {
            currentUserId = getCurrentUserId(request);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }

        DepartmentScoreApply apply = applyService.getById(id);
        if (apply == null || !Short.valueOf((short) 0).equals(apply.getStatus())) {
            return Result.fail("申报记录无效或已处理");
        }

        // 更新审核信息
        apply.setStatus(dto.getStatus());
        apply.setReviewerId(currentUserId);
        apply.setReviewRemark(dto.getReviewRemark());
        apply.setReviewTime(LocalDateTime.now());
        apply.setUpdateTime(LocalDateTime.now());
        applyService.updateById(apply);

        return Result.success(null);
    }

    @GetMapping("/final-audit/list")
    public Result<List<DepartmentScoreApply>> finalAuditList(HttpServletRequest request) {
        Long currentUserId;
        try {
            currentUserId = getCurrentUserId(request);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }

        List<Department> departments = departmentMapper.selectList(new LambdaQueryWrapper<Department>()
                .eq(Department::getTeacherId, currentUserId)
                .eq(Department::getStatus, (short) 1));

        if (departments.isEmpty()) return Result.success(List.of());

        List<Long> departmentIds = departments.stream().map(Department::getId).collect(Collectors.toList());

        List<DepartmentScoreApply> list = applyService.list(new LambdaQueryWrapper<DepartmentScoreApply>()
                .in(DepartmentScoreApply::getDepartmentId, departmentIds)
                .eq(DepartmentScoreApply::getStatus, (short) 1)
                .eq(DepartmentScoreApply::getFinalStatus, (short) 0)
                .orderByDesc(DepartmentScoreApply::getCreateTime));

        fillNames(list);
        return Result.success(list);
    }
    /**
     * 终审已处理列表
     *
     * finalStatus:
     * 1 = 终审通过
     * 2 = 终审驳回
     */
    @GetMapping("/final-audit/processed")
    public Result<List<DepartmentScoreApply>> finalAuditProcessed(
            HttpServletRequest request
    ) {

        Long currentUserId;

        try {
            currentUserId = getCurrentUserId(request);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }

        // 查询当前辅导员负责的部门
        List<Department> departments =
                departmentMapper.selectList(
                        new LambdaQueryWrapper<Department>()
                                .eq(
                                        Department::getTeacherId,
                                        currentUserId
                                )
                                .eq(
                                        Department::getStatus,
                                        (short) 1
                                )
                );

        if (departments == null || departments.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        List<Long> departmentIds =
                departments.stream()
                        .map(Department::getId)
                        .collect(Collectors.toList());

        /*
         * 查询已经终审处理过的申请
         *
         * finalStatus = 1：终审通过
         * finalStatus = 2：终审驳回
         */
        List<DepartmentScoreApply> list =
                applyService.list(
                        new LambdaQueryWrapper<DepartmentScoreApply>()
                                .in(
                                        DepartmentScoreApply::getDepartmentId,
                                        departmentIds
                                )
                                .in(
                                        DepartmentScoreApply::getFinalStatus,
                                        List.of(
                                                (short) 1,
                                                (short) 2
                                        )
                                )
                                .orderByDesc(
                                        DepartmentScoreApply::getFinalReviewTime
                                )
                );

        fillNames(list);

        return Result.success(list);
    }
    @GetMapping("/final-audit/history")
    public Result<List<DepartmentScoreApply>> finalAuditHistory(
            HttpServletRequest request
    ) {

        Long currentUserId;

        try {
            currentUserId = getCurrentUserId(request);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }

        // 1. 查询当前辅导员负责的部门
        List<Department> departments =
                departmentMapper.selectList(
                        new LambdaQueryWrapper<Department>()
                                .eq(
                                        Department::getTeacherId,
                                        currentUserId
                                )
                                .eq(
                                        Department::getStatus,
                                        (short) 1
                                )
                );

        if (departments == null || departments.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 2. 获取部门 ID
        List<Long> departmentIds =
                departments.stream()
                        .map(Department::getId)
                        .collect(Collectors.toList());

        // 3. 查询已经终审的记录
        List<DepartmentScoreApply> list =
                applyService.list(
                        new LambdaQueryWrapper<DepartmentScoreApply>()
                                .in(
                                        DepartmentScoreApply::getDepartmentId,
                                        departmentIds
                                )
                                .in(
                                        DepartmentScoreApply::getFinalStatus,
                                        List.of(
                                                (short) 1,
                                                (short) 2
                                        )
                                )
                                .orderByDesc(
                                        DepartmentScoreApply::getFinalReviewTime
                                )
                );

        fillNames(list);

        return Result.success(list);
    }
    @PutMapping("/final-audit/{id}")
    public Result<Void> finalAudit(
            @PathVariable Long id,
            @RequestBody DepartmentScoreFinalAuditDTO dto,
            HttpServletRequest request
    ) {

        Long currentUserId;

        try {
            currentUserId = getCurrentUserId(request);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }

        // 1. 检查参数
        if (dto == null || dto.getStatus() == null) {
            return Result.fail("终审状态不能为空");
        }

        Short finalStatus = dto.getStatus();

        if (finalStatus == null) {
            return Result.fail("终审状态不能为空");
        }

        if (finalStatus != 1 && finalStatus != 2) {
            return Result.fail("终审状态无效");
        }

        // 2. 查询申请
        DepartmentScoreApply apply =
                applyService.getById(id);

        if (apply == null) {
            return Result.fail("申报记录不存在");
        }

        // 3. 必须是部门审核通过
        if (!Short.valueOf((short) 1).equals(apply.getStatus())) {
            return Result.fail("该申报尚未通过部门审核，不能终审");
        }

        // 4. 必须是待终审
        if (!Short.valueOf((short) 0).equals(apply.getFinalStatus())) {
            return Result.fail("该申报已经终审，不能重复处理");
        }

        // 5. 更新终审状态
        apply.setFinalStatus(finalStatus);
        apply.setFinalReviewerId(currentUserId);
        apply.setFinalReviewRemark(dto.getReviewRemark());
        apply.setFinalReviewTime(LocalDateTime.now());
        apply.setUpdateTime(LocalDateTime.now());

        boolean updated = applyService.updateById(apply);

        if (!updated) {
            return Result.fail("终审状态更新失败");
        }

        // =====================================================
        // 6. 终审通过 → 生成成绩记录
        // =====================================================

        if (finalStatus == 1) {

            BigDecimal realScore = apply.getScore();

            // scoreType = -1 表示减分
            if (Short.valueOf((short) -1).equals(apply.getScoreType())) {
                realScore = realScore.negate();
            }

            ScoreRecord record = new ScoreRecord();

            record.setStudentId(apply.getStudentId());

            record.setScore(realScore);

            record.setSourceType("DEPARTMENT");

            record.setSourceId(apply.getId());

            // ★★★★★ 关键
            record.setStatus((short) 1);

            // 学生可见
            record.setAdminHidden((short) 0);

            record.setCreateTime(LocalDateTime.now());

            int result =
                    scoreRecordMapper.insert(record);

            if (result <= 0) {
                return Result.fail("终审通过，但成绩记录生成失败");
            }
        }

        return Result.success(null);
    }
    /**
     * 【深度优化】给前端补充数据
     * 解决姓名、学号、部门名称显示为 null 的问题
     */
    private void fillNames(List<DepartmentScoreApply> list) {
        if (list == null || list.isEmpty()) return;
        for (DepartmentScoreApply item : list) {
            // 补全被申报学生信息
            if (item.getStudentId() != null) {
                SysUser student = sysUserMapper.selectById(item.getStudentId());
                if (student != null) {
                    item.setStudentName(student.getRealName());
                    // 如果 Entity 扩展了字段，建议此处也设置学号
                    // item.setStudentNo(student.getStudentNo());
                }
            }
            // 补全部门名称
            if (item.getDepartmentId() != null) {
                Department department = departmentMapper.selectById(item.getDepartmentId());
                if (department != null) {
                    item.setDepartmentName(department.getName());
                }
            }
        }
    }
}