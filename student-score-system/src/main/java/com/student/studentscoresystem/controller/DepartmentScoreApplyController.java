package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.DepartmentScoreApplyAddDTO;
import com.student.studentscoresystem.dto.DepartmentScoreApplyAuditDTO;
import com.student.studentscoresystem.dto.DepartmentScoreFinalAuditDTO;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.entity.DepartmentScoreTemplate;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.IDepartmentScoreApplyService;
import com.student.studentscoresystem.service.IDepartmentScoreTemplateService;
import com.student.studentscoresystem.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
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

    public DepartmentScoreApplyController(
            IDepartmentScoreApplyService applyService,
            IDepartmentScoreTemplateService templateService,
            SysUserDepartmentMapper userDepartmentMapper,
            ScoreRecordMapper scoreRecordMapper,
            DepartmentMapper departmentMapper,
            SysUserMapper sysUserMapper) {

        this.applyService = applyService;
        this.templateService = templateService;
        this.userDepartmentMapper = userDepartmentMapper;
        this.scoreRecordMapper = scoreRecordMapper;
        this.departmentMapper = departmentMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /* =========================================================
       获取当前登录用户 ID
       ========================================================= */

    private Long getCurrentUserId(HttpServletRequest request) {

        Object userIdAttr = request.getAttribute("userId");

        if (userIdAttr != null) {

            try {
                return Long.valueOf(userIdAttr.toString());
            } catch (Exception ignored) {
            }
        }

        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("请先登录");
        }

        try {

            Claims claims =
                    JwtUtil.parseToken(token.substring(7));

            Object userId =
                    claims.get("userId");

            if (userId == null) {
                throw new IllegalArgumentException("登录状态无效");
            }

            return Long.valueOf(userId.toString());

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "登录状态无效，请重新登录"
            );
        }
    }


    /* =========================================================
       部门申报权限
       ========================================================= */

    private boolean canApply(String position) {

        return "干事".equals(position)
                || "副部长".equals(position)
                || "部长".equals(position);
    }


    /* =========================================================
       部门审核权限
       ========================================================= */

    private boolean canAudit(String position) {

        return "副部长".equals(position)
                || "部长".equals(position);
    }


    /* =========================================================
       提交部门加减分申报
       ========================================================= */

    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody DepartmentScoreApplyAddDTO dto,
            HttpServletRequest request) {

        Long applicantId;

        try {

            applicantId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(e.getMessage());
        }


        /* =========================
           参数检查
           ========================= */

        if (dto == null) {
            return Result.fail("申报参数不能为空");
        }

        if (applicantId == null) {
            return Result.fail("请先登录");
        }

        if (dto.getDepartmentId() == null) {
            return Result.fail("请选择申报部门");
        }

        if (dto.getStudentId() == null) {
            return Result.fail("请选择被加减分学生");
        }

        if (dto.getTemplateId() == null) {
            return Result.fail("请选择加减分项目");
        }


        /* =========================
           检查申报人部门关系
           ========================= */

        SysUserDepartment relation =
                userDepartmentMapper.selectOne(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getUserId,
                                        applicantId
                                )
                                .eq(
                                        SysUserDepartment::getDepartmentId,
                                        dto.getDepartmentId()
                                )
                                .eq(
                                        SysUserDepartment::getStatus,
                                        (short) 1
                                )
                );


        if (relation == null) {

            return Result.fail(
                    "你不是该部门在职成员，不能提交该部门申报"
            );
        }


        if (!canApply(relation.getPosition())) {

            return Result.fail(
                    "你没有部门加减分申报权限"
            );
        }


        /* =========================
           防止给自己申报
           ========================= */

        if (applicantId.equals(dto.getStudentId())) {

            return Result.fail(
                    "不能给自己提交部门加减分申报"
            );
        }


        /* =========================
           查询学生
           ========================= */

        SysUser student =
                sysUserMapper.selectById(
                        dto.getStudentId()
                );


        if (student == null) {

            return Result.fail(
                    "被申报学生不存在"
            );
        }


        if (!Short.valueOf((short) 1)
                .equals(student.getStatus())) {

            return Result.fail(
                    "被申报学生当前状态异常"
            );
        }


        /* =========================
           查询模板
           ========================= */

        DepartmentScoreTemplate template =
                templateService.getById(
                        dto.getTemplateId()
                );


        if (template == null) {

            return Result.fail(
                    "所选加减分项目不存在"
            );
        }


        if (!Short.valueOf((short) 1)
                .equals(template.getStatus())) {

            return Result.fail(
                    "所选加减分项目已经停用"
            );
        }


        /* =========================
           模板必须属于当前部门
           ========================= */

        if (!dto.getDepartmentId()
                .equals(template.getDepartmentId())) {

            return Result.fail(
                    "该加减分项目不属于当前申报部门"
            );
        }


        /* =========================
           检查模板分值
           ========================= */

        if (template.getScoreType() == null) {

            return Result.fail(
                    "该模板未配置加减分类型"
            );
        }


        if (template.getScoreType() != 1
                && template.getScoreType() != -1) {

            return Result.fail(
                    "该模板加减分类型配置错误"
            );
        }


        if (template.getScore() == null
                || template.getScore()
                .compareTo(BigDecimal.ZERO) <= 0) {

            return Result.fail(
                    "该模板分值配置错误"
            );
        }


        if (template.getName() == null
                || template.getName().trim().isEmpty()) {

            return Result.fail(
                    "该模板名称为空"
            );
        }


        /* =====================================================
           防止重复申报

           同一个：
           申报人
           +
           学生
           +
           部门
           +
           模板

           如果当前仍处于流程中，则不能重复申报。

           status = 0
           → 等待部门审核

           status = 1 && finalStatus = 0
           → 等待辅导员审核

           status = 1 && finalStatus = 1
           → 已最终通过
           ===================================================== */

        LambdaQueryWrapper<DepartmentScoreApply>
                duplicateWrapper =
                new LambdaQueryWrapper<>();


        duplicateWrapper
                .eq(
                        DepartmentScoreApply::getApplicantId,
                        applicantId
                )
                .eq(
                        DepartmentScoreApply::getStudentId,
                        dto.getStudentId()
                )
                .eq(
                        DepartmentScoreApply::getDepartmentId,
                        dto.getDepartmentId()
                )
                .eq(
                        DepartmentScoreApply::getTemplateId,
                        dto.getTemplateId()
                )
                .and(wrapper ->
                        wrapper
                                .eq(
                                        DepartmentScoreApply::getStatus,
                                        (short) 0
                                )
                                .or()
                                .and(w ->
                                        w.eq(
                                                        DepartmentScoreApply::getStatus,
                                                        (short) 1
                                                )
                                                .eq(
                                                        DepartmentScoreApply::getFinalStatus,
                                                        (short) 0
                                                )
                                )
                                .or()
                                .and(w ->
                                        w.eq(
                                                        DepartmentScoreApply::getStatus,
                                                        (short) 1
                                                )
                                                .eq(
                                                        DepartmentScoreApply::getFinalStatus,
                                                        (short) 1
                                                )
                                )
                );


        Long duplicateCount =
                applyService.count(
                        duplicateWrapper
                );


        if (duplicateCount != null
                && duplicateCount > 0) {

            return Result.fail(
                    "该学生的该加减分项目已经申报，请勿重复提交"
            );
        }


        /* =====================================================
           创建申报

           ★★★ 这里最关键 ★★★

           studentId
           = 被加减分学生

           applicantId
           = 当前登录的部门干部

           两个绝对不能写反。
           ===================================================== */

        DepartmentScoreApply apply =
                new DepartmentScoreApply();


        apply.setStudentId(
                dto.getStudentId()
        );


        apply.setApplicantId(
                applicantId
        );


        apply.setDepartmentId(
                dto.getDepartmentId()
        );


        apply.setTemplateId(
                dto.getTemplateId()
        );


        apply.setScoreType(
                template.getScoreType()
        );


        apply.setScore(
                template.getScore()
        );


        apply.setTitle(
                template.getName().trim()
        );


        apply.setDescription(
                template.getDescription()
        );


        apply.setEvidenceUrl(
                dto.getEvidenceUrl()
        );


        /*
         * 部门初审：
         *
         * 0 = 待审核
         */

        apply.setStatus(
                (short) 0
        );


        /*
         * 辅导员终审：
         *
         * 0 = 待审核
         */

        apply.setFinalStatus(
                (short) 0
        );


        LocalDateTime now =
                LocalDateTime.now();


        apply.setCreateTime(now);

        apply.setUpdateTime(now);


        /* =====================================================
           保存
           ===================================================== */

        boolean saved =
                applyService.save(apply);


        if (!saved) {

            return Result.fail(
                    "部门加减分申报保存失败"
            );
        }


        /*
         * 保存后检查 ID
         */

        if (apply.getId() == null) {

            return Result.fail(
                    "申报保存失败：未生成申报记录ID"
            );
        }


        System.out.println(
                "========== 部门申报保存成功 =========="
        );

        System.out.println(
                "applyId = " + apply.getId()
        );

        System.out.println(
                "studentId = " + apply.getStudentId()
        );

        System.out.println(
                "applicantId = " + apply.getApplicantId()
        );

        System.out.println(
                "departmentId = " + apply.getDepartmentId()
        );

        System.out.println(
                "templateId = " + apply.getTemplateId()
        );

        System.out.println(
                "scoreType = " + apply.getScoreType()
        );

        System.out.println(
                "score = " + apply.getScore()
        );

        System.out.println(
                "status = " + apply.getStatus()
        );

        System.out.println(
                "finalStatus = " + apply.getFinalStatus()
        );

        System.out.println(
                "======================================"
        );


        return Result.success(null);
    }


    /* =========================================================
       兼容 /apply
       ========================================================= */

    @PostMapping("/apply")
    public Result<Void> apply(
            @RequestBody DepartmentScoreApplyAddDTO dto,
            HttpServletRequest request) {

        return add(dto, request);
    }


    /* =========================================================
       当前用户部门权限
       ========================================================= */

    @GetMapping("/my-permissions")
    public Result<Map<String, Object>> myPermissions(
            HttpServletRequest request) {

        Long currentUserId;

        try {

            currentUserId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(e.getMessage());
        }


        List<SysUserDepartment> relations =
                userDepartmentMapper.selectList(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getUserId,
                                        currentUserId
                                )
                                .eq(
                                        SysUserDepartment::getStatus,
                                        (short) 1
                                )
                                .in(
                                        SysUserDepartment::getPosition,
                                        List.of(
                                                "干事",
                                                "副部长",
                                                "部长"
                                        )
                                )
                );


        List<Map<String, Object>>
                departments =
                new ArrayList<>();


        boolean canDepartmentAudit =
                false;


        for (SysUserDepartment relation :
                relations) {

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


            Map<String, Object> item =
                    new HashMap<>();


            item.put(
                    "departmentId",
                    relation.getDepartmentId()
            );


            item.put(
                    "departmentName",
                    department.getName()
            );


            item.put(
                    "position",
                    relation.getPosition()
            );


            departments.add(item);


            if (canAudit(
                    relation.getPosition()
            )) {

                canDepartmentAudit =
                        true;
            }
        }


        Map<String, Object> data =
                new HashMap<>();


        data.put(
                "canDepartmentApply",
                !departments.isEmpty()
        );


        data.put(
                "canDepartmentAudit",
                canDepartmentAudit
        );


        data.put(
                "departments",
                departments
        );


        return Result.success(data);
    }


    /* =========================================================
       我的申报记录
       ========================================================= */

    @GetMapping("/my")
    public Result<List<DepartmentScoreApply>> my(
            HttpServletRequest request) {

        Long currentUserId;

        try {

            currentUserId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(e.getMessage());
        }


        /*
         * ★★★ 最关键 ★★★
         *
         * 我的申报
         *
         * 必须按照 applicantId 查询。
         *
         * studentId 是被加减分学生，
         * 不能拿 studentId 查询“我的申报”。
         */

        List<DepartmentScoreApply> list =
                applyService.list(
                        new LambdaQueryWrapper<DepartmentScoreApply>()
                                .eq(
                                        DepartmentScoreApply::getApplicantId,
                                        currentUserId
                                )
                                .orderByDesc(
                                        DepartmentScoreApply::getCreateTime
                                )
                );


        fillNames(list);


        return Result.success(list);
    }


    /* =========================================================
       兼容 /my-list
       ========================================================= */

    @GetMapping("/my-list")
    public Result<List<DepartmentScoreApply>> myList(
            HttpServletRequest request) {

        return my(request);
    }


    /* =========================================================
       部门干部待审核
       ========================================================= */

    @GetMapping("/audit/list")
    public Result<List<DepartmentScoreApply>> auditList(
            HttpServletRequest request) {

        Long currentUserId;

        try {

            currentUserId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(e.getMessage());
        }


        /*
         * 查询当前用户担任副部长 / 部长的部门
         */

        List<SysUserDepartment>
                myPositions =
                userDepartmentMapper.selectList(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getUserId,
                                        currentUserId
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


        if (myPositions.isEmpty()) {

            return Result.success(
                    new ArrayList<>()
            );
        }


        List<Long> departmentIds =
                myPositions.stream()
                        .map(
                                SysUserDepartment::getDepartmentId
                        )
                        .distinct()
                        .collect(
                                Collectors.toList()
                        );


        /*
         * ★★★
         *
         * 只查询：
         *
         * 自己负责的部门
         *
         * +
         *
         * status = 0
         *
         * 即待部门审核
         */

        List<DepartmentScoreApply> list =
                applyService.list(
                        new LambdaQueryWrapper<DepartmentScoreApply>()
                                .in(
                                        DepartmentScoreApply::getDepartmentId,
                                        departmentIds
                                )
                                .eq(
                                        DepartmentScoreApply::getStatus,
                                        (short) 0
                                )
                                .orderByDesc(
                                        DepartmentScoreApply::getCreateTime
                                )
                );


        /*
         * 这里不在查询阶段过滤 applicantId。
         *
         * 如果副部长本人提交了申报，
         * 他可以看到这条申请。
         *
         * 但真正点击审核时，
         * audit() 会阻止审核自己的申请。
         */

        fillNames(list);


        return Result.success(list);
    }


    /* =========================================================
       部门审核
       ========================================================= */

    @PutMapping("/audit/{id}")
    public Result<Void> audit(
            @PathVariable Long id,
            @RequestBody DepartmentScoreApplyAuditDTO dto,
            HttpServletRequest request) {

        Long currentUserId;

        try {

            currentUserId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(e.getMessage());
        }


        if (dto == null
                || dto.getStatus() == null) {

            return Result.fail(
                    "审核状态不能为空"
            );
        }


        Short status =
                dto.getStatus();


        if (status != 1
                && status != 2) {

            return Result.fail(
                    "审核状态无效"
            );
        }


        /* =========================
           查询申请
           ========================= */

        DepartmentScoreApply apply =
                applyService.getById(id);


        if (apply == null) {

            return Result.fail(
                    "申报记录不存在"
            );
        }


        /*
         * 只能审核待部门审核
         */

        if (!Short.valueOf((short) 0)
                .equals(apply.getStatus())) {

            return Result.fail(
                    "该申报已经完成部门审核"
            );
        }


        /* =========================
           审核人必须是该部门副部长/部长
           ========================= */

        SysUserDepartment reviewerRelation =
                userDepartmentMapper.selectOne(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getUserId,
                                        currentUserId
                                )
                                .eq(
                                        SysUserDepartment::getDepartmentId,
                                        apply.getDepartmentId()
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


        if (reviewerRelation == null) {

            return Result.fail(
                    "你不是该部门的副部长或部长，没有审核权限"
            );
        }


        /* =========================
           不能审核自己的申报
           ========================= */

        if (currentUserId.equals(
                apply.getApplicantId()
        )) {

            return Result.fail(
                    "不能审核自己提交的申报"
            );
        }


        /* =========================
           更新
           ========================= */

        apply.setStatus(status);

        apply.setReviewerId(
                currentUserId
        );

        apply.setReviewRemark(
                dto.getReviewRemark()
        );

        apply.setReviewTime(
                LocalDateTime.now()
        );

        apply.setUpdateTime(
                LocalDateTime.now()
        );


        boolean updated =
                applyService.updateById(
                        apply
                );


        if (!updated) {

            return Result.fail(
                    "部门审核处理失败"
            );
        }


        return Result.success(null);
    }


    /* =========================================================
       辅导员待终审
       ========================================================= */

    @GetMapping("/final-audit/list")
    public Result<List<DepartmentScoreApply>>
    finalAuditList(
            HttpServletRequest request) {

        Long currentUserId;

        try {

            currentUserId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(e.getMessage());
        }


        /*
         * 查询当前辅导员负责的部门
         */

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


        if (departments.isEmpty()) {

            return Result.success(
                    new ArrayList<>()
            );
        }


        List<Long> departmentIds =
                departments.stream()
                        .map(Department::getId)
                        .distinct()
                        .collect(
                                Collectors.toList()
                        );


        /*
         * 只查询：
         *
         * 部门已经审核通过
         *
         * +
         *
         * 辅导员还没有审核
         */

        List<DepartmentScoreApply> list =
                applyService.list(
                        new LambdaQueryWrapper<DepartmentScoreApply>()
                                .in(
                                        DepartmentScoreApply::getDepartmentId,
                                        departmentIds
                                )
                                .eq(
                                        DepartmentScoreApply::getStatus,
                                        (short) 1
                                )
                                .eq(
                                        DepartmentScoreApply::getFinalStatus,
                                        (short) 0
                                )
                                .orderByDesc(
                                        DepartmentScoreApply::getCreateTime
                                )
                );


        fillNames(list);


        return Result.success(list);
    }


    /* =========================================================
       辅导员已处理
       ========================================================= */

    @GetMapping("/final-audit/processed")
    public Result<List<DepartmentScoreApply>>
    finalAuditProcessed(
            HttpServletRequest request) {

        Long currentUserId;

        try {

            currentUserId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(e.getMessage());
        }


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


        if (departments.isEmpty()) {

            return Result.success(
                    new ArrayList<>()
            );
        }


        List<Long> departmentIds =
                departments.stream()
                        .map(Department::getId)
                        .distinct()
                        .collect(
                                Collectors.toList()
                        );


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


    /* =========================================================
       辅导员历史
       ========================================================= */

    @GetMapping("/final-audit/history")
    public Result<List<DepartmentScoreApply>>
    finalAuditHistory(
            HttpServletRequest request) {

        return finalAuditProcessed(request);
    }


    /* =========================================================
       辅导员终审
       ========================================================= */
    @Transactional
    @PutMapping("/final-audit/{id}")
    public Result<Void> finalAudit(
            @PathVariable Long id,
            @RequestBody DepartmentScoreFinalAuditDTO dto,
            HttpServletRequest request) {

        Long currentUserId;

        try {

            currentUserId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(e.getMessage());
        }


        if (dto == null
                || dto.getStatus() == null) {

            return Result.fail(
                    "终审状态不能为空"
            );
        }


        Short finalStatus =
                dto.getStatus();


        if (finalStatus != 1
                && finalStatus != 2) {

            return Result.fail(
                    "终审状态无效"
            );
        }


        /* =========================
           查询申请
           ========================= */

        DepartmentScoreApply apply =
                applyService.getById(id);


        if (apply == null) {

            return Result.fail(
                    "申报记录不存在"
            );
        }


        /* =========================
           必须已经部门审核通过
           ========================= */

        if (!Short.valueOf((short) 1)
                .equals(apply.getStatus())) {

            return Result.fail(
                    "该申报尚未通过部门审核"
            );
        }


        /* =========================
           必须等待终审
           ========================= */

        if (!Short.valueOf((short) 0)
                .equals(apply.getFinalStatus())) {

            return Result.fail(
                    "该申报已经完成终审"
            );
        }


        /* =========================
           检查辅导员是否负责该部门
           ========================= */

        Department department =
                departmentMapper.selectOne(
                        new LambdaQueryWrapper<Department>()
                                .eq(
                                        Department::getId,
                                        apply.getDepartmentId()
                                )
                                .eq(
                                        Department::getTeacherId,
                                        currentUserId
                                )
                                .eq(
                                        Department::getStatus,
                                        (short) 1
                                )
                );


        if (department == null) {

            return Result.fail(
                    "你不是该部门的负责辅导员"
            );
        }


        /* =========================
           更新终审
           ========================= */

        apply.setFinalStatus(
                finalStatus
        );

        apply.setFinalReviewerId(
                currentUserId
        );

        apply.setFinalReviewRemark(
                dto.getReviewRemark()
        );

        apply.setFinalReviewTime(
                LocalDateTime.now()
        );

        apply.setUpdateTime(
                LocalDateTime.now()
        );


        boolean updated =
                applyService.updateById(
                        apply
                );


        if (!updated) {

            return Result.fail(
                    "终审状态更新失败"
            );
        }


        /* =====================================================
           终审通过
           → 生成正式成绩记录
           ===================================================== */

        if (finalStatus == 1) {

            BigDecimal realScore = apply.getScore();

            /*
             * scoreType：
             * 1 = 加分
             * -1 = 减分
             */
            if (Short.valueOf((short) -1).equals(apply.getScoreType())) {
                realScore = realScore.negate();
            }

            ScoreRecord record = new ScoreRecord();

            // 被加减分学生
            record.setStudentId(apply.getStudentId());

            // 最终实际分值
            record.setScore(realScore);

            // 来源类型
            record.setSourceType("DEPARTMENT");

            // 来源申请记录ID
            record.setSourceId(apply.getId());

            // ★★★ 非空字段 rule_id 必须赋值
            // 当前申报所使用的加减分规则/模板

            // 有效
            record.setStatus((short) 1);

            // 学生可见
            record.setAdminHidden((short) 0);

            record.setCreateTime(LocalDateTime.now());

            int result = scoreRecordMapper.insert(record);

            if (result <= 0) {
                return Result.fail("终审通过，但成绩记录生成失败");
            }
        }


        return Result.success(null);
    }


    /* =========================================================
       补充姓名、部门名称
       ========================================================= */

    private void fillNames(
            List<DepartmentScoreApply> list) {

        if (list == null
                || list.isEmpty()) {

            return;
        }


        for (DepartmentScoreApply item :
                list) {

            /* =====================
               学生姓名
               ===================== */

            if (item.getStudentId() != null) {

                SysUser student =
                        sysUserMapper.selectById(
                                item.getStudentId()
                        );


                if (student != null) {

                    item.setStudentName(
                            student.getRealName()
                    );
                }
            }


            /* =====================
               部门名称
               ===================== */

            if (item.getDepartmentId() != null) {

                Department department =
                        departmentMapper.selectById(
                                item.getDepartmentId()
                        );


                if (department != null) {

                    item.setDepartmentName(
                            department.getName()
                    );
                }
            }
        }
    }
}