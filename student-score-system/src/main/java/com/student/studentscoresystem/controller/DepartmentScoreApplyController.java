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
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.entity.SysSemester;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.ScoreRuleMapper;
import com.student.studentscoresystem.mapper.SysSemesterMapper;
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
import java.time.LocalDate;
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

    private final ScoreRuleMapper scoreRuleMapper;

    private final DepartmentMapper departmentMapper;

    private final SysUserMapper sysUserMapper;

    private final SysSemesterMapper sysSemesterMapper;


    public DepartmentScoreApplyController(
            IDepartmentScoreApplyService applyService,
            IDepartmentScoreTemplateService templateService,
            SysUserDepartmentMapper userDepartmentMapper,
            ScoreRecordMapper scoreRecordMapper,
            ScoreRuleMapper scoreRuleMapper,
            DepartmentMapper departmentMapper,
            SysUserMapper sysUserMapper,
            SysSemesterMapper sysSemesterMapper) {

        this.applyService = applyService;

        this.templateService = templateService;

        this.userDepartmentMapper =
                userDepartmentMapper;

        this.scoreRecordMapper =
                scoreRecordMapper;

        this.scoreRuleMapper =
                scoreRuleMapper;

        this.departmentMapper =
                departmentMapper;

        this.sysUserMapper =
                sysUserMapper;

        this.sysSemesterMapper =
                sysSemesterMapper;
    }


    /*
     * =========================================================
     * 获取当前登录用户 ID
     * =========================================================
     */
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


    /*
     * =========================================================
     * 部门申报权限
     *
     * 干事 / 副部长 / 部长
     *
     * 都可以提交部门加减分申报。
     * =========================================================
     */
    private boolean canApply(
            String position) {

        return "干事".equals(position)
                || "副部长".equals(position)
                || "部长".equals(position);
    }


    /*
     * =========================================================
     * 部门审核权限
     *
     * 副部长 / 部长
     *
     * 可以进行部门初审。
     * =========================================================
     */
    private boolean canAudit(
            String position) {

        return "副部长".equals(position)
                || "部长".equals(position);
    }


    /*
     * =========================================================
     * 查询当前正在进行的学期
     * =========================================================
     */
    private SysSemester getCurrentSemester() {

        LocalDate today =
                LocalDate.now();


        return sysSemesterMapper.selectOne(

                new LambdaQueryWrapper<SysSemester>()

                        .le(
                                SysSemester::getStartDate,
                                today
                        )

                        .ge(
                                SysSemester::getEndDate,
                                today
                        )

                        .eq(
                                SysSemester::getStatus,
                                (short) 1
                        )

                        .orderByDesc(
                                SysSemester::getStartDate
                        )

                        .last(
                                "LIMIT 1"
                        )
        );
    }


    /*
     * =========================================================
     * 提交部门加减分申报
     * =========================================================
     */
    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody DepartmentScoreApplyAddDTO dto,
            HttpServletRequest request) {


        Long applicantId;


        /*
         * 获取当前登录用户
         */
        try {

            applicantId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(
                    e.getMessage()
            );
        }


        /*
         * =====================================================
         * 参数检查
         * =====================================================
         */
        if (dto == null) {

            return Result.fail(
                    "申报参数不能为空"
            );
        }


        if (applicantId == null) {

            return Result.fail(
                    "请先登录"
            );
        }


        if (dto.getDepartmentId() == null) {

            return Result.fail(
                    "请选择申报部门"
            );
        }


        if (dto.getStudentId() == null) {

            return Result.fail(
                    "请选择被加减分学生"
            );
        }


        if (dto.getTemplateId() == null) {

            return Result.fail(
                    "请选择加减分项目"
            );
        }


        /*
         * =====================================================
         * 检查申报人是否属于该部门
         * =====================================================
         */
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


        /*
         * =====================================================
         * 检查部门申报权限
         * =====================================================
         */
        if (!canApply(
                relation.getPosition()
        )) {

            return Result.fail(
                    "你没有部门加减分申报权限"
            );
        }


        /*
         * =====================================================
         * 不能给自己申报
         * =====================================================
         */
        if (applicantId.equals(
                dto.getStudentId()
        )) {

            return Result.fail(
                    "不能给自己提交部门加减分申报"
            );
        }


        /*
         * =====================================================
         * 查询被申报学生
         * =====================================================
         */
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
                .equals(
                        student.getStatus()
                )) {

            return Result.fail(
                    "被申报学生当前状态异常"
            );
        }


        /*
         * =====================================================
         * 查询部门
         * =====================================================
         */
        Department department =
                departmentMapper.selectById(
                        dto.getDepartmentId()
                );


        if (department == null) {

            return Result.fail(
                    "申报部门不存在"
            );
        }


        if (!Short.valueOf((short) 1)
                .equals(
                        department.getStatus()
                )) {

            return Result.fail(
                    "该部门当前未启用"
            );
        }


        /*
         * =====================================================
         * 查询部门加减分模板
         * =====================================================
         */
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
                .equals(
                        template.getStatus()
                )) {

            return Result.fail(
                    "所选加减分项目已经停用"
            );
        }


        /*
         * =====================================================
         * 模板必须属于当前部门
         * =====================================================
         */
        if (!dto.getDepartmentId()
                .equals(
                        template.getDepartmentId()
                )) {

            return Result.fail(
                    "该加减分项目不属于当前申报部门"
            );
        }


        /*
         * =====================================================
         * 检查模板加减分类型
         *
         * 1  = 加分
         * -1 = 减分
         * =====================================================
         */
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


        /*
         * =====================================================
         * 检查模板分值
         * =====================================================
         */
        if (template.getScore() == null
                || template.getScore()
                .compareTo(BigDecimal.ZERO) <= 0) {

            return Result.fail(
                    "该模板分值配置错误"
            );
        }


        /*
         * =====================================================
         * 检查模板名称
         * =====================================================
         */
        if (template.getName() == null
                || template.getName()
                .trim()
                .isEmpty()) {

            return Result.fail(
                    "该模板名称为空"
            );
        }


        /*
         * =====================================================
         * 防止重复申报
         * =====================================================
         */
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


        /*
         * =====================================================
         * 创建部门申报记录
         * =====================================================
         */
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


        /*
         * department_score_apply.score
         *
         * 永远保存正数
         */
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
         * 部门审核：
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


        apply.setCreateTime(
                now
        );


        apply.setUpdateTime(
                now
        );


        /*
         * =====================================================
         * 保存
         * =====================================================
         */
        boolean saved =
                applyService.save(
                        apply
                );


        if (!saved) {

            return Result.fail(
                    "部门加减分申报保存失败"
            );
        }


        if (apply.getId() == null) {

            return Result.fail(
                    "申报保存失败：未生成申报记录ID"
            );
        }


        System.out.println(
                "========== 部门申报保存成功 =========="
        );


        System.out.println(
                "applyId = "
                        + apply.getId()
        );


        System.out.println(
                "studentId = "
                        + apply.getStudentId()
        );


        System.out.println(
                "applicantId = "
                        + apply.getApplicantId()
        );


        System.out.println(
                "departmentId = "
                        + apply.getDepartmentId()
        );


        System.out.println(
                "templateId = "
                        + apply.getTemplateId()
        );


        System.out.println(
                "scoreType = "
                        + apply.getScoreType()
        );


        System.out.println(
                "score = "
                        + apply.getScore()
        );


        System.out.println(
                "status = "
                        + apply.getStatus()
        );


        System.out.println(
                "finalStatus = "
                        + apply.getFinalStatus()
        );


        System.out.println(
                "======================================"
        );


        return Result.success(
                null
        );
    }


    /*
     * =========================================================
     * 兼容旧接口 /apply
     * =========================================================
     */
    @PostMapping("/apply")
    public Result<Void> apply(
            @RequestBody DepartmentScoreApplyAddDTO dto,
            HttpServletRequest request) {

        return add(
                dto,
                request
        );
    }


    /*
     * =========================================================
     * 当前用户部门权限
     * =========================================================
     */
    @GetMapping("/my-permissions")
    public Result<Map<String, Object>> myPermissions(
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


        /*
         * 查询当前用户所有在职部门身份
         */
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
                    .equals(
                            department.getStatus()
                    )) {

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


            departments.add(
                    item
            );


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


        return Result.success(
                data
        );
    }


    /*
     * =========================================================
     * 我的申报记录
     * =========================================================
     */
    @GetMapping("/my")
    public Result<List<DepartmentScoreApply>> my(
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


        fillNames(
                list
        );


        return Result.success(
                list
        );
    }


    /*
     * =========================================================
     * 兼容 /my-list
     * =========================================================
     */
    @GetMapping("/my-list")
    public Result<List<DepartmentScoreApply>> myList(
            HttpServletRequest request) {

        return my(
                request
        );
    }


    /*
     * =========================================================
     * 部门干部待审核
     *
     * 当前用户必须是：
     *
     * 副部长 / 部长
     *
     * 并且：
     *
     * 1. 负责该部门
     * 2. 申报状态 = 待审核
     * 3. 申报人不是自己
     * =========================================================
     */
    @GetMapping("/audit/list")
    public Result<List<DepartmentScoreApply>> auditList(
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


        /*
         * 查询当前用户担任：
         *
         * 副部长
         * 部长
         *
         * 的部门
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


        /*
         * 提取负责部门 ID
         */
        List<Long> departmentIds =

                myPositions.stream()

                        .map(
                                SysUserDepartment::getDepartmentId
                        )

                        .filter(
                                id -> id != null
                        )

                        .distinct()

                        .collect(
                                Collectors.toList()
                        );


        if (departmentIds.isEmpty()) {

            return Result.success(
                    new ArrayList<>()
            );
        }


        /*
         * 查询待审核申报
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

                                .ne(
                                        DepartmentScoreApply::getApplicantId,
                                        currentUserId
                                )

                                .orderByDesc(
                                        DepartmentScoreApply::getCreateTime
                                )
                );


        fillNames(
                list
        );


        return Result.success(
                list
        );
    }


    /*
     * =========================================================
     * 部门单条审核
     * =========================================================
     */
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

            return Result.fail(
                    e.getMessage()
            );
        }


        /*
         * 参数检查
         */
        if (dto == null
                || dto.getStatus() == null) {

            return Result.fail(
                    "审核状态不能为空"
            );
        }


        Short status =
                dto.getStatus();


        /*
         * 1 = 通过
         * 2 = 驳回
         */
        if (status != 1
                && status != 2) {

            return Result.fail(
                    "审核状态无效"
            );
        }


        /*
         * 查询申请
         */
        DepartmentScoreApply apply =
                applyService.getById(
                        id
                );


        if (apply == null) {

            return Result.fail(
                    "申报记录不存在"
            );
        }


        /*
         * 必须处于部门待审核状态
         */
        if (!Short.valueOf((short) 0)
                .equals(
                        apply.getStatus()
                )) {

            return Result.fail(
                    "该申报已经完成部门审核"
            );
        }


        /*
         * =====================================================
         * 审核人必须属于该申报部门
         *
         * 并且必须是：
         *
         * 副部长 / 部长
         * =====================================================
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
                    "你不是该部门的副部长或部长，没有审核权限"
            );
        }


        /*
         * 不能审核自己的申报
         */
        if (currentUserId.equals(
                apply.getApplicantId()
        )) {

            return Result.fail(
                    "不能审核自己提交的申报"
            );
        }


        /*
         * 更新部门审核
         */
        apply.setStatus(
                status
        );


        apply.setReviewerId(
                currentUserId
        );


        apply.setReviewRemark(
                dto.getReviewRemark()
        );


        LocalDateTime now =
                LocalDateTime.now();


        apply.setReviewTime(
                now
        );


        apply.setUpdateTime(
                now
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


        return Result.success(
                null
        );
    }


    /*
     * =========================================================
     * 部门一键审批全部
     *
     * 当前用户：
     *
     * 副部长 / 部长
     *
     * 自动审批自己负责部门下：
     *
     * status = 0
     *
     * 且不是自己提交的申报。
     *
     * =========================================================
     */
    @PutMapping("/audit/batch-pass")
    @Transactional
    public Result<Map<String, Object>> batchPass(
            HttpServletRequest request) {


        Long currentUserId;


        /*
         * 获取当前用户
         */
        try {

            currentUserId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(
                    e.getMessage()
            );
        }


        /*
         * 查询当前用户担任：
         *
         * 副部长 / 部长
         *
         * 的部门
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


        /*
         * 没有审核权限
         */
        if (myPositions == null
                || myPositions.isEmpty()) {

            return Result.fail(
                    "你没有部门审核权限"
            );
        }


        /*
         * 获取负责部门
         */
        List<Long> departmentIds =

                myPositions.stream()

                        .map(
                                SysUserDepartment::getDepartmentId
                        )

                        .filter(
                                id -> id != null
                        )

                        .distinct()

                        .collect(
                                Collectors.toList()
                        );


        if (departmentIds.isEmpty()) {

            return Result.fail(
                    "你当前没有负责的部门"
            );
        }


        /*
         * =====================================================
         * 查询待审批记录
         * =====================================================
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

                                .ne(
                                        DepartmentScoreApply::getApplicantId,
                                        currentUserId
                                )

                                .orderByAsc(
                                        DepartmentScoreApply::getCreateTime
                                )
                );


        /*
         * 没有待审批记录
         */
        if (list == null
                || list.isEmpty()) {

            Map<String, Object> data =
                    new HashMap<>();


            data.put(
                    "count",
                    0
            );


            data.put(
                    "message",
                    "当前没有可以审批的部门申报"
            );


            return Result.success(
                    data
            );
        }


        /*
         * =====================================================
         * 开始批量审批
         *
         * 注意：
         *
         * 这里只改变部门初审状态。
         *
         * 不在这里生成 score_record。
         *
         * score_record 必须等辅导员终审通过后生成。
         * =====================================================
         */
        LocalDateTime now =
                LocalDateTime.now();


        int successCount = 0;


        for (DepartmentScoreApply apply :
                list) {


            /*
             * 再次确认状态
             */
            if (!Short.valueOf((short) 0)
                    .equals(
                            apply.getStatus()
                    )) {

                continue;
            }


            /*
             * 再次确认不能审批自己的
             */
            if (currentUserId.equals(
                    apply.getApplicantId()
            )) {

                continue;
            }


            /*
             * =================================================
             * 最后一层确认：
             *
             * 当前审核人确实属于该申报部门，
             * 并且是部长 / 副部长。
             *
             * 防止一个干部负责多个部门时
             * 错误审批其他部门。
             * =================================================
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

                continue;
            }


            /*
             * 设置审核通过
             */
            apply.setStatus(
                    (short) 1
            );


            /*
             * 审核人
             */
            apply.setReviewerId(
                    currentUserId
            );


            /*
             * 审核意见
             */
            apply.setReviewRemark(
                    "一键审批通过"
            );


            /*
             * 审核时间
             */
            apply.setReviewTime(
                    now
            );


            /*
             * 更新时间
             */
            apply.setUpdateTime(
                    now
            );


            /*
             * 保存
             */
            boolean updated =
                    applyService.updateById(
                            apply
                    );


            if (updated) {

                successCount++;
            }
        }


        /*
         * =====================================================
         * 返回结果
         * =====================================================
         */
        Map<String, Object> data =
                new HashMap<>();


        data.put(
                "count",
                successCount
        );


        data.put(
                "message",
                "一键审批完成"
        );


        return Result.success(
                data
        );
    }


    /*
     * =========================================================
     * 辅导员待终审
     *
     * 当前辅导员负责哪些部门，
     * 完全由 department.teacher_id 决定。
     *
     * 一个老师可以负责多个部门。
     * =========================================================
     */
    @GetMapping("/final-audit/list")
    public Result<List<DepartmentScoreApply>>
    finalAuditList(
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


        /*
         * 查询当前辅导员负责的所有部门
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


        /*
         * 提取部门 ID
         */
        List<Long> departmentIds =

                departments.stream()

                        .map(
                                Department::getId
                        )

                        .filter(
                                id -> id != null
                        )

                        .distinct()

                        .collect(
                                Collectors.toList()
                        );


        /*
         * 查询：
         *
         * 部门审核通过
         * +
         * 等待辅导员终审
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


        fillNames(
                list
        );


        return Result.success(
                list
        );
    }


    /*
     * =========================================================
     * 辅导员已处理
     * =========================================================
     */
    @GetMapping("/final-audit/processed")
    public Result<List<DepartmentScoreApply>>
    finalAuditProcessed(
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


        /*
         * 查询当前辅导员负责部门
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

                        .map(
                                Department::getId
                        )

                        .filter(
                                id -> id != null
                        )

                        .distinct()

                        .collect(
                                Collectors.toList()
                        );


        /*
         * finalStatus：
         *
         * 1 = 通过
         * 2 = 驳回
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


        fillNames(
                list
        );


        return Result.success(
                list
        );
    }


    /*
     * =========================================================
     * 辅导员历史
     *
     * 兼容旧接口。
     * =========================================================
     */
    @GetMapping("/final-audit/history")
    public Result<List<DepartmentScoreApply>>
    finalAuditHistory(
            HttpServletRequest request) {

        return finalAuditProcessed(
                request
        );
    }


    /*
     * =========================================================
     * 辅导员终审
     *
     * finalStatus：
     *
     * 1 = 通过
     * 2 = 驳回
     *
     * 终审通过：
     *
     * 自动生成 score_record。
     * =========================================================
     */
    @Transactional
    @PutMapping("/final-audit/{id}")
    public Result<Void> finalAudit(
            @PathVariable Long id,
            @RequestBody DepartmentScoreFinalAuditDTO dto,
            HttpServletRequest request) {


        Long currentUserId;


        /*
         * 获取当前辅导员
         */
        try {

            currentUserId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(
                    e.getMessage()
            );
        }


        /*
         * =====================================================
         * 参数检查
         * =====================================================
         */
        if (dto == null
                || dto.getStatus() == null) {

            return Result.fail(
                    "终审状态不能为空"
            );
        }


        Short finalStatus =
                dto.getStatus();


        /*
         * 1 = 通过
         * 2 = 驳回
         */
        if (finalStatus != 1
                && finalStatus != 2) {

            return Result.fail(
                    "终审状态无效"
            );
        }


        /*
         * =====================================================
         * 查询申报
         * =====================================================
         */
        DepartmentScoreApply apply =
                applyService.getById(
                        id
                );


        if (apply == null) {

            return Result.fail(
                    "申报记录不存在"
            );
        }


        /*
         * =====================================================
         * 必须已经部门审核通过
         * =====================================================
         */
        if (!Short.valueOf((short) 1)
                .equals(
                        apply.getStatus()
                )) {

            return Result.fail(
                    "该申报尚未通过部门审核"
            );
        }


        /*
         * =====================================================
         * 必须等待终审
         * =====================================================
         */
        if (!Short.valueOf((short) 0)
                .equals(
                        apply.getFinalStatus()
                )) {

            return Result.fail(
                    "该申报已经完成终审"
            );
        }


        /*
         * =====================================================
         * 检查辅导员是否负责该部门
         *
         * department.teacher_id
         * 必须等于当前辅导员 ID。
         *
         * 一个辅导员可以对应多个部门。
         * =====================================================
         */
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


        /*
         * =====================================================
         * 更新终审结果
         * =====================================================
         */
        LocalDateTime now =
                LocalDateTime.now();


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
                now
        );


        apply.setUpdateTime(
                now
        );


        boolean updated =
                applyService.updateById(
                        apply
                );


        if (!updated) {

            throw new IllegalArgumentException(
                    "终审状态更新失败"
            );
        }


        /*
         * =====================================================
         * 终审驳回
         * =====================================================
         */
        if (finalStatus == 2) {

            System.out.println(
                    "========== 部门加减分终审驳回 =========="
            );


            System.out.println(
                    "applyId = "
                            + apply.getId()
            );


            System.out.println(
                    "studentId = "
                            + apply.getStudentId()
            );


            System.out.println(
                    "departmentId = "
                            + apply.getDepartmentId()
            );


            System.out.println(
                    "finalReviewerId = "
                            + currentUserId
            );


            System.out.println(
                    "======================================"
            );


            return Result.success(
                    null
            );
        }


        /*
         * =====================================================
         * 终审通过
         *
         * 下面开始生成正式成绩。
         * =====================================================
         */
        if (finalStatus == 1) {


            /*
             * =================================================
             * 防止重复生成正式成绩
             *
             * source_type = DEPARTMENT
             * source_id = department_score_apply.id
             * =================================================
             */
            Long recordCount =

                    scoreRecordMapper.selectCount(

                            new LambdaQueryWrapper<ScoreRecord>()

                                    .eq(
                                            ScoreRecord::getSourceType,
                                            "DEPARTMENT"
                                    )

                                    .eq(
                                            ScoreRecord::getSourceId,
                                            apply.getId()
                                    )
                    );


            if (recordCount != null
                    && recordCount > 0) {

                throw new IllegalArgumentException(
                        "该申报已经生成正式成绩记录，请勿重复生成"
                );
            }


            /*
             * =================================================
             * 检查申报分值
             * =================================================
             */
            BigDecimal realScore =
                    apply.getScore();


            if (realScore == null) {

                throw new IllegalArgumentException(
                        "该申报分值为空，无法生成成绩记录"
                );
            }


            /*
             * =================================================
             * 根据 scoreType 决定最终正负
             *
             * 1  = 加分
             * -1 = 减分
             *
             * department_score_apply.score
             * 保存正数。
             *
             * score_record.score
             * 加分 = 正数
             * 减分 = 负数
             * =================================================
             */
            if (Short.valueOf((short) -1)
                    .equals(
                            apply.getScoreType()
                    )) {

                realScore =
                        realScore.negate();
            }


            /*
             * =================================================
             * 当前学期
             * =================================================
             */
            SysSemester currentSemester =
                    getCurrentSemester();


            if (currentSemester == null) {

                throw new IllegalArgumentException(
                        "当前没有正在进行的学期，无法生成正式成绩记录"
                );
            }


            /*
             * =================================================
             * ★★★ 关键修复
             *
             * 以前错误代码：
             *
             * record.setRuleId(
             *     apply.getTemplateId()
             * );
             *
             * 这是错误的。
             *
             * 因为：
             *
             * department_score_template.id
             *
             * 不等于：
             *
             * score_rule.id
             *
             * =================================================
             */


            /*
             * =================================================
             * 查找正式成绩规则
             *
             * 使用：
             *
             * 1. department_id
             * 2. name
             * 3. status
             *
             * 来找到真正的 score_rule。
             *
             * 例如：
             *
             * department_id = 3
             * name = 档案整理
             *
             * 得到：
             *
             * score_rule.id = 2
             * =================================================
             */
            ScoreRule scoreRule =

                    scoreRuleMapper.selectOne(

                            new LambdaQueryWrapper<ScoreRule>()

                                    .eq(
                                            ScoreRule::getDepartmentId,
                                            apply.getDepartmentId()
                                    )

                                    .eq(
                                            ScoreRule::getName,
                                            apply.getTitle()
                                    )

                                    .eq(
                                            ScoreRule::getStatus,
                                            (short) 1
                                    )

                                    .last(
                                            "LIMIT 1"
                                    )
                    );


            /*
             * =================================================
             * 找不到正式规则
             *
             * 直接终止事务。
             *
             * 这样不会生成一条 rule_id 错误的成绩记录。
             * =================================================
             */
            if (scoreRule == null) {

                throw new IllegalArgumentException(

                        "未找到对应的正式加减分规则：" +

                                "部门ID=" +

                                apply.getDepartmentId() +

                                "，规则名称=" +

                                apply.getTitle()
                );
            }


            /*
             * =================================================
             * 检查正式规则分值
             *
             * 这里做一道保险。
             *
             * 如果 score_rule 中的分值
             * 和部门申报模板不一致，
             * 不允许继续生成正式成绩。
             * =================================================
             */
            if (scoreRule.getScore() == null
                    || scoreRule.getScore()
                    .compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        "正式加减分规则分值配置错误：" +
                                scoreRule.getName()
                );
            }


            /*
             * =================================================
             * 检查正式规则分值
             *
             * 正常情况下：
             *
             * department_score_template.score
             *
             * =
             *
             * score_rule.score
             *
             * =================================================
             */
            if (apply.getScore() == null
                    || scoreRule.getScore()
                    .compareTo(
                            apply.getScore()
                    ) != 0) {

                throw new IllegalArgumentException(

                        "部门申报分值与正式加减分规则分值不一致：" +

                                "申报分值=" +

                                apply.getScore() +

                                "，正式规则分值=" +

                                scoreRule.getScore()
                );
            }


            /*
             * =================================================
             * 创建正式成绩记录
             * =================================================
             */
            ScoreRecord record =
                    new ScoreRecord();


            /*
             * 学生
             */
            record.setStudentId(
                    apply.getStudentId()
            );


            /*
             * =================================================
             * ★★★ 正确的 rule_id
             *
             * 使用：
             *
             * score_rule.id
             *
             * 而不是：
             *
             * department_score_template.id
             * =================================================
             */
            record.setRuleId(
                    scoreRule.getId()
            );


            /*
             * 最终实际分值
             */
            record.setScore(
                    realScore
            );


            /*
             * 当前学期
             */
            record.setSemesterId(
                    currentSemester.getId()
            );


            /*
             * 来源类型
             */
            record.setSourceType(
                    "DEPARTMENT"
            );


            /*
             * 来源业务 ID
             *
             * 对应：
             *
             * department_score_apply.id
             */
            record.setSourceId(
                    apply.getId()
            );


            /*
             * 有效
             */
            record.setStatus(
                    (short) 1
            );


            /*
             * 管理员不隐藏
             */
            record.setAdminHidden(
                    (short) 0
            );


            /*
             * 创建时间
             */
            record.setCreateTime(
                    now
            );


            /*
             * =================================================
             * 保存正式成绩
             * =================================================
             */
            int result =
                    scoreRecordMapper.insert(
                            record
                    );


            if (result <= 0) {

                throw new IllegalArgumentException(
                        "终审通过，但成绩记录生成失败"
                );
            }


            /*
             * =================================================
             * 输出日志
             * =================================================
             */
            System.out.println(
                    "========== 部门加减分正式成绩生成成功 =========="
            );


            System.out.println(
                    "applyId = "
                            + apply.getId()
            );


            System.out.println(
                    "studentId = "
                            + record.getStudentId()
            );


            System.out.println(
                    "departmentId = "
                            + apply.getDepartmentId()
            );


            System.out.println(
                    "departmentName = "
                            + department.getName()
            );


            System.out.println(
                    "templateId = "
                            + apply.getTemplateId()
            );


            System.out.println(
                    "ruleId = "
                            + record.getRuleId()
            );


            System.out.println(
                    "ruleName = "
                            + scoreRule.getName()
            );


            System.out.println(
                    "ruleScore = "
                            + scoreRule.getScore()
            );


            System.out.println(
                    "score = "
                            + record.getScore()
            );


            System.out.println(
                    "semesterId = "
                            + record.getSemesterId()
            );


            System.out.println(
                    "semesterName = "
                            + currentSemester.getName()
            );


            System.out.println(
                    "semesterStartDate = "
                            + currentSemester.getStartDate()
            );


            System.out.println(
                    "semesterEndDate = "
                            + currentSemester.getEndDate()
            );


            System.out.println(
                    "sourceType = "
                            + record.getSourceType()
            );


            System.out.println(
                    "sourceId = "
                            + record.getSourceId()
            );


            System.out.println(
                    "=============================================="
            );
        }


        return Result.success(
                null
        );
    }


    /*
     * =========================================================
     * 补充姓名、部门名称
     *
     * studentName
     * applicantName
     * departmentName
     *
     * 都是 exist = false
     *
     * 所以手动查询。
     * =========================================================
     */
    private void fillNames(
            List<DepartmentScoreApply> list) {


        if (list == null
                || list.isEmpty()) {

            return;
        }


        for (DepartmentScoreApply item :
                list) {


            /*
             * =================================================
             * 学生姓名
             * =================================================
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
             * =================================================
             * 申报人姓名
             * =================================================
             */
            if (item.getApplicantId() != null) {

                SysUser applicant =
                        sysUserMapper.selectById(
                                item.getApplicantId()
                        );


                if (applicant != null) {

                    item.setApplicantName(
                            applicant.getRealName()
                    );
                }
            }


            /*
             * =================================================
             * 部门名称
             * =================================================
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