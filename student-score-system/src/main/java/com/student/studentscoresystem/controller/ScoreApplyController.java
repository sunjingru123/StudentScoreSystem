package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.ScoreApply;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.ScoreApplyMapper;
import com.student.studentscoresystem.mapper.ScoreRuleMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.vo.ScoreApplyVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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

    public ScoreApplyController(
            ScoreApplyMapper scoreApplyMapper,
            SysUserMapper sysUserMapper,
            ScoreRuleMapper scoreRuleMapper,
            ObjectMapper objectMapper
    ) {

        this.scoreApplyMapper =
                scoreApplyMapper;

        this.sysUserMapper =
                sysUserMapper;

        this.scoreRuleMapper =
                scoreRuleMapper;

        this.objectMapper =
                objectMapper;
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

        Object userIdObj =
                request.getAttribute("userId");

        if (userIdObj == null) {

            return Result.fail(
                    "请先登录"
            );
        }

        Long currentStudentId;

        try {

            currentStudentId =
                    Long.valueOf(
                            userIdObj.toString()
                    );

        } catch (Exception e) {

            return Result.fail(
                    "登录用户信息无效"
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
     * 学生只允许填写个人证书相关信息。
     *
     * 不允许填写：
     *
     * departmentId
     * departmentName
     * activityId
     * ruleId
     * applyScore
     * score
     * studentId
     * applyType
     *
     * studentId 和 applyType 由后端自动设置。
     *
     * 最终加分由档案部审核确定。
     */
    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody JsonNode data,
            HttpServletRequest request
    ) {

        Object userIdObj =
                request.getAttribute("userId");

        if (userIdObj == null) {

            return Result.fail(
                    "请先登录"
            );
        }

        Long currentStudentId;

        try {

            currentStudentId =
                    Long.valueOf(
                            userIdObj.toString()
                    );

        } catch (Exception e) {

            return Result.fail(
                    "登录用户信息无效"
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
         * 获奖类别校验
         * =====================================================
         */

        if (
                awardCategory == null ||
                        awardCategory.isBlank()
        ) {

            return Result.error(
                    "请选择获奖类别"
            );
        }

        if (
                !(
                        "A".equals(awardCategory) ||
                                "B".equals(awardCategory) ||
                                "C".equals(awardCategory) ||
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
                awardName == null ||
                        awardName.isBlank()
        ) {

            return Result.error(
                    "请输入获奖名称"
            );
        }

        /*
         * =====================================================
         * 获奖级别
         *
         * 与 Apply.vue 保持一致：
         *
         * 国家级
         * 省级
         * 校级
         * 院级
         * =====================================================
         */

        if (
                awardLevel == null ||
                        awardLevel.isBlank()
        ) {

            return Result.error(
                    "请选择获奖级别"
            );
        }

        if (
                !(
                        "国家级".equals(awardLevel) ||
                                "省级".equals(awardLevel) ||
                                "校级".equals(awardLevel) ||
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
                awardGrade == null ||
                        awardGrade.isBlank()
        ) {

            return Result.error(
                    "请选择获奖等级"
            );
        }

        if (
                !(
                        "一等奖".equals(awardGrade) ||
                                "二等奖".equals(awardGrade) ||
                                "三等奖".equals(awardGrade) ||
                                "优秀奖".equals(awardGrade) ||
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
                    awardGradeOther == null ||
                            awardGradeOther.isBlank()
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
                awardTime == null ||
                        awardTime.isBlank()
        ) {

            return Result.error(
                    "请选择获奖时间"
            );
        }

        /*
         * =====================================================
         * 奖项类型
         *
         * 与 Apply.vue 保持一致：
         *
         * 个人奖
         * 团体奖
         * =====================================================
         */

        if (
                awardType == null ||
                        awardType.isBlank()
        ) {

            return Result.error(
                    "请选择奖项类型"
            );
        }

        if (
                !(
                        "个人奖".equals(awardType) ||
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
                hasCertificate == null ||
                        hasCertificate.isBlank()
        ) {

            return Result.error(
                    "请选择是否有获奖凭证"
            );
        }

        if (
                !(
                        "YES".equals(hasCertificate) ||
                                "NO".equals(hasCertificate)
                )
        ) {

            return Result.error(
                    "获奖凭证选项无效"
            );
        }

        /*
         * =====================================================
         * 有凭证
         * =====================================================
         */

        if (
                "YES".equals(hasCertificate)
        ) {

            if (
                    materialFile == null ||
                            materialFile.isBlank()
            ) {

                return Result.error(
                        "有获奖凭证时必须上传凭证材料"
                );
            }
        }

        /*
         * =====================================================
         * 无凭证
         * =====================================================
         */

        if (
                "NO".equals(hasCertificate)
        ) {

            if (
                    certificateReason == null ||
                            certificateReason.isBlank()
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

        /*
         * 当前登录学生
         */
        apply.setStudentId(
                currentStudentId
        );

        /*
         * 明确设置为个人证书
         */
        apply.setApplyType(
                "CERTIFICATE"
        );

        /*
         * 学生不能填写最终分值
         */
        apply.setApplyScore(
                null
        );

        /*
         * 个人证书不属于活动申报
         */
        apply.setActivityId(
                null
        );

        /*
         * 个人证书暂时不绑定规则
         */
        apply.setRuleId(
                null
        );

        /*
         * 有凭证才保存文件
         */
        apply.setMaterialFile(
                "YES".equals(hasCertificate)
                        ? materialFile
                        : null
        );

        /*
         * 证书详细信息
         */
        apply.setDescription(
                descriptionJson
        );

        /*
         * 0 = 待审核
         */
        apply.setStatus(
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

            if (
                    result > 0
            ) {

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
     * 档案部查询个人证书申请
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

        /*
         * =====================================================
         * 根据学号 / 班级筛选学生
         * =====================================================
         */

        List<Long> studentIds =
                null;

        if (

                (
                        studentNo != null &&
                                !studentNo.isBlank()
                )

                        ||

                        (
                                className != null &&
                                        !className.isBlank()
                        )

        ) {

            LambdaQueryWrapper<SysUser>
                    userWrapper =
                    new LambdaQueryWrapper<>();

            if (
                    studentNo != null &&
                            !studentNo.isBlank()
            ) {

                userWrapper.like(
                        SysUser::getStudentNo,
                        studentNo
                );
            }

            if (
                    className != null &&
                            !className.isBlank()
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

            /*
             * 搜不到学生
             */

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
         * 查询个人证书
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

        /*
         * =====================================================
         * 转换 VO
         * =====================================================
         */

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
             * 获奖类别筛选
             */

            if (

                    awardCategory != null &&

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
             * 是否有凭证筛选
             */

            if (

                    hasCertificate != null &&

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
     * 查询待审核个人证书
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
            String hasCertificate
    ) {

        return list(
                pageNum,
                pageSize,
                studentNo,
                className,
                awardCategory,
                hasCertificate,
                0
        );
    }

    /**
     * ========================================================
     * 档案部审核个人证书
     * ========================================================
     *
     * status：
     *
     * 1 = 审核通过
     * 2 = 审核驳回
     *
     * 审核通过：
     * 必须填写最终加分。
     *
     * 审核驳回：
     * 最终加分为空。
     */
    @PostMapping("/audit")
    public Result<Void> audit(
            @RequestBody AuditRequest request,
            HttpServletRequest httpRequest
    ) {

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

                request.getStatus() != 1 &&

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

        /*
         * 必须是个人证书
         */

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
         * =====================================================
         * 审核通过
         * =====================================================
         */

        if (
                request.getStatus() == 1
        ) {

            if (
                    request.getFinalScore() == null
            ) {

                return Result.error(
                        "审核通过时必须填写最终加分"
                );
            }

            if (

                    request.getFinalScore()
                            .compareTo(
                                    BigDecimal.ZERO
                            ) < 0

            ) {

                return Result.error(
                        "最终加分不能小于0"
                );
            }

            /*
             * 最终加分由档案部确定
             */

            apply.setApplyScore(
                    request.getFinalScore()
            );
        }

        /*
         * =====================================================
         * 审核驳回
         * =====================================================
         */

        if (
                request.getStatus() == 2
        ) {

            apply.setApplyScore(
                    null
            );
        }

        /*
         * =====================================================
         * 更新状态
         * =====================================================
         */

        apply.setStatus(
                request.getStatus()
                        .shortValue()
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
                    "审核操作失败"
            );
        }

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

        vo.setApplyScore(
                apply.getApplyScore()
        );

        /*
         * score 与申请最终分值保持一致
         */

        vo.setScore(
                apply.getApplyScore()
        );

        /*
         * 审核状态
         */

        vo.setStatus(
                apply.getStatus() != null
                        ? apply.getStatus().intValue()
                        : 0
        );

        /*
         * 材料
         */

        vo.setMaterialFile(
                apply.getMaterialFile()
        );

        /*
         * 创建时间
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

                vo.setStudentNo(
                        user.getStudentNo()
                );

                vo.setClassName(
                        user.getClassName()
                );

                /*
                 * 如果 ScoreApplyVO 中存在 realName 字段，
                 * 可以使用：
                 *
                 * vo.setRealName(user.getRealName());
                 *
                 * 当前不强制设置，避免 VO 没有该字段再次报错。
                 *
                 * 注意：
                 * SysUser 没有 getName()。
                 * 正确的是 getRealName()。
                 */
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
             * 个人证书属于加分申请。
             *
             * 注意：
             * 这里不再调用：
             *
             * vo.setScoreType(...)
             *
             * 因为当前 ScoreApplyVO 没有这个方法。
             */

            /*
             * 项目名称
             */

            if (

                    vo.getAwardName() != null &&

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
         * 规则名称
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
     * 解析个人证书 JSON
     * ========================================================
     */
    private void parseCertificateDescription(
            String description,
            ScoreApplyVO vo
    ) {

        if (

                description == null ||

                        description.isBlank()

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
     * JSON 字段读取
     * ========================================================
     */
    private String getText(
            JsonNode node,
            String field
    ) {

        if (

                node == null ||

                        !node.has(field) ||

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

        /*
         * YES = 有凭证
         * NO = 无凭证
         */
        public String hasCertificate;

        /*
         * 没有凭证时填写的原因
         */
        public String certificateReason;

        /*
         * 学生补充说明
         */
        public String description;
    }
}