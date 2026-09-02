package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.*;
import com.student.studentscoresystem.mapper.*;
import com.student.studentscoresystem.utils.JwtUtil;
import com.student.studentscoresystem.vo.ScoreApplyVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/scoreApply")
public class ScoreApplyController {

    private final ScoreApplyMapper scoreApplyMapper;

    private final SysUserMapper sysUserMapper;

    private final ScoreRuleMapper scoreRuleMapper;

    private final ObjectMapper objectMapper;

    private final SysUserDepartmentMapper userDepartmentMapper;

    private final DepartmentMapper departmentMapper;

    private final ScoreRecordMapper scoreRecordMapper;

    private final SysSemesterMapper sysSemesterMapper;

    public ScoreApplyController(
            ScoreApplyMapper scoreApplyMapper,
            SysUserMapper sysUserMapper,
            ScoreRuleMapper scoreRuleMapper,
            ObjectMapper objectMapper,
            SysUserDepartmentMapper userDepartmentMapper,
            DepartmentMapper departmentMapper,
            ScoreRecordMapper scoreRecordMapper,
            SysSemesterMapper sysSemesterMapper
    ) {

        this.scoreApplyMapper =
                scoreApplyMapper;

        this.sysUserMapper =
                sysUserMapper;

        this.scoreRuleMapper =
                scoreRuleMapper;

        this.objectMapper =
                objectMapper;

        this.userDepartmentMapper =
                userDepartmentMapper;

        this.departmentMapper =
                departmentMapper;

        this.scoreRecordMapper =
                scoreRecordMapper;

        this.sysSemesterMapper =
                sysSemesterMapper;
    }

    /**
     * ========================================================
     * 获取当前登录用户ID
     * ========================================================
     */
    private Long getCurrentUserId(
            HttpServletRequest request
    ) {

        String token =
                request.getHeader("Authorization");

        if (
                token == null
                        || !token.startsWith("Bearer ")
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

    /**
     * ========================================================
     * 判断是否为档案部干事 / 副部长 / 部长
     * ========================================================
     */
    private boolean isArchiveDepartmentMember(
            Long userId
    ) {

        if (userId == null) {
            return false;
        }

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
                                .in(
                                        SysUserDepartment::getPosition,
                                        List.of(
                                                "干事",
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

        for (
                SysUserDepartment relation :
                relations
        ) {

            if (
                    relation == null
                            || relation.getDepartmentId() == null
            ) {
                continue;
            }

            Department department =
                    departmentMapper.selectById(
                            relation.getDepartmentId()
                    );

            if (
                    department != null
                            &&
                            Short.valueOf((short) 1)
                                    .equals(
                                            department.getStatus()
                                    )
                            &&
                            "档案部".equals(
                                    department.getName()
                            )
            ) {

                return true;
            }
        }

        return false;
    }

    /**
     * ========================================================
     * 判断是否为档案部副部长 / 部长
     *
     * 只有副部长、部长可以终审
     * ========================================================
     */
    private boolean isArchiveFinalReviewer(
            Long userId
    ) {

        if (userId == null) {
            return false;
        }

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

        for (
                SysUserDepartment relation :
                relations
        ) {

            if (
                    relation == null
                            || relation.getDepartmentId() == null
            ) {
                continue;
            }

            Department department =
                    departmentMapper.selectById(
                            relation.getDepartmentId()
                    );

            if (
                    department != null
                            &&
                            Short.valueOf((short) 1)
                                    .equals(
                                            department.getStatus()
                                    )
                            &&
                            "档案部".equals(
                                    department.getName()
                            )
            ) {

                return true;
            }
        }

        return false;
    }

    /**
     * ========================================================
     * 学生查询自己的个人证书申报记录
     * ========================================================
     */
    @GetMapping("/my")
    public Result<List<ScoreApplyVO>> myList(
            HttpServletRequest request
    ) {

        Long currentStudentId;

        try {

            currentStudentId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(
                    e.getMessage()
            );
        }

        List<ScoreApply> applies =
                scoreApplyMapper.selectList(
                        new LambdaQueryWrapper<ScoreApply>()
                                .eq(
                                        ScoreApply::getStudentId,
                                        currentStudentId
                                )
                                .eq(
                                        ScoreApply::getApplyType,
                                        "CERTIFICATE"
                                )
                                .orderByDesc(
                                        ScoreApply::getCreateTime
                                )
                );

        List<ScoreApplyVO> voList =
                new ArrayList<>();

        for (
                ScoreApply apply :
                applies
        ) {

            voList.add(
                    convertToVO(apply)
            );
        }

        return Result.success(
                voList
        );
    }

    /**
     * ========================================================
     * 学生提交个人证书申报
     * ========================================================
     *
     * 学生不能填写：
     *
     * ruleId
     * applyScore
     * score
     * studentId
     *
     * 最终分值由档案部初审人员确定。
     */
    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody JsonNode data,
            HttpServletRequest request
    ) {

        Long currentStudentId;

        try {

            currentStudentId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(
                    e.getMessage()
            );
        }

        if (data == null) {

            return Result.error(
                    "申报数据不能为空"
            );
        }

        /*
         * =====================================================
         * 获取个人证书字段
         * =====================================================
         */

        String awardCategory =
                getText(
                        data,
                        "awardCategory"
                );

        String awardName =
                getText(
                        data,
                        "awardName"
                );

        String awardLevel =
                getText(
                        data,
                        "awardLevel"
                );

        String awardGrade =
                getText(
                        data,
                        "awardGrade"
                );

        String awardGradeOther =
                getText(
                        data,
                        "awardGradeOther"
                );

        String awardTime =
                getText(
                        data,
                        "awardTime"
                );

        String awardType =
                getText(
                        data,
                        "awardType"
                );

        String hasCertificate =
                getText(
                        data,
                        "hasCertificate"
                );

        String certificateReason =
                getText(
                        data,
                        "certificateReason"
                );

        String description =
                getText(
                        data,
                        "description"
                );

        String materialFile =
                getText(
                        data,
                        "materialFile"
                );

        /*
         * =====================================================
         * 获奖类别
         * =====================================================
         */

        if (
                awardCategory == null
                        || awardCategory.isBlank()
        ) {

            return Result.error(
                    "请选择获奖类别"
            );
        }

        if (
                !(
                        "A".equals(awardCategory)
                                ||
                                "B".equals(awardCategory)
                                ||
                                "C".equals(awardCategory)
                                ||
                                "OTHER".equals(awardCategory)
                )
        ) {

            return Result.error(
                    "获奖类别无效"
            );
        }

        /*
         * =====================================================
         * 获奖名称
         * =====================================================
         */

        if (
                awardName == null
                        || awardName.isBlank()
        ) {

            return Result.error(
                    "请输入获奖名称"
            );
        }

        /*
         * =====================================================
         * 获奖级别
         * =====================================================
         */

        if (
                awardLevel == null
                        || awardLevel.isBlank()
        ) {

            return Result.error(
                    "请选择获奖级别"
            );
        }

        if (
                !(
                        "国家级".equals(awardLevel)
                                ||
                                "省级".equals(awardLevel)
                                ||
                                "校级".equals(awardLevel)
                                ||
                                "院级".equals(awardLevel)
                )
        ) {

            return Result.error(
                    "获奖级别无效"
            );
        }

        /*
         * =====================================================
         * 获奖等级
         * =====================================================
         */

        if (
                awardGrade == null
                        || awardGrade.isBlank()
        ) {

            return Result.error(
                    "请选择获奖等级"
            );
        }

        if (
                !(
                        "一等奖".equals(awardGrade)
                                ||
                                "二等奖".equals(awardGrade)
                                ||
                                "三等奖".equals(awardGrade)
                                ||
                                "优秀奖".equals(awardGrade)
                                ||
                                "OTHER".equals(awardGrade)
                )
        ) {

            return Result.error(
                    "获奖等级无效"
            );
        }

        if (
                "OTHER".equals(awardGrade)
        ) {

            if (
                    awardGradeOther == null
                            || awardGradeOther.isBlank()
            ) {

                return Result.error(
                        "请输入其他获奖等级"
                );
            }
        }

        /*
         * =====================================================
         * 获奖时间
         * =====================================================
         */

        if (
                awardTime == null
                        || awardTime.isBlank()
        ) {

            return Result.error(
                    "请选择获奖时间"
            );
        }

        /*
         * =====================================================
         * 奖项类型
         * =====================================================
         */

        if (
                awardType == null
                        || awardType.isBlank()
        ) {

            return Result.error(
                    "请选择奖项类型"
            );
        }

        if (
                !(
                        "个人奖".equals(awardType)
                                ||
                                "团体奖".equals(awardType)
                )
        ) {

            return Result.error(
                    "奖项类型无效"
            );
        }

        /*
         * =====================================================
         * 获奖凭证
         * =====================================================
         */

        if (
                hasCertificate == null
                        || hasCertificate.isBlank()
        ) {

            return Result.error(
                    "请选择是否有获奖凭证"
            );
        }

        if (
                !(
                        "YES".equals(hasCertificate)
                                ||
                                "NO".equals(hasCertificate)
                )
        ) {

            return Result.error(
                    "获奖凭证选项无效"
            );
        }

        if (
                "YES".equals(hasCertificate)
        ) {

            if (
                    materialFile == null
                            || materialFile.isBlank()
            ) {

                return Result.error(
                        "有获奖凭证时必须上传凭证材料"
                );
            }
        }

        if (
                "NO".equals(hasCertificate)
        ) {

            if (
                    certificateReason == null
                            || certificateReason.isBlank()
            ) {

                return Result.error(
                        "没有获奖凭证时必须填写原因"
                );
            }
        }

        /*
         * =====================================================
         * 打包个人证书信息
         * =====================================================
         */

        CertificateDescription certificate =
                new CertificateDescription();

        certificate.awardCategory =
                awardCategory;

        certificate.awardName =
                awardName;

        certificate.awardLevel =
                awardLevel;

        certificate.awardGrade =
                awardGrade;

        certificate.awardGradeOther =
                awardGradeOther;

        certificate.awardTime =
                awardTime;

        certificate.awardType =
                awardType;

        certificate.hasCertificate =
                hasCertificate;

        certificate.certificateReason =
                certificateReason;

        certificate.description =
                description;

        String descriptionJson;

        try {

            descriptionJson =
                    objectMapper.writeValueAsString(
                            certificate
                    );

        } catch (
                JsonProcessingException e
        ) {

            e.printStackTrace();

            return Result.error(
                    "证书信息保存失败"
            );
        }

        /*
         * =====================================================
         * 创建申请
         * =====================================================
         */

        ScoreApply apply =
                new ScoreApply();

        apply.setStudentId(
                currentStudentId
        );

        apply.setApplyType(
                "CERTIFICATE"
        );

        /*
         * 学生不能填写分值
         */
        apply.setApplyScore(
                null
        );

        apply.setActivityId(
                null
        );

        /*
         * 个人证书暂不绑定规则
         */
        apply.setRuleId(
                null
        );

        apply.setMaterialFile(
                "YES".equals(hasCertificate)
                        ? materialFile
                        : null
        );

        apply.setDescription(
                descriptionJson
        );

        /*
         * 0 = 待初审
         */
        apply.setStatus(
                (short) 0
        );

        apply.setPreliminaryStatus(
                (short) 0
        );

        apply.setFinalStatus(
                (short) 0
        );

        apply.setCreateTime(
                LocalDateTime.now()
        );

        apply.setUpdateTime(
                LocalDateTime.now()
        );

        try {

            int result =
                    scoreApplyMapper.insert(
                            apply
                    );

            if (result > 0) {

                return Result.success(
                        null
                );
            }

            return Result.error(
                    "提交失败"
            );

        } catch (
                Exception e
        ) {

            e.printStackTrace();

            return Result.error(
                    "数据库写入失败：" +
                            e.getMessage()
            );
        }
    }

    /**
     * ========================================================
     * 档案部查询所有个人证书申请
     * ========================================================
     */
    @GetMapping("/list")
    public Result<Page<ScoreApplyVO>> list(
            @RequestParam(
                    defaultValue = "1"
            )
            long pageNum,

            @RequestParam(
                    defaultValue = "10"
            )
            long pageSize,

            @RequestParam(
                    required = false
            )
            String studentNo,

            @RequestParam(
                    required = false
            )
            String className,

            @RequestParam(
                    required = false
            )
            String awardCategory,

            @RequestParam(
                    required = false
            )
            String hasCertificate,

            @RequestParam(
                    required = false
            )
            Integer status
    ) {

        List<Long> studentIds =
                null;

        /*
         * =====================================================
         * 根据学生信息筛选
         * =====================================================
         */

        if (
                (
                        studentNo != null
                                &&
                                !studentNo.isBlank()
                )
                        ||
                        (
                                className != null
                                        &&
                                        !className.isBlank()
                        )
        ) {

            LambdaQueryWrapper<SysUser>
                    userWrapper =
                    new LambdaQueryWrapper<>();

            if (
                    studentNo != null
                            && !studentNo.isBlank()
            ) {

                userWrapper.like(
                        SysUser::getStudentNo,
                        studentNo
                );
            }

            if (
                    className != null
                            && !className.isBlank()
            ) {

                userWrapper.like(
                        SysUser::getClassName,
                        className
                );
            }

            List<SysUser> users =
                    sysUserMapper.selectList(
                            userWrapper
                    );

            studentIds =
                    new ArrayList<>();

            for (
                    SysUser user :
                    users
            ) {

                studentIds.add(
                        user.getId()
                );
            }

            if (
                    studentIds.isEmpty()
            ) {

                Page<ScoreApplyVO> emptyPage =
                        new Page<>(
                                pageNum,
                                pageSize,
                                0
                        );

                emptyPage.setRecords(
                        new ArrayList<>()
                );

                return Result.success(
                        emptyPage
                );
            }
        }

        /*
         * =====================================================
         * 查询申请
         * =====================================================
         */

        LambdaQueryWrapper<ScoreApply>
                wrapper =
                new LambdaQueryWrapper<>();

        wrapper.eq(
                ScoreApply::getApplyType,
                "CERTIFICATE"
        );

        if (
                studentIds != null
        ) {

            wrapper.in(
                    ScoreApply::getStudentId,
                    studentIds
            );
        }

        if (
                status != null
        ) {

            wrapper.eq(
                    ScoreApply::getStatus,
                    status.shortValue()
            );
        }

        wrapper.orderByDesc(
                ScoreApply::getCreateTime
        );

        Page<ScoreApply> page =
                new Page<>(
                        pageNum,
                        pageSize
                );

        Page<ScoreApply> applyPage =
                scoreApplyMapper.selectPage(
                        page,
                        wrapper
                );

        List<ScoreApplyVO> voList =
                new ArrayList<>();

        for (
                ScoreApply apply :
                applyPage.getRecords()
        ) {

            ScoreApplyVO vo =
                    convertToVO(
                            apply
                    );

            if (
                    awardCategory != null
                            &&
                            !awardCategory.isBlank()
            ) {

                if (
                        !awardCategory.equals(
                                vo.getAwardCategory()
                        )
                ) {

                    continue;
                }
            }

            if (
                    hasCertificate != null
                            &&
                            !hasCertificate.isBlank()
            ) {

                if (
                        !hasCertificate.equals(
                                vo.getHasCertificate()
                        )
                ) {

                    continue;
                }
            }

            voList.add(
                    vo
            );
        }

        Page<ScoreApplyVO> voPage =
                new Page<>(
                        applyPage.getCurrent(),
                        applyPage.getSize(),
                        applyPage.getTotal()
                );

        voPage.setRecords(
                voList
        );

        return Result.success(
                voPage
        );
    }

    /**
     * ========================================================
     * 初审待审核
     *
     * 干事 / 副部长 / 部长均可以审核
     * ========================================================
     */
    @GetMapping("/pending")
    public Result<Page<ScoreApplyVO>> pending(
            @RequestParam(
                    defaultValue = "1"
            )
            long pageNum,

            @RequestParam(
                    defaultValue = "10"
            )
            long pageSize,

            @RequestParam(
                    required = false
            )
            String studentNo,

            @RequestParam(
                    required = false
            )
            String className,

            @RequestParam(
                    required = false
            )
            String awardCategory,

            @RequestParam(
                    required = false
            )
            String hasCertificate,

            HttpServletRequest request
    ) {

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
         * 必须是档案部干事 / 副部长 / 部长
         */
        if (
                !isArchiveDepartmentMember(
                        currentUserId
                )
        ) {

            return Result.fail(
                    "你没有档案部个人证书初审权限"
            );
        }

        /*
         * 只查询：
         *
         * 初审状态 = 0
         *
         * 不再使用旧 status 判断
         */
        LambdaQueryWrapper<ScoreApply>
                wrapper =
                new LambdaQueryWrapper<>();

        wrapper.eq(
                ScoreApply::getApplyType,
                "CERTIFICATE"
        );

        wrapper.eq(
                ScoreApply::getPreliminaryStatus,
                (short) 0
        );

        wrapper.orderByDesc(
                ScoreApply::getCreateTime
        );

        Page<ScoreApply> page =
                new Page<>(
                        pageNum,
                        pageSize
                );

        Page<ScoreApply> applyPage =
                scoreApplyMapper.selectPage(
                        page,
                        wrapper
                );

        List<ScoreApplyVO> voList =
                new ArrayList<>();

        for (
                ScoreApply apply :
                applyPage.getRecords()
        ) {

            ScoreApplyVO vo =
                    convertToVO(
                            apply
                    );

            /*
             * 学生筛选
             */
            if (
                    studentNo != null
                            &&
                            !studentNo.isBlank()
            ) {

                if (
                        vo.getStudentNo() == null
                                ||
                                !vo.getStudentNo()
                                        .contains(studentNo)
                ) {

                    continue;
                }
            }

            /*
             * 班级筛选
             */
            if (
                    className != null
                            &&
                            !className.isBlank()
            ) {

                if (
                        vo.getClassName() == null
                                ||
                                !vo.getClassName()
                                        .contains(className)
                ) {

                    continue;
                }
            }

            /*
             * 获奖类别
             */
            if (
                    awardCategory != null
                            &&
                            !awardCategory.isBlank()
            ) {

                if (
                        !awardCategory.equals(
                                vo.getAwardCategory()
                        )
                ) {

                    continue;
                }
            }

            /*
             * 是否有凭证
             */
            if (
                    hasCertificate != null
                            &&
                            !hasCertificate.isBlank()
            ) {

                if (
                        !hasCertificate.equals(
                                vo.getHasCertificate()
                        )
                ) {

                    continue;
                }
            }

            voList.add(
                    vo
            );
        }

        Page<ScoreApplyVO> voPage =
                new Page<>(
                        pageNum,
                        pageSize,
                        voList.size()
                );

        voPage.setRecords(
                voList
        );

        return Result.success(
                voPage
        );
    }

    /**
     * ========================================================
     * 初审
     *
     * 干事 / 副部长 / 部长
     *
     * 通过时必须填写认定分值
     * ========================================================
     */
    @PostMapping("/preliminary-audit")
    public Result<Void> preliminaryAudit(
            @RequestBody AuditRequest request,
            HttpServletRequest httpRequest
    ) {

        Long reviewerId;

        try {

            reviewerId =
                    getCurrentUserId(
                            httpRequest
                    );

        } catch (Exception e) {

            return Result.fail(
                    e.getMessage()
            );
        }

        /*
         * 权限
         */
        if (
                !isArchiveDepartmentMember(
                        reviewerId
                )
        ) {

            return Result.fail(
                    "你没有档案部个人证书初审权限"
            );
        }

        /*
         * 参数
         */
        if (
                request == null
        ) {

            return Result.error(
                    "审核参数不能为空"
            );
        }

        if (
                request.getId() == null
        ) {

            return Result.error(
                    "申请ID不能为空"
            );
        }

        if (
                request.getStatus() == null
        ) {

            return Result.error(
                    "审核状态不能为空"
            );
        }

        if (
                request.getStatus() != 1
                        &&
                        request.getStatus() != 2
        ) {

            return Result.error(
                    "审核状态无效"
            );
        }

        ScoreApply apply =
                scoreApplyMapper.selectById(
                        request.getId()
                );

        if (
                apply == null
        ) {

            return Result.error(
                    "申请不存在"
            );
        }

        if (
                !"CERTIFICATE".equals(
                        apply.getApplyType()
                )
        ) {

            return Result.error(
                    "该申请不是个人证书申请"
            );
        }

        /*
         * 防止重复初审
         */
        if (
                !Short.valueOf((short) 0)
                        .equals(
                                apply.getPreliminaryStatus()
                        )
        ) {

            return Result.error(
                    "该申请已经完成初审，不能重复审核"
            );
        }

        /*
         * =====================================================
         * 初审通过
         * =====================================================
         */
        if (
                request.getStatus() == 1
        ) {

            /*
             * 必须填写分值
             */
            if (
                    request.getFinalScore() == null
            ) {

                return Result.error(
                        "初审通过时必须填写认定分值"
                );
            }

            /*
             * 分值必须 > 0
             */
            if (
                    request.getFinalScore()
                            .compareTo(
                                    BigDecimal.ZERO
                            ) <= 0
            ) {

                return Result.error(
                        "认定分值必须大于0"
                );
            }

            /*
             * 保存档案部认定分值
             */
            apply.setApplyScore(
                    request.getFinalScore()
            );

            /*
             * 初审通过
             */
            apply.setPreliminaryStatus(
                    (short) 1
            );

            /*
             * 进入终审
             */
            apply.setFinalStatus(
                    (short) 0
            );

            /*
             * 旧总状态暂时保持待审核
             */
            apply.setStatus(
                    (short) 0
            );

        } else {

            /*
             * =================================================
             * 初审驳回
             * =================================================
             */

            apply.setApplyScore(
                    null
            );

            apply.setPreliminaryStatus(
                    (short) 2
            );

            /*
             * 驳回以后无需终审
             */
            apply.setFinalStatus(
                    (short) 2
            );

            apply.setStatus(
                    (short) 2
            );
        }

        apply.setPreliminaryReviewerId(
                reviewerId
        );

        apply.setPreliminaryReviewTime(
                LocalDateTime.now()
        );

        apply.setUpdateTime(
                LocalDateTime.now()
        );

        int result =
                scoreApplyMapper.updateById(
                        apply
                );

        if (
                result <= 0
        ) {

            return Result.error(
                    "初审操作失败"
            );
        }

        return Result.success(
                null
        );
    }

    /**
     * ========================================================
     * 终审待审核
     *
     * 只有档案部副部长 / 部长可以查看
     * ========================================================
     */
    @GetMapping("/final-pending")
    public Result<Page<ScoreApplyVO>> finalPending(
            @RequestParam(
                    defaultValue = "1"
            )
            long pageNum,

            @RequestParam(
                    defaultValue = "10"
            )
            long pageSize,

            HttpServletRequest request
    ) {

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
         * 只有副部长 / 部长
         */
        if (
                !isArchiveFinalReviewer(
                        currentUserId
                )
        ) {

            return Result.fail(
                    "只有档案部副部长、部长可以进行终审"
            );
        }

        LambdaQueryWrapper<ScoreApply>
                wrapper =
                new LambdaQueryWrapper<>();

        wrapper.eq(
                ScoreApply::getApplyType,
                "CERTIFICATE"
        );

        /*
         * 初审通过
         */
        wrapper.eq(
                ScoreApply::getPreliminaryStatus,
                (short) 1
        );

        /*
         * 等待终审
         */
        wrapper.eq(
                ScoreApply::getFinalStatus,
                (short) 0
        );

        wrapper.orderByDesc(
                ScoreApply::getCreateTime
        );

        Page<ScoreApply> page =
                new Page<>(
                        pageNum,
                        pageSize
                );

        Page<ScoreApply> result =
                scoreApplyMapper.selectPage(
                        page,
                        wrapper
                );

        List<ScoreApplyVO> records =
                new ArrayList<>();

        for (
                ScoreApply apply :
                result.getRecords()
        ) {

            records.add(
                    convertToVO(apply)
            );
        }

        Page<ScoreApplyVO> voPage =
                new Page<>(
                        result.getCurrent(),
                        result.getSize(),
                        result.getTotal()
                );

        voPage.setRecords(
                records
        );

        return Result.success(
                voPage
        );
    }

    /**
     * ========================================================
     * 终审
     *
     * 只有副部长 / 部长
     *
     * 终审通过以后：
     *
     * ScoreRecord
     *       ↓
     * 正式成绩
     * ========================================================
     */
    @PostMapping("/final-audit")
    @Transactional
    public Result<Void> finalAudit(
            @RequestBody AuditRequest request,
            HttpServletRequest httpRequest
    ) {

        Long reviewerId;

        try {

            reviewerId =
                    getCurrentUserId(
                            httpRequest
                    );

        } catch (Exception e) {

            return Result.fail(
                    e.getMessage()
            );
        }

        /*
         * =====================================================
         * 权限
         * =====================================================
         */

        if (
                !isArchiveFinalReviewer(
                        reviewerId
                )
        ) {

            return Result.fail(
                    "只有档案部副部长、部长可以进行终审"
            );
        }

        /*
         * =====================================================
         * 参数
         * =====================================================
         */

        if (
                request == null
        ) {

            return Result.error(
                    "审核参数不能为空"
            );
        }

        if (
                request.getId() == null
        ) {

            return Result.error(
                    "申请ID不能为空"
            );
        }

        if (
                request.getStatus() == null
        ) {

            return Result.error(
                    "审核状态不能为空"
            );
        }

        if (
                request.getStatus() != 1
                        &&
                        request.getStatus() != 2
        ) {

            return Result.error(
                    "审核状态无效"
            );
        }

        ScoreApply apply =
                scoreApplyMapper.selectById(
                        request.getId()
                );

        if (
                apply == null
        ) {

            return Result.error(
                    "申请不存在"
            );
        }

        if (
                !"CERTIFICATE".equals(
                        apply.getApplyType()
                )
        ) {

            return Result.error(
                    "该申请不是个人证书申请"
            );
        }

        /*
         * 必须通过初审
         */
        if (
                !Short.valueOf((short) 1)
                        .equals(
                                apply.getPreliminaryStatus()
                        )
        ) {

            return Result.error(
                    "该申请尚未通过初审，不能进行终审"
            );
        }

        /*
         * 防止重复终审
         */
        if (
                !Short.valueOf((short) 0)
                        .equals(
                                apply.getFinalStatus()
                        )
        ) {

            return Result.error(
                    "该申请已经完成终审，不能重复审核"
            );
        }

        /*
         * =====================================================
         * 终审驳回
         * =====================================================
         */

        if (
                request.getStatus() == 2
        ) {

            apply.setFinalStatus(
                    (short) 2
            );

            apply.setStatus(
                    (short) 2
            );

            apply.setFinalReviewerId(
                    reviewerId
            );

            apply.setFinalReviewTime(
                    LocalDateTime.now()
            );

            apply.setUpdateTime(
                    LocalDateTime.now()
            );

            scoreApplyMapper.updateById(
                    apply
            );

            return Result.success(
                    null
            );
        }

        /*
         * =====================================================
         * 终审通过
         * =====================================================
         */

        if (
                apply.getApplyScore() == null
        ) {

            return Result.error(
                    "该申请没有档案部认定分值，无法终审"
            );
        }

        if (
                apply.getApplyScore()
                        .compareTo(
                                BigDecimal.ZERO
                        ) <= 0
        ) {

            return Result.error(
                    "档案部认定分值必须大于0"
            );
        }

        /*
         * =====================================================
         * 查询当前学期
         * =====================================================
         */

        LocalDate today =
                LocalDate.now();

        SysSemester semester =
                sysSemesterMapper.selectOne(
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

        if (
                semester == null
        ) {

            return Result.error(
                    "当前没有正在进行的学期，无法生成正式成绩记录"
            );
        }

        /*
         * =====================================================
         * 防止重复生成 ScoreRecord
         * =====================================================
         */

        Long existCount =
                scoreRecordMapper.selectCount(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .eq(
                                        ScoreRecord::getSourceType,
                                        "CERTIFICATE"
                                )
                                .eq(
                                        ScoreRecord::getSourceId,
                                        apply.getId()
                                )
                );

        if (
                existCount != null
                        &&
                        existCount > 0
        ) {

            return Result.error(
                    "该证书已经生成正式成绩记录，不能重复审批"
            );
        }

        /*
         * =====================================================
         * 创建正式成绩记录
         * =====================================================
         */

        ScoreRecord record =
                new ScoreRecord();

        record.setStudentId(
                apply.getStudentId()
        );

        /*
         * 个人证书暂时没有 ruleId
         */
        record.setRuleId(
                apply.getRuleId()
        );

        record.setScore(
                apply.getApplyScore()
        );

        record.setSemesterId(
                semester.getId()
        );

        /*
         * 来源类型
         */
        record.setSourceType(
                "CERTIFICATE"
        );

        /*
         * 来源申请ID
         */
        record.setSourceId(
                apply.getId()
        );

        /*
         * 正常
         */
        record.setStatus(
                (short) 1
        );

        /*
         * 管理员未隐藏
         */
        record.setAdminHidden(
                (short) 0
        );

        record.setCreateTime(
                LocalDateTime.now()
        );

        int insertResult =
                scoreRecordMapper.insert(
                        record
                );

        if (
                insertResult <= 0
        ) {

            return Result.error(
                    "正式成绩生成失败"
            );
        }

        /*
         * =====================================================
         * 更新申请最终状态
         * =====================================================
         */

        apply.setFinalStatus(
                (short) 1
        );

        apply.setFinalReviewerId(
                reviewerId
        );

        apply.setFinalReviewTime(
                LocalDateTime.now()
        );

        /*
         * 1 = 整个证书审核完成
         */
        apply.setStatus(
                (short) 1
        );

        apply.setUpdateTime(
                LocalDateTime.now()
        );

        scoreApplyMapper.updateById(
                apply
        );

        return Result.success(
                null
        );
    }

    /**
     * ========================================================
     * Entity → VO
     * ========================================================
     */
    private ScoreApplyVO convertToVO(
            ScoreApply apply
    ) {

        ScoreApplyVO vo =
                new ScoreApplyVO();

        vo.setId(
                apply.getId()
        );

        vo.setStudentId(
                apply.getStudentId()
        );

        vo.setApplyType(
                apply.getApplyType()
        );

        /*
         * 档案部认定分值
         *
         * 学生提交时为 null
         * 初审通过以后才有值
         */
        vo.setApplyScore(
                apply.getApplyScore()
        );

        vo.setScore(
                apply.getApplyScore()
        );

        /*
         * =====================================================
         * 总状态
         * =====================================================
         */

        vo.setStatus(
                apply.getStatus() != null
                        ? apply.getStatus().intValue()
                        : 0
        );

        /*
         * =====================================================
         * 初审状态
         * =====================================================
         */

        vo.setPreliminaryStatus(
                apply.getPreliminaryStatus() != null
                        ? apply.getPreliminaryStatus().intValue()
                        : 0
        );

        vo.setPreliminaryReviewerId(
                apply.getPreliminaryReviewerId()
        );

        vo.setPreliminaryReviewTime(
                apply.getPreliminaryReviewTime()
        );

        /*
         * =====================================================
         * 终审状态
         * =====================================================
         */

        vo.setFinalStatus(
                apply.getFinalStatus() != null
                        ? apply.getFinalStatus().intValue()
                        : 0
        );

        vo.setFinalReviewerId(
                apply.getFinalReviewerId()
        );

        vo.setFinalReviewTime(
                apply.getFinalReviewTime()
        );

        /*
         * =====================================================
         * 材料
         * =====================================================
         */

        vo.setMaterialFile(
                apply.getMaterialFile()
        );

        /*
         * =====================================================
         * 创建时间
         * =====================================================
         */

        vo.setCreateTime(
                apply.getCreateTime()
        );

        /*
         * =====================================================
         * 学生信息
         * =====================================================
         */

        if (
                apply.getStudentId() != null
        ) {

            SysUser user =
                    sysUserMapper.selectById(
                            apply.getStudentId()
                    );

            if (
                    user != null
            ) {

                /*
                 * 学号
                 */
                vo.setStudentNo(
                        user.getStudentNo()
                );

                /*
                 * 班级
                 */
                vo.setClassName(
                        user.getClassName()
                );

                /*
                 * 学生姓名
                 */
                vo.setStudentName(
                        user.getRealName()
                );
            }
        }

        /*
         * =====================================================
         * 个人证书
         * =====================================================
         */

        if (
                "CERTIFICATE".equals(
                        apply.getApplyType()
                )
        ) {

            parseCertificateDescription(
                    apply.getDescription(),
                    vo
            );

            /*
             * 页面标题
             */
            if (
                    vo.getAwardName() != null
                            &&
                            !vo.getAwardName().isBlank()
            ) {

                vo.setTitle(
                        vo.getAwardName()
                );

            } else {

                vo.setTitle(
                        "个人证书申报"
                );
            }
        }

        /*
         * =====================================================
         * 规则
         * =====================================================
         */

        if (
                apply.getRuleId() != null
        ) {

            ScoreRule rule =
                    scoreRuleMapper.selectById(
                            apply.getRuleId()
                    );

            if (
                    rule != null
            ) {

                vo.setRuleName(
                        rule.getName()
                );
            }
        }

        return vo;
    }

    /**
     * ========================================================
     * 解析个人证书JSON
     * ========================================================
     */
    private void parseCertificateDescription(
            String description,
            ScoreApplyVO vo
    ) {

        if (
                description == null
                        || description.isBlank()
        ) {

            return;
        }

        try {

            JsonNode node =
                    objectMapper.readTree(
                            description
                    );

            vo.setAwardCategory(
                    getText(
                            node,
                            "awardCategory"
                    )
            );

            vo.setAwardName(
                    getText(
                            node,
                            "awardName"
                    )
            );

            vo.setAwardLevel(
                    getText(
                            node,
                            "awardLevel"
                    )
            );

            vo.setAwardGrade(
                    getText(
                            node,
                            "awardGrade"
                    )
            );

            vo.setAwardGradeOther(
                    getText(
                            node,
                            "awardGradeOther"
                    )
            );

            vo.setAwardTime(
                    getText(
                            node,
                            "awardTime"
                    )
            );

            vo.setAwardType(
                    getText(
                            node,
                            "awardType"
                    )
            );

            vo.setHasCertificate(
                    getText(
                            node,
                            "hasCertificate"
                    )
            );

            vo.setCertificateReason(
                    getText(
                            node,
                            "certificateReason"
                    )
            );

            vo.setDescription(
                    getText(
                            node,
                            "description"
                    )
            );

        } catch (
                Exception e
        ) {

            vo.setDescription(
                    description
            );
        }
    }

    /**
     * ========================================================
     * JSON字段读取
     * ========================================================
     */
    private String getText(
            JsonNode node,
            String field
    ) {

        if (
                node == null
                        ||
                        !node.has(field)
                        ||
                        node.get(field).isNull()
        ) {

            return null;
        }

        return node.get(field)
                .asText();
    }

    /**
     * ========================================================
     * 审核请求
     * ========================================================
     */
    public static class AuditRequest {

        private Long id;

        private Integer status;

        private BigDecimal finalScore;

        public Long getId() {

            return id;
        }

        public void setId(
                Long id
        ) {

            this.id = id;
        }

        public Integer getStatus() {

            return status;
        }

        public void setStatus(
                Integer status
        ) {

            this.status = status;
        }

        public BigDecimal getFinalScore() {

            return finalScore;
        }

        public void setFinalScore(
                BigDecimal finalScore
        ) {

            this.finalScore = finalScore;
        }
    }

    /**
     * ========================================================
     * 个人证书存储结构
     * ========================================================
     */
    private static class CertificateDescription {

        public String awardCategory;

        public String awardName;

        public String awardLevel;

        public String awardGrade;

        public String awardGradeOther;

        public String awardTime;

        public String awardType;

        public String hasCertificate;

        public String certificateReason;

        public String description;
    }
}