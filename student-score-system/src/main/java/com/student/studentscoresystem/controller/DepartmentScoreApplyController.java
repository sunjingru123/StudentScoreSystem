package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.ArchiveBatchPassDTO;
import com.student.studentscoresystem.dto.DepartmentScoreApplyAddDTO;
import com.student.studentscoresystem.dto.DepartmentScoreApplyAuditDTO;
import com.student.studentscoresystem.dto.DepartmentScoreArchiveScoreDTO;
import com.student.studentscoresystem.dto.DepartmentScoreFinalAuditDTO;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.entity.DepartmentScoreTemplate;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.DepartmentScoreApplyImportService;
import com.student.studentscoresystem.service.DepartmentScoreArchiveAuditService;
import com.student.studentscoresystem.service.DepartmentScoreFinalizeService;
import com.student.studentscoresystem.service.IDepartmentScoreApplyService;
import com.student.studentscoresystem.service.IDepartmentScoreTemplateService;
import com.student.studentscoresystem.service.OrgScopeService;
import com.student.studentscoresystem.service.PovertyImportPermissionService;
import com.student.studentscoresystem.service.ScoreManagerPermissionService;
import com.student.studentscoresystem.utils.JwtUtil;
import com.student.studentscoresystem.utils.DepartmentScoreReviewRules;
import com.student.studentscoresystem.utils.OrgPositionLevel;
import com.student.studentscoresystem.vo.ArchiveScoreOptionsVO;
import com.student.studentscoresystem.vo.BatchAuditResultVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    private final DepartmentMapper departmentMapper;

    private final SysUserMapper sysUserMapper;

    private final DepartmentScoreFinalizeService departmentScoreFinalizeService;

    private final DepartmentScoreApplyImportService departmentScoreApplyImportService;

    private final ScoreManagerPermissionService scoreManagerPermissionService;

    private final DepartmentScoreArchiveAuditService departmentScoreArchiveAuditService;

    private final PovertyImportPermissionService povertyImportPermissionService;

    private final OrgScopeService orgScopeService;


    public DepartmentScoreApplyController(
            IDepartmentScoreApplyService applyService,
            IDepartmentScoreTemplateService templateService,
            SysUserDepartmentMapper userDepartmentMapper,
            DepartmentMapper departmentMapper,
            SysUserMapper sysUserMapper,
            DepartmentScoreFinalizeService departmentScoreFinalizeService,
            DepartmentScoreApplyImportService departmentScoreApplyImportService,
            ScoreManagerPermissionService scoreManagerPermissionService,
            DepartmentScoreArchiveAuditService departmentScoreArchiveAuditService,
            PovertyImportPermissionService povertyImportPermissionService,
            OrgScopeService orgScopeService) {

        this.applyService = applyService;

        this.templateService = templateService;

        this.userDepartmentMapper =
                userDepartmentMapper;

        this.departmentMapper =
                departmentMapper;

        this.sysUserMapper =
                sysUserMapper;

        this.departmentScoreFinalizeService =
                departmentScoreFinalizeService;

        this.departmentScoreApplyImportService =
                departmentScoreApplyImportService;

        this.scoreManagerPermissionService =
                scoreManagerPermissionService;

        this.departmentScoreArchiveAuditService =
                departmentScoreArchiveAuditService;

        this.povertyImportPermissionService =
                povertyImportPermissionService;

        this.orgScopeService =
                orgScopeService;
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
     * 组织申报权限
     *
     * 副职 / 正职（副部长、部长、副站长、站长、
     * 理事长、副理事长、主任、副主任 …）可以提交申报。
     *
     * 按权限层级判断，干事不再具备部门加减分申报权限。
     * =========================================================
     */
    private boolean canApply(
            String positionLevel) {

        return OrgPositionLevel.isLeaderOrDeputy(
                normalizeLevel(positionLevel)
        );
    }


    /*
     * =========================================================
     * 组织审核权限
     *
     * 已统一由 DepartmentScoreReviewRules 按组织层级判断：
     *   小社团：仅正职（部长 / 主任）
     *   部门：正职 / 副职
     *   大社团：正副职可审核旗下小社团
     * =========================================================
     */


    /**
     * 兼容历史数据：传入的可能是职位名称而不是层级
     */
    private String normalizeLevel(
            String value) {

        if (value == null || value.isBlank()) {

            return null;
        }

        if (OrgPositionLevel.isLeaderOrDeputy(value)) {

            return value;
        }

        return OrgPositionLevel.resolveLevel(value);
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
                OrgPositionLevel.levelOf(relation)
        )) {

            return Result.fail(
                    "你没有部门加减分申报权限"
            );
        }


        /*
         * =====================================================
         * 允许给自己申报
         *
         * 部门副部长 / 部长 也可以给自己申报加减分。
         * 自己的申报不会出现在本人的待审核列表，
         * 需要由部门其他负责人初审、辅导员终审；
         * 审核侧「不能审核自己的申报」的约束保持不变。
         * =====================================================
         */


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
         * 这里不做「同一学生 + 同一项目」判重：
         * 一个加减分项目（例如「课堂考勤旷课」）
         * 在同一学期内可以发生多次，每次都应各成一条申报。
         * 是否采纳由部门初审 / 辅导员终审决定。
         * 与 Excel 批量导入 /departmentScoreApply/import 保持同一口径。
         */


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
         * 审核初始状态按组织类型决定：
         *
         * 1. 部门：待部门初审（原有流程）；
         * 2. 大社团：负责人提交，直接进入档案部审核；
         * 3. 小社团正职：初审视为通过，待大社团审核；
         * 4. 小社团副职：待小社团正职初审，之后再由大社团审核。
         */
        String applicantLevel =
                OrgPositionLevel.levelOf(
                        relation
                );


        apply.setStatus(
                DepartmentScoreReviewRules.initialStatus(
                        department,
                        applicantLevel
                )
        );


        apply.setBigClubStatus(
                DepartmentScoreReviewRules.initialBigClubStatus(
                        department
                )
        );


        /*
         * 辅导员终审：
         *
         * 0 = 待审核
         */
        apply.setFinalStatus(
                (short) 0
        );


        /*
         * 档案部审核：
         *
         * 0 = 待档案部审核
         */
        apply.setArchiveStatus(
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


    /*
     * =========================================================
     * 下载部门申报导入模板
     *
     * 表头固定：学号 | 姓名 | 日期 | 项目 | 分值 | 备注
     * 其中「项目」列是下拉选择，只能选该部门已启用的项目。
     * =========================================================
     */
    @GetMapping("/import/template")
    public void importTemplate(
            @RequestParam("departmentId") Long departmentId,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        Long currentUserId;

        try {

            currentUserId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            writeDownloadError(
                    response,
                    e.getMessage()
            );

            return;
        }

        byte[] content;

        try {

            content =
                    departmentScoreApplyImportService
                            .buildTemplateWorkbook(
                                    departmentId,
                                    currentUserId
                            );

        } catch (IllegalArgumentException e) {

            writeDownloadError(
                    response,
                    e.getMessage()
            );

            return;
        }

        String fileName =
                URLEncoder.encode(
                        "部门加减分申报导入模板.xlsx",
                        StandardCharsets.UTF_8
                ).replace("+", "%20");

        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );

        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        response.setHeader(
                "Content-Disposition",
                "attachment;filename*=UTF-8''" + fileName
        );

        response.setContentLength(content.length);

        response.getOutputStream().write(content);

        response.flushBuffer();
    }


    /*
     * =========================================================
     * 导入部门加减分申报
     *
     * 只有该部门在职的副部长 / 部长可以导入，
     * 权限最终由 Service 层校验。
     * =========================================================
     */
    @PostMapping("/import")
    public Result<Map<String, Object>> importApplies(
            @RequestParam("departmentId") Long departmentId,
            @RequestParam("file") MultipartFile file,
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

        try {

            return Result.success(
                    departmentScoreApplyImportService
                            .importApplies(
                                    departmentId,
                                    currentUserId,
                                    file
                            )
            );

        } catch (IllegalArgumentException e) {

            return Result.fail(
                    e.getMessage()
            );
        }
    }


    /*
     * 下载失败时返回 JSON，前端 blob 请求可以提示真实原因
     */
    private void writeDownloadError(
            HttpServletResponse response,
            String message) throws IOException {

        response.reset();

        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        response.setContentType(
                "application/json;charset=UTF-8"
        );

        String safeMessage =
                message == null
                        ? "下载导入模板失败"
                        : message
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"");

        response.getWriter().write(
                "{\"code\":400,\"message\":\""
                        + safeMessage
                        + "\",\"data\":null}"
        );

        response.flushBuffer();
    }


    /**
     * ============================================================
     * 获取当前用户部门权限
     * ============================================================
     *
     * 权限规则：
     *
     * 干事：
     *      不可以部门加减分申报
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

        boolean canBigClubReview = false;


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
                    OrgPositionLevel.levelOf(relation);


            System.out.println(
                    "部门关系："
                            + "departmentId="
                            + relation.getDepartmentId()
                            + ", position="
                            + position
            );


        /* ====================================================
           5. 岗位判断

           副部长 / 部长
           可以进行部门申报（干事不具备该权限）
           ==================================================== */

            boolean applyPermission =
                    canApply(position);


        /* ====================================================
           6. 组织初审权限

           小社团：仅正职（部长 / 主任）
           部门：正职或副职（兼容原有流程）
           ==================================================== */

            Department relationOrg =
                    departmentMapper.selectById(
                            relation.getDepartmentId()
                    );


            boolean auditPermission =
                    DepartmentScoreReviewRules.canAuditFirstStage(
                            relationOrg,
                            position
                    );


        /* ====================================================
           6.1 大社团审核权限

           大社团的正职 / 副职可以审核旗下小社团申报
           ==================================================== */

            boolean bigClubAuditPermission =
                    DepartmentScoreReviewRules.isBigClub(
                            relationOrg
                    )
                            && OrgPositionLevel.isLeaderOrDeputy(
                            position
                    );


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


            if (bigClubAuditPermission) {

                canBigClubReview = true;

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


        /*
         * 大社团审核权限：
         * 主席 / 副主席 / 理事长 / 副理事长可审核旗下小社团申报。
         */
        data.put(
                "canBigClubReview",
                canBigClubReview
        );


    /* ========================================================
       10.1 辅导员端成绩删除相关权限

       高级辅导员 / 档案部负责辅导员可以直接删除学生成绩；
       审核档案部成员提交的删除申请则只开放给「分管档案部的辅导员」；
       档案部部长 / 副部长只能提交删除申请。

       前端路由守卫与布局菜单直接复用这里的返回值，
       避免额外网络请求。
       ======================================================== */

        boolean seniorCounselor =
                scoreManagerPermissionService
                        .isSeniorCounselor(currentUserId);


        boolean canExportAllScopes =
                scoreManagerPermissionService
                        .isAdmin(currentUserId)
                        || seniorCounselor;


        data.put(
                "seniorCounselor",
                seniorCounselor
        );


        data.put(
                "canExportAllScopes",
                canExportAllScopes
        );


        data.put(
                "canDeleteScoreDirectly",
                scoreManagerPermissionService
                        .canDeleteScoreDirectly(currentUserId)
        );


        data.put(
                "canReviewScoreDeleteApply",
                scoreManagerPermissionService
                        .canReviewScoreDeleteApply(currentUserId)
        );


        data.put(
                "canSubmitScoreDeleteApply",
                scoreManagerPermissionService
                        .canSubmitScoreDeleteApply(currentUserId)
        );


        /*
         * 档案部部门申报审核权限：
         * 仅档案部的部长 / 副部长可以在辅导员终审之前审核部门申报。
         */
        data.put(
                "canArchiveAudit",
                scoreManagerPermissionService
                        .isArchiveLeader(currentUserId)
        );


        /*
         * 困难学生名单导入权限：
         * 档案部部长（以及管理员）。
         */
        data.put(
                "canImportPoverty",
                povertyImportPermissionService
                        .canImportPovertyList(currentUserId)
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
                                        SysUserDepartment::getPositionLevel,
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


        /*
         * =====================================================
         * 按真实组织层级计算审核范围
         *
         * 1. 组织初审：
         *      部门正职 / 副职 → 本部门（原有流程）
         *      小社团正职      → 本小社团
         * 2. 大社团审核：
         *      大社团正职 / 副职 → 旗下所有小社团
         * =====================================================
         */
        AuditScope auditScope =
                resolveAuditScope(
                        myPositions
                );


        List<Long> firstStageOrgIds =
                auditScope.getFirstStageOrgIds();

        List<Long> childOrgIds =
                auditScope.getChildOrgIds();


        if (firstStageOrgIds.isEmpty()
                && childOrgIds.isEmpty()) {

            return Result.success(
                    new ArrayList<>()
            );
        }


        /*
         * =====================================================
         * 查询待审核申报
         *
         * A. 组织初审待审核：status = 0，范围为可初审的组织
         * B. 大社团待审核：status = 1 且 bigClubStatus = 0，
         *    范围为大社团旗下小社团
         * =====================================================
         */
        LambdaQueryWrapper<DepartmentScoreApply> auditWrapper =
                new LambdaQueryWrapper<>();


        auditWrapper.ne(
                DepartmentScoreApply::getApplicantId,
                currentUserId
        );


        auditWrapper.and(
                condition -> {

                    boolean hasFirstStage =
                            !firstStageOrgIds.isEmpty();

                    if (hasFirstStage) {

                        condition.and(
                                first -> first
                                        .eq(
                                                DepartmentScoreApply::getStatus,
                                                (short) 0
                                        )
                                        .in(
                                                DepartmentScoreApply::getDepartmentId,
                                                firstStageOrgIds
                                        )
                        );
                    }


                    if (!childOrgIds.isEmpty()) {

                        if (hasFirstStage) {

                            condition.or();
                        }

                        condition.and(
                                big -> big
                                        .eq(
                                                DepartmentScoreApply::getStatus,
                                                (short) 1
                                        )
                                        .eq(
                                                DepartmentScoreApply::getBigClubStatus,
                                                DepartmentScoreReviewRules.BIG_CLUB_PENDING
                                        )
                                        .in(
                                                DepartmentScoreApply::getDepartmentId,
                                                childOrgIds
                                        )
                        );
                    }
                }
        );


        auditWrapper.orderByDesc(
                DepartmentScoreApply::getCreateTime
        );


        List<DepartmentScoreApply> list =
                applyService.list(
                        auditWrapper
                );


        /*
         * 标记当前审核阶段，便于前端展示
         */
        if (list != null) {

            for (DepartmentScoreApply apply : list) {

                apply.setReviewStage(
                        DepartmentScoreReviewRules.waitingBigClubReview(apply)
                                ? "BIG_CLUB"
                                : "ORGANIZATION"
                );
            }
        }


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


        boolean firstStage =
                Short.valueOf((short) 0)
                        .equals(
                                apply.getStatus()
                        );

        boolean bigClubStage =
                DepartmentScoreReviewRules.waitingBigClubReview(
                        apply
                );


        if (!firstStage && !bigClubStage) {

            return Result.fail(
                    "该申报当前不在可审核阶段"
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


        Department applyOrg =
                departmentMapper.selectById(
                        apply.getDepartmentId()
                );


        if (applyOrg == null) {

            return Result.fail(
                    "申报所属组织不存在"
            );
        }


        SysUserDepartment myRelation =
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
                                .last("LIMIT 1")
                );


        String myLevel =
                myRelation == null
                        ? null
                        : OrgPositionLevel.levelOf(myRelation);


        LocalDateTime now =
                LocalDateTime.now();


        if (bigClubStage) {

            /*
             * 大社团审核：
             * 必须是该小社团所属大社团的正职 / 副职。
             */
            Department parentOrg =
                    applyOrg.getParentId() == null
                            ? null
                            : departmentMapper.selectById(
                            applyOrg.getParentId()
                    );


            SysUserDepartment parentRelation =
                    parentOrg == null
                            ? null
                            : userDepartmentMapper.selectOne(
                            new LambdaQueryWrapper<SysUserDepartment>()
                                    .eq(
                                            SysUserDepartment::getUserId,
                                            currentUserId
                                    )
                                    .eq(
                                            SysUserDepartment::getDepartmentId,
                                            parentOrg.getId()
                                    )
                                    .eq(
                                            SysUserDepartment::getStatus,
                                            (short) 1
                                    )
                                    .last("LIMIT 1")
                    );


            String parentLevel =
                    parentRelation == null
                            ? null
                            : OrgPositionLevel.levelOf(parentRelation);


            if (!DepartmentScoreReviewRules.canAuditBigClubStage(
                    applyOrg,
                    parentOrg,
                    parentLevel
            )) {

                return Result.fail(
                        "你不是该小社团所属大社团的负责人，没有审核权限"
                );
            }


            apply.setBigClubStatus(
                    status
            );

            apply.setBigClubReviewerId(
                    currentUserId
            );

            apply.setBigClubReviewRemark(
                    dto.getReviewRemark()
            );

            apply.setBigClubReviewTime(
                    now
            );

        } else {

            /*
             * 组织初审：
             * 小社团仅正职；部门正职 / 副职。
             */
            if (!DepartmentScoreReviewRules.canAuditFirstStage(
                    applyOrg,
                    myLevel
            )) {

                return Result.fail(
                        "你没有该组织的审核权限"
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

            apply.setReviewTime(
                    now
            );


            /*
             * 小社团初审后的去向：
             *
             * 驳回 → 大社团审核直接标记驳回，不再流转；
             * 通过 → 保持待大社团审核；
             * 数据异常（无上级大社团）→ 放行至档案部审核。
             */
            if (DepartmentScoreReviewRules.isSmallClub(
                    applyOrg
            )) {

                if (status == 2) {

                    apply.setBigClubStatus(
                            DepartmentScoreReviewRules.BIG_CLUB_REJECTED
                    );

                } else if (applyOrg.getParentId() == null) {

                    apply.setBigClubStatus(
                            DepartmentScoreReviewRules.BIG_CLUB_APPROVED
                    );
                }
            }
        }


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
                                        SysUserDepartment::getPositionLevel,
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
         * 审核范围按真实组织层级计算：
         * 部门正副职 → 本部门；小社团正职 → 本小社团；
         * 大社团正副职 → 旗下小社团
         */
        AuditScope auditScope =
                resolveAuditScope(
                        myPositions
                );


        List<Long> firstStageOrgIds =
                auditScope.getFirstStageOrgIds();

        List<Long> childOrgIds =
                auditScope.getChildOrgIds();


        if (firstStageOrgIds.isEmpty()
                && childOrgIds.isEmpty()) {

            return Result.fail(
                    "你当前没有需要审核的组织"
            );
        }


        /*
         * 查询待审核数据（组织初审 + 大社团审核）
         */
        LambdaQueryWrapper<DepartmentScoreApply> batchWrapper =
                new LambdaQueryWrapper<>();


        batchWrapper.ne(
                DepartmentScoreApply::getApplicantId,
                currentUserId
        );


        batchWrapper.and(
                condition -> {

                    boolean hasFirstStage =
                            !firstStageOrgIds.isEmpty();

                    if (hasFirstStage) {

                        condition.and(
                                first -> first
                                        .eq(
                                                DepartmentScoreApply::getStatus,
                                                (short) 0
                                        )
                                        .in(
                                                DepartmentScoreApply::getDepartmentId,
                                                firstStageOrgIds
                                        )
                        );
                    }


                    if (!childOrgIds.isEmpty()) {

                        if (hasFirstStage) {

                            condition.or();
                        }

                        condition.and(
                                big -> big
                                        .eq(
                                                DepartmentScoreApply::getStatus,
                                                (short) 1
                                        )
                                        .eq(
                                                DepartmentScoreApply::getBigClubStatus,
                                                DepartmentScoreReviewRules.BIG_CLUB_PENDING
                                        )
                                        .in(
                                                DepartmentScoreApply::getDepartmentId,
                                                childOrgIds
                                        )
                        );
                    }
                }
        );


        batchWrapper.orderByAsc(
                DepartmentScoreApply::getCreateTime
        );


        List<DepartmentScoreApply> list =
                applyService.list(
                        batchWrapper
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


            /*
             * 自己的申报仍然不能审批
             */
            if (currentUserId.equals(
                    apply.getApplicantId()
            )) {

                continue;
            }


            boolean firstStage =
                    Short.valueOf((short) 0)
                            .equals(
                                    apply.getStatus()
                            );

            boolean bigClubStage =
                    DepartmentScoreReviewRules.waitingBigClubReview(
                            apply
                    );


            if (!firstStage && !bigClubStage) {

                continue;
            }


            Department applyOrg =
                    departmentMapper.selectById(
                            apply.getDepartmentId()
                    );


            if (applyOrg == null) {

                continue;
            }


            if (bigClubStage) {

                /*
                 * 大社团审核：必须是其下属小社团
                 */
                if (!childOrgIds.contains(
                        apply.getDepartmentId()
                )) {

                    continue;
                }


                apply.setBigClubStatus(
                        DepartmentScoreReviewRules.BIG_CLUB_APPROVED
                );

                apply.setBigClubReviewerId(
                        currentUserId
                );

                apply.setBigClubReviewRemark(
                        "一键审批通过"
                );

                apply.setBigClubReviewTime(
                        now
                );

            } else {

                /*
                 * 组织初审范围（部门正副职 / 小社团正职）
                 */
                if (!firstStageOrgIds.contains(
                        apply.getDepartmentId()
                )) {

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


                /*
                 * 小社团无上级大社团（数据异常）→ 直接放行至档案部
                 */
                if (DepartmentScoreReviewRules.isSmallClub(
                        applyOrg
                ) && applyOrg.getParentId() == null) {

                    apply.setBigClubStatus(
                            DepartmentScoreReviewRules.BIG_CLUB_APPROVED
                    );
                }
            }


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
     * 档案部待审核列表
     *
     * 部门初审通过（status = 1）、尚未档案部审核（archiveStatus = 0）
     * 的申报，由档案部部长 / 副部长在这里审核。
     * =========================================================
     */
    @GetMapping("/archive-audit/list")
    public Result<List<DepartmentScoreApply>>
    archiveAuditList(
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


        if (!scoreManagerPermissionService
                .isArchiveLeader(currentUserId)) {

            return Result.fail(
                    "你不是档案部的部长或副部长，没有审核权限"
            );
        }


        List<DepartmentScoreApply> list =

                applyService.list(

                        new LambdaQueryWrapper<DepartmentScoreApply>()

                                .eq(
                                        DepartmentScoreApply::getStatus,
                                        (short) 1
                                )

                                .eq(
                                        DepartmentScoreApply::getArchiveStatus,
                                        (short) 0
                                )

                                /*
                                 * 小社团申报必须先通过大社团审核，
                                 * 才能进入档案部审核。
                                 * 部门申报的 bigClubStatus 为 NULL，不受影响。
                                 */
                                .and(
                                        condition -> condition
                                                .isNull(
                                                        DepartmentScoreApply::getBigClubStatus
                                                )
                                                .or()
                                                .eq(
                                                        DepartmentScoreApply::getBigClubStatus,
                                                        DepartmentScoreReviewRules.BIG_CLUB_APPROVED
                                                )
                                )

                                /*
                                 * 自己的申报不能自己审批
                                 */
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
     * 档案部单条审核（通过 / 驳回）
     *
     * 该环节只更新档案部审核字段，不生成成绩；
     * 成绩仍由辅导员终审通过后生成。
     * =========================================================
     */
    @PutMapping("/archive-audit/{id}")
    public Result<Void> archiveAudit(
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


        String message =
                departmentScoreArchiveAuditService
                        .archiveAudit(
                                id,
                                currentUserId,
                                dto.getStatus(),
                                dto.getReviewRemark()
                        );


        if (message != null) {

            return Result.fail(
                    message
            );
        }


        return Result.success(
                null
        );
    }


    /*
     * =========================================================
     * 档案部批量审批
     * =========================================================
     *
     * 支持两种用法：
     *   1. 不传请求体 / ids 为空：一键通过全部待审核；
     *   2. 传入 ids：只通过选中的申报。
     */
    @PutMapping("/archive-audit/batch-pass")
    public Result<BatchAuditResultVO> archiveBatchPass(
            @RequestBody(required = false) ArchiveBatchPassDTO dto,
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


        if (!scoreManagerPermissionService
                .isArchiveLeader(currentUserId)) {

            return Result.fail(
                    "你不是档案部的部长或副部长，没有审核权限"
            );
        }


        BatchAuditResultVO result =
                departmentScoreArchiveAuditService
                        .batchPass(
                                currentUserId,
                                dto == null
                                        ? null
                                        : dto.getIds()
                        );


        return Result.success(
                result
        );
    }


    /*
     * =========================================================
     * 档案部审核时查询可调整的分值范围
     *
     * restricted = true  只能从 allowedScores 中选择正式规则分值
     * restricted = false 该项目没有正式规则，可自由填写正数
     * =========================================================
     */
    @GetMapping("/archive-audit/{id}/score-options")
    public Result<ArchiveScoreOptionsVO> archiveScoreOptions(
            @PathVariable Long id,
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


        if (!scoreManagerPermissionService
                .isArchiveLeader(currentUserId)) {

            return Result.fail(
                    "你不是档案部的部长或副部长，没有审核权限"
            );
        }


        return Result.success(
                departmentScoreArchiveAuditService
                        .scoreOptions(
                                id
                        )
        );
    }


    /*
     * =========================================================
     * 档案部审核时调整申报分值
     *
     * 只修改申报分值，不改变加减分类型与审核状态；
     * 分值必须落在该项目的正式规则分值集合内，
     * 否则辅导员终审无法生成成绩。
     * =========================================================
     */
    @PutMapping("/archive-audit/{id}/score")
    public Result<Void> archiveUpdateScore(
            @PathVariable Long id,
            @RequestBody DepartmentScoreArchiveScoreDTO dto,
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


        if (!scoreManagerPermissionService
                .isArchiveLeader(currentUserId)) {

            return Result.fail(
                    "你不是档案部的部长或副部长，没有审核权限"
            );
        }


        if (dto == null
                || dto.getScore() == null) {

            return Result.fail(
                    "请填写调整后的分值"
            );
        }


        String message =
                departmentScoreArchiveAuditService
                        .updateScore(
                                id,
                                currentUserId,
                                dto.getScore()
                        );


        if (message != null) {

            return Result.fail(
                    message
            );
        }


        return Result.success(
                null
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

                                /*
                                 * 只有通过档案部审核的申报才进入辅导员终审
                                 */
                                .eq(
                                        DepartmentScoreApply::getArchiveStatus,
                                        (short) 1
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

    /**
     * 辅导员首页：今日提交的全部部门申报（不受审核状态限制）。
     */
    @GetMapping("/final-audit/today")
    public Result<List<DepartmentScoreApply>> finalAuditToday(
            HttpServletRequest request) {
        Long currentUserId;
        try {
            currentUserId = getCurrentUserId(request);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }

        List<Long> departmentIds = departmentMapper.selectList(
                        new LambdaQueryWrapper<Department>()
                                .eq(Department::getTeacherId, currentUserId)
                                .eq(Department::getStatus, (short) 1)
                ).stream()
                .map(Department::getId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (departmentIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        LocalDate today = LocalDate.now();
        List<DepartmentScoreApply> list = applyService.list(
                new LambdaQueryWrapper<DepartmentScoreApply>()
                        .in(DepartmentScoreApply::getDepartmentId, departmentIds)
                        .ge(DepartmentScoreApply::getCreateTime, today.atStartOfDay())
                        .lt(DepartmentScoreApply::getCreateTime, today.plusDays(1).atStartOfDay())
                        .orderByDesc(DepartmentScoreApply::getCreateTime)
        );
        fillNames(list);
        return Result.success(list);
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


        /*
         * 必须先通过档案部审核，才能进入辅导员终审
         */
        if (!Short.valueOf((short) 1)
                .equals(
                        apply.getArchiveStatus()
                )) {

            return Result.fail(
                    "该申报尚未通过档案部审核"
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


        /*
         * =====================================================
         * 终审驳回
         * =====================================================
         */
        if (finalStatus == 2) {

            LocalDateTime now =
                    LocalDateTime.now();


            apply.setFinalStatus(
                    (short) 2
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

                return Result.fail(
                        "终审状态更新失败"
                );
            }


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
         * 正式成绩生成已下沉到 Service，
         * 在同一个事务内完成：
         * 重复守卫 + 分值校验 + 学期校验 + 规则匹配 +
         * ScoreRecord + 申报状态。
         * =====================================================
         */

        try {

            String message =
                    departmentScoreFinalizeService
                            .finalizeApproval(
                                    apply,
                                    currentUserId,
                                    dto.getReviewRemark()
                            );

            if (message != null) {

                return Result.error(
                        message
                );
            }

            return Result.success(
                    null
            );

        } catch (DuplicateKeyException e) {

            /*
             * 并发场景：
             *
             * 另一个请求已经为同一申报生成成绩记录，
             * 唯一索引 uk_score_record_source 拦截了重复写入，
             * 事务整体回滚，这里做幂等提示。
             */
            return Result.error(
                    "该申报已经生成正式成绩记录，请勿重复生成"
            );
        }
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


    /**
     * =========================================================
     * 按真实组织层级计算当前用户的审核范围
     * =========================================================
     */
    private AuditScope resolveAuditScope(
            List<SysUserDepartment> myPositions) {

        AuditScope scope =
                new AuditScope();


        if (myPositions == null
                || myPositions.isEmpty()) {

            return scope;
        }


        List<Long> bigClubIds =
                new ArrayList<>();


        for (SysUserDepartment myPosition : myPositions) {

            if (myPosition == null
                    || myPosition.getDepartmentId() == null) {

                continue;
            }


            Department relationOrg =
                    departmentMapper.selectById(
                            myPosition.getDepartmentId()
                    );


            String level =
                    OrgPositionLevel.levelOf(
                            myPosition
                    );


            if (DepartmentScoreReviewRules.canAuditFirstStage(
                    relationOrg,
                    level
            )) {

                scope.getFirstStageOrgIds().add(
                        myPosition.getDepartmentId()
                );
            }


            if (DepartmentScoreReviewRules.isBigClub(
                    relationOrg
            ) && OrgPositionLevel.isLeaderOrDeputy(level)) {

                bigClubIds.add(
                        myPosition.getDepartmentId()
                );
            }
        }


        if (bigClubIds.isEmpty()) {

            return scope;
        }


        List<Department> children =
                departmentMapper.selectList(
                        new LambdaQueryWrapper<Department>()
                                .in(
                                        Department::getParentId,
                                        bigClubIds
                                )
                );


        if (children != null) {

            for (Department child : children) {

                if (child != null
                        && child.getId() != null) {

                    scope.getChildOrgIds().add(
                            child.getId()
                    );
                }
            }
        }


        return scope;
    }


    /**
     * 当前用户的审核范围
     */
    private static final class AuditScope {

        /** 可做组织初审的组织 ID */
        private final List<Long> firstStageOrgIds =
                new ArrayList<>();

        /** 可做大社团审核的下级小社团 ID */
        private final List<Long> childOrgIds =
                new ArrayList<>();

        private List<Long> getFirstStageOrgIds() {

            return firstStageOrgIds;
        }

        private List<Long> getChildOrgIds() {

            return childOrgIds;
        }
    }
}
