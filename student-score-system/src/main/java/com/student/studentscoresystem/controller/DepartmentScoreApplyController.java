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


        try {

            applicantId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(
                    e.getMessage()
            );
        }


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


        if (!canApply(
                relation.getPosition()
        )) {

            return Result.fail(
                    "你没有部门加减分申报权限"
            );
        }


        /*
         * 不能给自己申报
         */
        if (applicantId.equals(
                dto.getStudentId()
        )) {

            return Result.fail(
                    "不能给自己提交部门加减分申报"
            );
        }


        /*
         * 查询学生
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
         * 查询部门
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
         * 查询模板
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


        if (!dto.getDepartmentId()
                .equals(
                        template.getDepartmentId()
                )) {

            return Result.fail(
                    "该加减分项目不属于当前申报部门"
            );
        }


        /*
         * 1 = 加分
         * -1 = 减分
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


        if (template.getScore() == null
                || template.getScore()
                .compareTo(BigDecimal.ZERO) <= 0) {

            return Result.fail(
                    "该模板分值配置错误"
            );
        }


        if (template.getName() == null
                || template.getName()
                .trim()
                .isEmpty()) {

            return Result.fail(
                    "该模板名称为空"
            );
        }


        /*
         * 防止重复申报
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
         * 创建申报
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
         * 申报表中的 score 永远保存正数
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


    /**
     * ============================================================
     * 获取当前用户部门权限
     * ============================================================
     *
     * 权限规则：
     *
     * 干事：
     *      可以部门加减分申报
     *      不可以部门申报审核
     *
     * 副部长：
     *      可以部门加减分申报
     *      可以部门申报审核
     *
     * 部长：
     *      可以部门加减分申报
     *      可以部门申报审核
     *
     * 普通学生：
     *      不可以部门加减分申报
     *      不可以部门申报审核
     */
    @GetMapping("/my-permissions")
    public Result<Map<String, Object>> myPermissions(
            HttpServletRequest request) {

    /* ========================================================
       1. 获取当前登录用户
       ======================================================== */

        Long currentUserId;

        try {

            currentUserId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(
                    e.getMessage()
            );

        }


        if (currentUserId == null) {

            return Result.fail(
                    "请先登录"
            );

        }


        System.out.println(
                "========================================"
        );

        System.out.println(
                "部门权限检查，当前用户ID = "
                        + currentUserId
        );


    /* ========================================================
       2. 查询当前用户所有有效部门关系

       这里非常重要：

       不要在 SQL 查询阶段直接过滤 position。

       先把用户所有有效部门关系查出来，
       然后 Java 再判断岗位。

       这样可以避免数据库中的岗位文字
       与前端显示存在轻微差异时，
       直接导致整个部门消失。
       ======================================================== */

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
                );


        System.out.println(
                "当前用户有效部门关系数量 = "
                        + relations.size()
        );


    /* ========================================================
       3. 准备返回数据
       ======================================================== */

        List<Map<String, Object>> departments =
                new ArrayList<>();


        boolean canDepartmentApply = false;

        boolean canDepartmentAudit = false;


    /* ========================================================
       4. 遍历部门关系
       ======================================================== */

        for (
                SysUserDepartment relation
                : relations
        ) {

            if (
                    relation == null
                            || relation.getDepartmentId() == null
            ) {

                continue;

            }


            String position =
                    relation.getPosition();


            System.out.println(
                    "部门关系："
                            + "departmentId="
                            + relation.getDepartmentId()
                            + ", position="
                            + position
            );


        /* ====================================================
           5. 岗位判断

           干事 / 副部长 / 部长
           都可以进行部门申报
           ==================================================== */

            boolean applyPermission =
                    canApply(position);


        /* ====================================================
           6. 岗位判断

           副部长 / 部长
           可以进行部门审核
           ==================================================== */

            boolean auditPermission =
                    canAudit(position);


            /*
             * 如果岗位本身不是部门干部，
             * 直接跳过。
             *
             * 例如：
             *
             * 普通成员
             * 普通学生
             * 其他岗位
             */
            if (
                    !applyPermission
                            && !auditPermission
            ) {

                continue;

            }


        /* ====================================================
           7. 查询部门

           注意：
           这里不再因为 department.status
           导致整个权限直接消失。

           只要部门存在，就返回部门身份。
           ==================================================== */

            Department department =
                    departmentMapper.selectById(
                            relation.getDepartmentId()
                    );


            String departmentName;


            if (department != null) {

                departmentName =
                        department.getName();

            } else {

                /*
                 * 如果部门表查询不到，
                 * 至少保留部门 ID。
                 *
                 * 这样前端不会直接显示：
                 *
                 * 当前没有部门干部身份
                 *
                 * 而是可以明确看到部门关系存在。
                 */

                departmentName =
                        "部门 " +
                                relation.getDepartmentId();

            }


        /* ====================================================
           8. 构造部门信息
           ==================================================== */

            Map<String, Object> item =
                    new HashMap<>();


            item.put(
                    "departmentId",
                    relation.getDepartmentId()
            );


            item.put(
                    "departmentName",
                    departmentName
            );


            item.put(
                    "position",
                    position
            );


            departments.add(item);


        /* ====================================================
           9. 设置权限
           ==================================================== */

            if (applyPermission) {

                canDepartmentApply = true;

            }


            if (auditPermission) {

                canDepartmentAudit = true;

            }

        }


    /* ========================================================
       10. 返回权限
       ======================================================== */

        Map<String, Object> data =
                new HashMap<>();


        data.put(
                "canDepartmentApply",
                canDepartmentApply
        );


        data.put(
                "canDepartmentAudit",
                canDepartmentAudit
        );


        data.put(
                "departments",
                departments
        );


        System.out.println(
                "最终部门数量 = "
                        + departments.size()
        );


        System.out.println(
                "canDepartmentApply = "
                        + canDepartmentApply
        );


        System.out.println(
                "canDepartmentAudit = "
                        + canDepartmentAudit
        );


        System.out.println(
                "========================================"
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
     * ★★★ 这里是这次最重要的修改
     *
     * 逻辑：
     *
     * 1. 当前登录用户
     * 2. 必须是部长 / 副部长
     * 3. 找出其负责的所有部门
     * 4. 查询这些部门的 status = 0 申报
     * 5. 不再额外限制申请人
     *
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


        System.out.println(
                "========== 部门审核列表 =========="
        );


        System.out.println(
                "当前审核用户 ID = "
                        + currentUserId
        );


        /*
         * =====================================================
         * 查询当前用户所有部长 / 副部长身份
         * =====================================================
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

                                .orderByAsc(
                                        SysUserDepartment::getDepartmentId
                                )
                );


        System.out.println(
                "当前用户部长/副部长身份数量 = "
                        + myPositions.size()
        );


        /*
         * =====================================================
         * 没有审核身份
         * =====================================================
         */
        if (myPositions.isEmpty()) {

            System.out.println(
                    "当前用户没有部长/副部长身份"
            );

            System.out.println(
                    "================================"
            );


            return Result.success(
                    new ArrayList<>()
            );
        }


        /*
         * =====================================================
         * 获取负责部门 ID
         * =====================================================
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


        System.out.println(
                "当前用户负责部门 = "
                        + departmentIds
        );


        /*
         * =====================================================
         * 没有部门
         * =====================================================
         */
        if (departmentIds.isEmpty()) {

            System.out.println(
                    "当前用户没有有效部门"
            );

            System.out.println(
                    "================================"
            );


            return Result.success(
                    new ArrayList<>()
            );
        }


        /*
         * =====================================================
         * ★★★ 查询待审核申报
         *
         * department_id IN 当前用户负责部门
         *
         * status = 0
         *
         * 这里不再写 applicantId != currentUserId。
         *
         * 因为“不能审核自己的申报”应该在真正执行审核时
         * 再进行判断，而不是影响待审核列表展示。
         *
         * =====================================================
         */
        List<DepartmentScoreApply> list =

                applyService.list(

                        new LambdaQueryWrapper<DepartmentScoreApply>()

                                /*
                                 * =====================================================
                                 * 只查询当前用户负责部门的待审核申报
                                 * =====================================================
                                 */

                                .in(
                                        DepartmentScoreApply::getDepartmentId,
                                        departmentIds
                                )

                                /*
                                 * 只查询待审核
                                 *
                                 * 0 = 待审核
                                 */

                                .eq(
                                        DepartmentScoreApply::getStatus,
                                        (short) 0
                                )

                                /*
                                 * =====================================================
                                 * ★★★ 最关键
                                 *
                                 * 不能审核自己提交的申报。
                                 *
                                 * 这里直接从“待审核列表”里排除自己提交的，
                                 * 而不是等点击审核以后才报错。
                                 *
                                 * 所以：
                                 *
                                 * 部长自己提交的 → 不显示
                                 * 副部长自己提交的 → 不显示
                                 * 其他人提交的     → 正常显示
                                 * =====================================================
                                 */

                                .ne(
                                        DepartmentScoreApply::getApplicantId,
                                        currentUserId
                                )

                                /*
                                 * 最新申报排在前面
                                 */

                                .orderByDesc(
                                        DepartmentScoreApply::getCreateTime
                                )
                );


        System.out.println(
                "查询到待审核申报数量 = "
                        + list.size()
        );


        /*
         * 输出每一条数据
         */
        for (DepartmentScoreApply apply :
                list) {

            System.out.println(
                    "待审核："
                            + "applyId="
                            + apply.getId()
                            + "，studentId="
                            + apply.getStudentId()
                            + "，applicantId="
                            + apply.getApplicantId()
                            + "，departmentId="
                            + apply.getDepartmentId()
                            + "，status="
                            + apply.getStatus()
                            + "，finalStatus="
                            + apply.getFinalStatus()
            );
        }


        fillNames(
                list
        );


        System.out.println(
                "================================"
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


        DepartmentScoreApply apply =
                applyService.getById(
                        id
                );


        if (apply == null) {

            return Result.fail(
                    "申报记录不存在"
            );
        }


        if (!Short.valueOf((short) 0)
                .equals(
                        apply.getStatus()
                )) {

            return Result.fail(
                    "该申报已经完成部门审核"
            );
        }


        /*
         * 审核人必须属于该部门
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
     * =========================================================
     */
    @PutMapping("/audit/batch-pass")
    @Transactional
    public Result<Map<String, Object>> batchPass(
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


        if (myPositions == null
                || myPositions.isEmpty()) {

            return Result.fail(
                    "你没有部门审核权限"
            );
        }


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
         * 查询待审核数据
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

                                .orderByAsc(
                                        DepartmentScoreApply::getCreateTime
                                )
                );


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


        LocalDateTime now =
                LocalDateTime.now();


        int successCount = 0;


        for (DepartmentScoreApply apply :
                list) {


            if (!Short.valueOf((short) 0)
                    .equals(
                            apply.getStatus()
                    )) {

                continue;
            }


            /*
             * 自己的申报仍然不能审批
             */
            if (currentUserId.equals(
                    apply.getApplicantId()
            )) {

                continue;
            }


            /*
             * 再次确认审核人属于这个部门
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


            apply.setStatus(
                    (short) 1
            );


            apply.setReviewerId(
                    currentUserId
            );


            apply.setReviewRemark(
                    "一键审批通过"
            );


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


            if (updated) {

                successCount++;
            }
        }


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
     * =========================================================
     */
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

            return Result.fail(
                    e.getMessage()
            );
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


        DepartmentScoreApply apply =
                applyService.getById(
                        id
                );


        if (apply == null) {

            return Result.fail(
                    "申报记录不存在"
            );
        }


        if (!Short.valueOf((short) 1)
                .equals(
                        apply.getStatus()
                )) {

            return Result.fail(
                    "该申报尚未通过部门审核"
            );
        }


        if (!Short.valueOf((short) 0)
                .equals(
                        apply.getFinalStatus()
                )) {

            return Result.fail(
                    "该申报已经完成终审"
            );
        }


        /*
         * 检查辅导员是否负责该部门
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
         * 终审驳回
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
         * 生成正式成绩
         * =====================================================
         */
        if (finalStatus == 1) {


            /*
             * 防止重复生成
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
             * 申报分值
             */
            BigDecimal realScore =
                    apply.getScore();


            if (realScore == null) {

                throw new IllegalArgumentException(
                        "该申报分值为空，无法生成成绩记录"
                );
            }


            /*
             * 减分转负数
             */
            if (Short.valueOf((short) -1)
                    .equals(
                            apply.getScoreType()
                    )) {

                realScore =
                        realScore.negate();
            }


            /*
             * 当前学期
             */
            SysSemester currentSemester =
                    getCurrentSemester();


            if (currentSemester == null) {

                throw new IllegalArgumentException(
                        "当前没有正在进行的学期，无法生成正式成绩记录"
                );
            }


            /*
             * 查找正式成绩规则
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
             * 正式规则分值
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
             * 申报分值必须与正式规则一致
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
             * 创建正式成绩
             */
            ScoreRecord record =
                    new ScoreRecord();


            record.setStudentId(
                    apply.getStudentId()
            );


            record.setRuleId(
                    scoreRule.getId()
            );


            record.setScore(
                    realScore
            );


            record.setSemesterId(
                    currentSemester.getId()
            );


            record.setSourceType(
                    "DEPARTMENT"
            );


            record.setSourceId(
                    apply.getId()
            );


            record.setStatus(
                    (short) 1
            );


            record.setAdminHidden(
                    (short) 0
            );


            record.setCreateTime(
                    now
            );


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
             * 日志
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
             * 学生姓名
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
             * 申报人姓名
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
             * 部门名称
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