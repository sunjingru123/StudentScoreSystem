package com.student.studentscoresystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.mapper.ScoreRuleMapper;
import com.student.studentscoresystem.vo.ArchiveScoreOptionsVO;
import com.student.studentscoresystem.vo.BatchAuditResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * =========================================================
 * 部门加减分申报「档案部审核」环节
 *
 * 审核链路（四段）：
 *   干事 / 副部长 / 部长 提交
 *     -> 部门副部长 / 部长 初审（status 0 -> 1 / 2）
 *     -> 档案部副部长 / 部长 审核（archiveStatus 0 -> 1 / 2）
 *     -> 辅导员 终审（finalStatus 0 -> 1 / 2，通过后才生成正式成绩）
 *
 * 设计要点：
 *
 * 1. 写操作全部下沉到本 Service，并带
 *    {@code @Transactional(rollbackFor = Exception.class)} 边界，
 *    Controller 只负责参数 / 权限的最外层校验与响应封装。
 *
 * 2. 通过 / 驳回都只改动 department_score_apply 的档案部审核字段，
 *    不会在档案部环节生成成绩；成绩生成点仍在
 *    {@link DepartmentScoreFinalizeService#finalizeApproval}（辅导员终审）。
 *
 * 3. 审核人必须是「档案部」的部长 / 副部长，
 *    且不能审核自己提交的申报，与部门初审保持一致。
 *
 * 4. 档案部审核时可以调整申报分值，但调整范围受正式规则约束：
 *    {@link DepartmentScoreFinalizeService} 终审时要求
 *    「部门 + 名称 + 分值 + 类型」能匹配到一条正式 score_rule，
 *    否则拒绝生成成绩。因此分值调整必须限制在该项目的正式规则分值集合内，
 *    避免把申报改成分值后卡死在终审环节。
 * =========================================================
 */
@Service
public class DepartmentScoreArchiveAuditService {

    /**
     * 一键通过时统一写入的审核意见
     */
    private static final String ARCHIVE_BATCH_PASS_REMARK =
            "一键审批通过";

    /**
     * 单个申报的分值上限，防止误填异常大值
     */
    private static final BigDecimal MAX_SCORE =
            new BigDecimal("100");

    /**
     * 分值允许的小数位
     */
    private static final int MAX_SCORE_SCALE = 2;

    private final IDepartmentScoreApplyService applyService;

    private final ScoreManagerPermissionService scoreManagerPermissionService;

    private final ScoreRuleMapper scoreRuleMapper;


    public DepartmentScoreArchiveAuditService(
            IDepartmentScoreApplyService applyService,
            ScoreManagerPermissionService scoreManagerPermissionService,
            ScoreRuleMapper scoreRuleMapper) {

        this.applyService = applyService;

        this.scoreManagerPermissionService = scoreManagerPermissionService;

        this.scoreRuleMapper = scoreRuleMapper;
    }


    /**
     * =========================================================
     * 单条档案部审核
     *
     * @param applyId    申报记录 ID
     * @param reviewerId 当前登录用户
     * @param status     1 = 通过，2 = 驳回
     * @param remark     审核意见
     * @return 错误信息；返回 null 表示审核成功
     * =========================================================
     */
    @Transactional(rollbackFor = Exception.class)
    public String archiveAudit(
            Long applyId,
            Long reviewerId,
            Short status,
            String remark) {

        if (!scoreManagerPermissionService.isArchiveLeader(reviewerId)) {

            return "你不是档案部的部长或副部长，没有审核权限";
        }


        if (applyId == null) {

            return "申报记录ID不能为空";
        }


        if (status == null
                || (status != 1 && status != 2)) {

            return "审核状态无效";
        }


        DepartmentScoreApply apply =
                applyService.getById(applyId);


        if (apply == null) {

            return "申报记录不存在";
        }


        String editableError =
                checkEditable(apply, reviewerId);


        if (editableError != null) {

            return editableError;
        }


        LocalDateTime now =
                LocalDateTime.now();


        apply.setArchiveStatus(
                status
        );


        apply.setArchiveReviewerId(
                reviewerId
        );


        apply.setArchiveReviewRemark(
                remark
        );


        apply.setArchiveReviewTime(
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

            return "档案部审核处理失败";
        }


        return null;
    }


    /**
     * =========================================================
     * 档案部审核时调整申报分值
     *
     * 只修改 department_score_apply.score，
     * 不改变加减分类型（score_type）、审核状态与成绩生成结果；
     * 分值是否合法由 {@link #scoreOptions} 暴露给前端，
     * 后端在这里再做一次强校验。
     *
     * @param applyId    申报记录 ID
     * @param reviewerId 当前登录用户
     * @param newScore   调整后的分值（正数）
     * @return 错误信息；返回 null 表示调整成功
     * =========================================================
     */
    @Transactional(rollbackFor = Exception.class)
    public String updateScore(
            Long applyId,
            Long reviewerId,
            BigDecimal newScore) {

        if (!scoreManagerPermissionService.isArchiveLeader(reviewerId)) {

            return "你不是档案部的部长或副部长，没有审核权限";
        }


        if (applyId == null) {

            return "申报记录ID不能为空";
        }


        DepartmentScoreApply apply =
                applyService.getById(applyId);


        if (apply == null) {

            return "申报记录不存在";
        }


        String editableError =
                checkEditable(apply, reviewerId);


        if (editableError != null) {

            return editableError;
        }


        if (newScore == null) {

            return "请填写调整后的分值";
        }


        if (newScore.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            return "分值必须大于 0";
        }


        if (newScore.stripTrailingZeros().scale()
                > MAX_SCORE_SCALE) {

            return "分值最多保留两位小数";
        }


        if (newScore.compareTo(
                MAX_SCORE
        ) > 0) {

            return "分值不能超过 100";
        }


        /*
         * 有正式规则时必须落在正式规则分值集合内，
         * 否则辅导员终审无法生成成绩（见 DepartmentScoreFinalizeService）。
         */
        List<ScoreRule> candidates =
                listFormalRules(apply);


        if (!candidates.isEmpty()) {

            List<BigDecimal> allowedScores =
                    allowedScores(candidates, apply);


            if (allowedScores.isEmpty()) {

                return "该项目在当前部门的正式评分规则与申报类型不匹配，" +
                        "无法调整分值，请联系管理员";
            }


            if (!containsScore(allowedScores, newScore)) {

                return "调整后的分值必须与该项目正式评分规则一致，可选分值：" +
                        formatScores(allowedScores);
            }
        }


        apply.setScore(
                newScore
        );

        apply.setUpdateTime(
                LocalDateTime.now()
        );


        if (!applyService.updateById(
                apply
        )) {

            return "分值调整失败";
        }


        return null;
    }


    /**
     * =========================================================
     * 查询某条申报允许调整的分值范围
     *
     * restricted = true  该项目存在正式规则，只能从 allowedScores 中选择
     * restricted = false 该项目没有正式规则，可自由填写正数
     * =========================================================
     */
    public ArchiveScoreOptionsVO scoreOptions(
            Long applyId) {

        ArchiveScoreOptionsVO vo =
                new ArchiveScoreOptionsVO();


        if (applyId == null) {

            return vo;
        }


        DepartmentScoreApply apply =
                applyService.getById(applyId);


        if (apply == null) {

            return vo;
        }


        vo.setScoreType(
                apply.getScoreType()
        );

        vo.setCurrentScore(
                apply.getScore()
        );


        List<ScoreRule> candidates =
                listFormalRules(apply);


        if (candidates.isEmpty()) {

            /*
             * 部门自建活动，没有正式规则，分值以申报单为准
             */
            vo.setRestricted(
                    false
            );

            return vo;
        }


        vo.setRestricted(
                true
        );

        vo.setAllowedScores(
                allowedScores(candidates, apply)
        );


        return vo;
    }


    /**
     * =========================================================
     * 一键通过全部待档案部审核的申报
     *
     * 逐条复用 {@link #archiveAudit}，
     * 单条失败只记录原因，不影响其它记录。
     * =========================================================
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchAuditResultVO batchPass(
            Long reviewerId) {

        BatchAuditResultVO result =
                new BatchAuditResultVO();


        if (!scoreManagerPermissionService.isArchiveLeader(reviewerId)) {

            result.addFailure(
                    "你不是档案部的部长或副部长，没有审核权限"
            );

            return result;
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
                                 * 自己的申报不能自己审批
                                 */
                                .ne(
                                        DepartmentScoreApply::getApplicantId,
                                        reviewerId
                                )
                                .orderByAsc(
                                        DepartmentScoreApply::getCreateTime
                                )
                );


        if (list == null || list.isEmpty()) {

            return result;
        }


        result.setTotal(
                list.size()
        );


        for (DepartmentScoreApply apply : list) {

            if (apply == null || apply.getId() == null) {

                result.addFailure(
                        "存在无效的申报记录（ID为空）"
                );

                continue;
            }


            String message =
                    archiveAudit(
                            apply.getId(),
                            reviewerId,
                            (short) 1,
                            ARCHIVE_BATCH_PASS_REMARK
                    );


            if (message == null) {

                result.addSuccess();

            } else {

                result.addFailure(
                        message
                );
            }
        }


        return result;
    }


    /**
     * =========================================================
     * 审核前置条件：只有「部门初审通过 + 档案部尚未审核」
     * 且不是自己提交的申报，才允许审核或调整分值
     *
     * @return 错误信息；返回 null 表示可以操作
     * =========================================================
     */
    private String checkEditable(
            DepartmentScoreApply apply,
            Long reviewerId) {

        /*
         * 只有通过部门初审的申报才进入档案部审核
         */
        if (!Short.valueOf((short) 1)
                .equals(apply.getStatus())) {

            return "该申报尚未通过部门审核";
        }


        if (!Short.valueOf((short) 0)
                .equals(apply.getArchiveStatus())) {

            return "该申报已经完成档案部审核";
        }


        /*
         * 不能审核自己提交的申报
         */
        if (reviewerId != null
                && reviewerId.equals(apply.getApplicantId())) {

            return "不能审核自己提交的申报";
        }


        return null;
    }


    /**
     * =========================================================
     * 查询该申报对应的正式加减分规则
     *
     * 与 {@link DepartmentScoreFinalizeService} 完全同口径：
     * 部门 + 规则名称（申报标题）+ 启用状态。
     * 同一部门可能存在同名但分值不同的规则，因此返回全部候选。
     * =========================================================
     */
    private List<ScoreRule> listFormalRules(
            DepartmentScoreApply apply) {

        if (apply == null
                || apply.getDepartmentId() == null
                || apply.getTitle() == null
                || apply.getTitle().trim().isEmpty()) {

            return new ArrayList<>();
        }


        List<ScoreRule> candidates =
                scoreRuleMapper.selectList(
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
                                .orderByAsc(
                                        ScoreRule::getId
                                )
                );


        return candidates == null
                ? new ArrayList<>()
                : candidates;
    }


    /**
     * =========================================================
     * 从候选规则中提取允许的分值集合
     *
     * 过滤掉分值缺失 / 非正数的脏配置，
     * 并要求加减分类型与申报一致（历史数据类型为空时向后兼容）。
     * =========================================================
     */
    private List<BigDecimal> allowedScores(
            List<ScoreRule> candidates,
            DepartmentScoreApply apply) {

        List<BigDecimal> scores =
                new ArrayList<>();


        if (candidates == null) {

            return scores;
        }


        for (ScoreRule rule : candidates) {

            if (rule == null
                    || rule.getScore() == null
                    || rule.getScore().compareTo(
                    BigDecimal.ZERO
            ) <= 0) {

                continue;
            }


            if (!scoreTypeCompatible(rule, apply)) {

                continue;
            }


            if (!containsScore(scores, rule.getScore())) {

                scores.add(
                        rule.getScore()
                );
            }
        }


        scores.sort(
                Comparator.naturalOrder()
        );


        return scores;
    }


    /**
     * =========================================================
     * 规则与申报的加减分类型是否兼容
     *
     * 任一侧未标注类型时只比较分值，保持对历史数据的兼容。
     * =========================================================
     */
    private boolean scoreTypeCompatible(
            ScoreRule rule,
            DepartmentScoreApply apply) {

        if (apply == null
                || apply.getScoreType() == null
                || rule.getScoreType() == null) {

            return true;
        }


        return apply.getScoreType().equals(
                rule.getScoreType()
        );
    }


    /**
     * 分值集合是否包含目标值（按数值比较，忽略精度差异）
     */
    private boolean containsScore(
            List<BigDecimal> scores,
            BigDecimal target) {

        if (scores == null
                || target == null) {

            return false;
        }


        for (BigDecimal score : scores) {

            if (score != null
                    && score.compareTo(target) == 0) {

                return true;
            }
        }


        return false;
    }


    /**
     * 分值集合格式化为业务提示文案
     */
    private String formatScores(
            List<BigDecimal> scores) {

        StringBuilder builder =
                new StringBuilder();


        for (BigDecimal score : scores) {

            if (builder.length() > 0) {

                builder.append(" / ");
            }


            builder.append(
                    score.stripTrailingZeros().toPlainString()
            );
        }


        return builder.toString();
    }
}
