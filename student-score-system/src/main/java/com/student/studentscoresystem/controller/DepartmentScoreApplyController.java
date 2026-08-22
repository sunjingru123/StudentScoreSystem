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
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {

        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("请先登录");
        }

        Claims claims = JwtUtil.parseToken(token.substring(7));

        return claims.get("userId", Long.class);
    }

    /**
     * 判断岗位是否具有部门申报权限
     */
    private boolean canApply(String position) {
        return "干事".equals(position)
                || "副部长".equals(position)
                || "部长".equals(position);
    }

    /**
     * 判断岗位是否具有部门审核权限
     */
    private boolean canAudit(String position) {
        return "副部长".equals(position)
                || "部长".equals(position);
    }

    /**
     * ============================================================
     * 提交部门加减分申请
     * ============================================================
     *
     * 新逻辑：
     *
     * applicantId = 当前登录的干事/副部长/部长
     * studentId   = 被加分/扣分的学生
     * departmentId = 当前干部所属部门
     * templateId  = 当前部门自己的模板
     *
     * scoreType、score、title、description
     * 全部从模板读取。
     */
    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody DepartmentScoreApplyAddDTO dto,
            HttpServletRequest request) {

        Long currentUserId;

        try {
            currentUserId = getCurrentUserId(request);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }

        /*
         * ========================================================
         * 1. 基础参数
         * ========================================================
         */

        if (dto.getDepartmentId() == null) {
            return Result.fail("请选择申报部门");
        }

        /*
         * 被加减分学生
         */
        if (dto.getStudentId() == null) {
            return Result.fail("请选择需要加减分的学生");
        }

        /*
         * 模板
         */
        if (dto.getTemplateId() == null) {
            return Result.fail("请选择加减分项目");
        }

        /*
         * ========================================================
         * 2. 校验当前登录用户是否属于该部门
         * ========================================================
         */

        SysUserDepartment applicantRelation =
                userDepartmentMapper.selectOne(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getUserId,
                                        currentUserId
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

        if (applicantRelation == null) {
            return Result.fail(
                    "你不是该部门在职成员，不能提交该部门加减分申报"
            );
        }

        /*
         * ========================================================
         * 3. 校验岗位
         * ========================================================
         */

        if (!canApply(applicantRelation.getPosition())) {
            return Result.fail(
                    "只有干事、副部长、部长可以提交部门加减分申报"
            );
        }

        /*
         * ========================================================
         * 4. 校验被加减分对象
         * ========================================================
         *
         * 只有“学生”岗位的人才允许成为部门加减分对象。
         *
         * 注意：
         * 干事、副部长、部长如果同时拥有“学生”岗位，
         * 仍然可以被加减分。
         *
         * 老师、管理员没有“学生”岗位，
         * 即使 sys_user.status = 1，也不能被加减分。
         */

        SysUser student =
                sysUserMapper.selectById(dto.getStudentId());

        if (student == null) {
            return Result.fail("被加减分学生不存在");
        }

        if (!Short.valueOf((short) 1).equals(student.getStatus())) {
            return Result.fail("被加减分学生当前不是正常用户");
        }

        /*
         * 查询该用户是否拥有“学生”岗位
         */
        Long studentPositionId =
                sysPositionMapper.selectOne(
                        new LambdaQueryWrapper<SysPosition>()
                                .eq(
                                        SysPosition::getName,
                                        "学生"
                                )
                ) != null
                        ? sysPositionMapper.selectOne(
                        new LambdaQueryWrapper<SysPosition>()
                                .eq(
                                        SysPosition::getName,
                                        "学生"
                                )
                ).getId()
                        : null;

        if (studentPositionId == null) {
            return Result.fail("系统中不存在“学生”岗位");
        }

        /*
         * 查询被加减分用户的学生岗位关系
         */
        Long studentPositionCount =
                sysUserPositionMapper.selectCount(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .eq(
                                        SysUserPosition::getUserId,
                                        dto.getStudentId()
                                )
                                .eq(
                                        SysUserPosition::getPositionId,
                                        studentPositionId
                                )
                );

        if (studentPositionCount == 0) {
            return Result.fail(
                    "该用户不是学生，不能进行部门加减分申报"
            );
        }

        /*
         * ========================================================
         * 5. 查询模板
         * ========================================================
         */

        DepartmentScoreTemplate template =
                templateService.getById(dto.getTemplateId());

        if (template == null) {
            return Result.fail("所选加减分项目不存在");
        }

        /*
         * 模板必须启用
         */
        if (!Short.valueOf((short) 1).equals(template.getStatus())) {
            return Result.fail("所选加减分项目已经停用");
        }

        /*
         * ========================================================
         * 6. 最重要的部门隔离
         * ========================================================
         *
         * 模板所属部门必须和当前申报部门完全一致。
         *
         * 学习部 -> 只能使用学习部模板
         * 生活部 -> 只能使用生活部模板
         * 体育部 -> 只能使用体育部模板
         */

        if (!dto.getDepartmentId().equals(template.getDepartmentId())) {
            return Result.fail(
                    "该加减分项目不属于当前申报部门，不能使用"
            );
        }

        /*
         * ========================================================
         * 7. 模板数据合法性校验
         * ========================================================
         */

        if (template.getScoreType() == null
                || (template.getScoreType() != 1
                && template.getScoreType() != -1)) {

            return Result.fail("该模板的加减分类型配置错误，请联系管理员");
        }

        if (template.getScore() == null
                || template.getScore().compareTo(BigDecimal.ZERO) <= 0) {

            return Result.fail("该模板的分值配置错误，请联系管理员");
        }

        if (template.getName() == null
                || template.getName().trim().isEmpty()) {

            return Result.fail("该模板项目名称为空，请联系管理员");
        }

        /*
         * ========================================================
         * 8. 防止重复申报
         * ========================================================
         *
         * 同一个申请人
         * + 同一个被加减分学生
         * + 同一个部门
         * + 同一个模板
         *
         * 待审核 / 初审通过 / 最终通过
         * 不允许重复提交。
         */

        LambdaQueryWrapper<DepartmentScoreApply> duplicateWrapper =
                new LambdaQueryWrapper<>();

        duplicateWrapper
                .eq(
                        DepartmentScoreApply::getApplicantId,
                        currentUserId
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
                                .and(w ->
                                        w.eq(
                                                DepartmentScoreApply::getStatus,
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

        Long duplicateCount = applyService.count(duplicateWrapper);

        if (duplicateCount > 0) {
            return Result.fail(
                    "该学生已经提交过相同的部门加减分项目，请勿重复申报"
            );
        }

        /*
         * ========================================================
         * 9. 创建申请
         * ========================================================
         */

        DepartmentScoreApply apply = new DepartmentScoreApply();

        /*
         * 被加减分学生
         */
        apply.setStudentId(dto.getStudentId());

        /*
         * 当前登录用户 = 申报人
         */
        apply.setApplicantId(currentUserId);

        /*
         * 所属部门
         */
        apply.setDepartmentId(dto.getDepartmentId());

        /*
         * 模板
         */
        apply.setTemplateId(dto.getTemplateId());

        /*
         * 以下全部从模板读取
         *
         * 前端传什么都不信。
         */

        apply.setScoreType(template.getScoreType());

        apply.setScore(template.getScore());

        apply.setTitle(template.getName().trim());

        apply.setDescription(template.getDescription());

        /*
         * 凭证地址可以由申报人上传
         */
        apply.setEvidenceUrl(dto.getEvidenceUrl());

        /*
         * 初审状态
         */
        apply.setStatus((short) 0);

        /*
         * 最终审核状态
         */
        apply.setFinalStatus((short) 0);

        apply.setCreateTime(LocalDateTime.now());

        apply.setUpdateTime(LocalDateTime.now());

        applyService.save(apply);

        return Result.success(null);
    }

    /**
     * ============================================================
     * /apply 兼容接口
     * ============================================================
     */
    @PostMapping("/apply")
    public Result<Void> apply(
            @RequestBody DepartmentScoreApplyAddDTO dto,
            HttpServletRequest request) {

        return add(dto, request);
    }

    /**
     * ============================================================
     * 查询当前用户可以申报的部门
     * ============================================================
     */
    @GetMapping("/my-permissions")
    public Result<Map<String, Object>> myPermissions(
            HttpServletRequest request) {

        Long currentUserId;

        try {
            currentUserId = getCurrentUserId(request);
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

        List<Map<String, Object>> departments =
                new ArrayList<>();

        boolean canDepartmentAudit = false;

        for (SysUserDepartment relation : relations) {

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

            Map<String, Object> item = new HashMap<>();

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

            if (canAudit(relation.getPosition())) {
                canDepartmentAudit = true;
            }
        }

        Map<String, Object> data = new HashMap<>();

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

    /**
     * ============================================================
     * 查询当前用户提交的部门申报
     * ============================================================
     */
    @GetMapping("/my")
    public Result<List<DepartmentScoreApply>> my(
            HttpServletRequest request) {

        Long currentUserId;

        try {
            currentUserId = getCurrentUserId(request);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }

        /*
         * 注意：
         *
         * 现在 applicantId 才代表“我提交的申请”。
         *
         * 不能再使用 studentId=currentUserId，
         * 因为 studentId 现在是“被加减分的人”。
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

    /**
     * 兼容前端 /my-list
     */
    @GetMapping("/my-list")
    public Result<List<DepartmentScoreApply>> myList(
            HttpServletRequest request) {

        return my(request);
    }

    /**
     * ============================================================
     * 部门干部待审核列表
     * ============================================================
     *
     * 副部长 / 部长可见。
     *
     * 只看自己所属部门。
     *
     * 自动过滤自己提交的申请。
     */
    @GetMapping("/audit/list")
    public Result<List<DepartmentScoreApply>> auditList(
            HttpServletRequest request) {

        Long currentUserId;

        try {
            currentUserId = getCurrentUserId(request);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }

        List<SysUserDepartment> myPositions =
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
            return Result.success(List.of());
        }

        List<Long> departmentIds =
                myPositions.stream()
                        .map(SysUserDepartment::getDepartmentId)
                        .distinct()
                        .collect(Collectors.toList());

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
         * 核心：
         *
         * 不能审核自己提交的单据。
         */
        list.removeIf(item ->
                currentUserId.equals(item.getApplicantId())
        );

        fillNames(list);

        return Result.success(list);
    }

    /**
     * ============================================================
     * 部门干部初审
     * ============================================================
     */
    @PutMapping("/audit/{id}")
    public Result<Void> audit(
            @PathVariable Long id,
            @RequestBody DepartmentScoreApplyAuditDTO dto,
            HttpServletRequest request) {

        Long currentUserId;

        try {
            currentUserId = getCurrentUserId(request);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }

        if (dto.getStatus() == null
                || (dto.getStatus() != 1
                && dto.getStatus() != 2)) {

            return Result.fail("审核状态错误");
        }

        DepartmentScoreApply apply =
                applyService.getById(id);

        if (apply == null) {
            return Result.fail("申报记录不存在");
        }

        /*
         * 只能审核待初审单据
         */
        if (!Short.valueOf((short) 0)
                .equals(apply.getStatus())) {

            return Result.fail(
                    "该申请已经完成部门初审，不可重复操作"
            );
        }

        /*
         * ========================================================
         * 审核人必须是该部门副部长 / 部长
         * ========================================================
         */

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
                    "你不是该部门副部长或部长，无初审权限"
            );
        }

        /*
         * ========================================================
         * 禁止审核自己提交的申请
         * ========================================================
         */

        if (currentUserId.equals(apply.getApplicantId())) {
            return Result.fail(
                    "不能审核自己提交的部门申报"
            );
        }

        /*
         * 更新审核信息
         */
        apply.setStatus(dto.getStatus());

        apply.setReviewerId(currentUserId);

        apply.setReviewRemark(dto.getReviewRemark());

        apply.setReviewTime(LocalDateTime.now());

        apply.setUpdateTime(LocalDateTime.now());

        applyService.updateById(apply);

        return Result.success(null);
    }

    /**
     * ============================================================
     * 辅导员终审列表
     * ============================================================
     */
    @GetMapping("/final-audit/list")
    public Result<List<DepartmentScoreApply>> finalAuditList(
            HttpServletRequest request) {

        Long currentUserId;

        try {
            currentUserId = getCurrentUserId(request);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }

        /*
         * 找当前辅导员负责的部门
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
            return Result.success(List.of());
        }

        List<Long> departmentIds =
                departments.stream()
                        .map(Department::getId)
                        .collect(Collectors.toList());

        /*
         * status=1
         * 表示干部初审通过
         *
         * finalStatus=0
         * 表示等待辅导员终审
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

    /**
     * ============================================================
     * 辅导员最终审核
     * ============================================================
     */
    @PutMapping("/final-audit/{id}")
    public Result<Void> finalAudit(
            @PathVariable Long id,
            @RequestBody DepartmentScoreFinalAuditDTO dto,
            HttpServletRequest request) {

        Long currentUserId;

        try {
            currentUserId = getCurrentUserId(request);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }

        if (dto.getStatus() == null
                || (dto.getStatus() != 1
                && dto.getStatus() != 2)) {

            return Result.fail("最终审核状态错误");
        }

        DepartmentScoreApply apply =
                applyService.getById(id);

        if (apply == null) {
            return Result.fail("申报记录不存在");
        }

        /*
         * 必须先经过部门干部初审
         */
        if (!Short.valueOf((short) 1)
                .equals(apply.getStatus())) {

            return Result.fail(
                    "该申报尚未通过部门干部初审，无法终审"
            );
        }

        /*
         * 禁止重复终审
         */
        if (!Short.valueOf((short) 0)
                .equals(apply.getFinalStatus())) {

            return Result.fail(
                    "该申报已经完成辅导员终审"
            );
        }

        /*
         * ========================================================
         * 校验辅导员
         * ========================================================
         */

        Department department =
                departmentMapper.selectById(
                        apply.getDepartmentId()
                );

        if (department == null) {
            return Result.fail("所属部门不存在");
        }

        if (department.getTeacherId() == null
                || !department.getTeacherId()
                .equals(currentUserId)) {

            return Result.fail(
                    "你不是该部门辅导员，无最终审核权限"
            );
        }

        /*
         * 更新终审
         */
        apply.setFinalStatus(dto.getStatus());

        apply.setFinalReviewerId(currentUserId);

        apply.setFinalReviewRemark(
                dto.getReviewRemark()
        );

        apply.setFinalReviewTime(
                LocalDateTime.now()
        );

        apply.setUpdateTime(
                LocalDateTime.now()
        );

        applyService.updateById(apply);

        /*
         * ========================================================
         * 驳回
         * ========================================================
         */

        if (dto.getStatus() == 2) {
            return Result.success(null);
        }

        /*
         * ========================================================
         * 终审通过
         * ========================================================
         *
         * 生成 ScoreRecord
         */

        Long existRecord =
                scoreRecordMapper.selectCount(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .eq(
                                        ScoreRecord::getStudentId,
                                        apply.getStudentId()
                                )
                                .eq(
                                        ScoreRecord::getSourceType,
                                        "DEPARTMENT"
                                )
                                .eq(
                                        ScoreRecord::getSourceId,
                                        apply.getId()
                                )
                );

        if (existRecord > 0) {
            return Result.success(null);
        }

        /*
         * ========================================================
         * 正负分处理
         * ========================================================
         */

        BigDecimal realScore =
                apply.getScore();

        if (Short.valueOf((short) -1)
                .equals(apply.getScoreType())) {

            realScore = realScore.negate();
        }

        /*
         * ========================================================
         * 生成成绩记录
         * ========================================================
         */

        ScoreRecord record =
                new ScoreRecord();

        /*
         * 注意：
         *
         * 这里一定使用 studentId。
         *
         * 因为 studentId 是真正被加分/扣分的人。
         */
        record.setStudentId(
                apply.getStudentId()
        );

        record.setRuleId(null);

        record.setScore(
                realScore
        );

        record.setSourceType(
                "DEPARTMENT"
        );

        record.setSourceId(
                apply.getId()
        );

        record.setAdminHidden(
                (short) 0
        );

        record.setCreateTime(
                LocalDateTime.now()
        );

        scoreRecordMapper.insert(record);

        return Result.success(null);
    }

    /**
     * ============================================================
     * 给前端补充学生姓名、部门名称
     * ============================================================
     */
    private void fillNames(
            List<DepartmentScoreApply> list) {

        for (DepartmentScoreApply item : list) {

            /*
             * 被加减分学生
             */
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

            /*
             * 部门
             */
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