package com.student.studentscoresystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.entity.ScoreAdminAdjustment;
import com.student.studentscoresystem.entity.ScoreApply;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.mapper.DepartmentScoreApplyMapper;
import com.student.studentscoresystem.mapper.ScoreAdminAdjustmentMapper;
import com.student.studentscoresystem.mapper.ScoreApplyMapper;
import com.student.studentscoresystem.mapper.ScoreRuleMapper;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * =========================================================
 * 成绩记录「评分项目 / 计分项」名称解析
 * =========================================================
 *
 * score_record 表只保存 rule_id、source_type、source_id，
 * 没有直接保存项目名称，所以统一在这里解析：
 *
 * 1. 绑定了规则           -> 规则名称
 * 2. CERTIFICATE / APPLY -> 申报中的获奖名称（可附带获奖级别）
 * 3. DEPARTMENT          -> 部门申报标题
 * 4. ADMIN_ADJUSTMENT    -> 调整原因
 * 5. 都取不到            -> 综合测评项目（兜底）
 *
 * 两种用法：
 *
 * 1. 页面明细（单条、逐条查库）
 *
 *    scoreProjectNameResolver.resolve(record)
 *
 * 2. 导出（批量预取、一次查库，避免 N+1）
 *
 *    PreloadedNames names = scoreProjectNameResolver.preload(records);
 *    names.resolve(record, true)
 *
 * 注意：
 *
 * 个人证书申报按设计不绑定规则（rule_id 为空），
 * 以前这些成绩记录在「评分项目」列会直接显示为空。
 */
@Component
public class ScoreProjectNameResolver {

    /**
     * 来源类型
     */
    public static final String TYPE_CERTIFICATE =
            "CERTIFICATE";

    public static final String TYPE_APPLY =
            "APPLY";

    public static final String TYPE_DEPARTMENT =
            "DEPARTMENT";

    public static final String TYPE_ADMIN_ADJUSTMENT =
            "ADMIN_ADJUSTMENT";

    /**
     * 兜底名称
     */
    public static final String FALLBACK_NAME =
            "综合测评项目";

    private final ScoreRuleMapper scoreRuleMapper;

    private final ScoreApplyMapper scoreApplyMapper;

    private final DepartmentScoreApplyMapper departmentScoreApplyMapper;

    private final ScoreAdminAdjustmentMapper scoreAdminAdjustmentMapper;

    private final ObjectMapper objectMapper;

    public ScoreProjectNameResolver(
            ScoreRuleMapper scoreRuleMapper,
            ScoreApplyMapper scoreApplyMapper,
            DepartmentScoreApplyMapper departmentScoreApplyMapper,
            ScoreAdminAdjustmentMapper scoreAdminAdjustmentMapper,
            ObjectMapper objectMapper
    ) {
        this.scoreRuleMapper = scoreRuleMapper;
        this.scoreApplyMapper = scoreApplyMapper;
        this.departmentScoreApplyMapper = departmentScoreApplyMapper;
        this.scoreAdminAdjustmentMapper = scoreAdminAdjustmentMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * =========================================================
     * 单条解析
     *
     * 页面明细使用，不带获奖级别
     * =========================================================
     */
    public String resolve(
            ScoreRecord record
    ) {

        return resolve(
                record,
                false
        );
    }

    /**
     * =========================================================
     * 单条解析
     *
     * @param withAwardLevel 个人证书是否附带获奖级别，
     *                       例如「全国大学生数学建模竞赛一等奖（国家级）」
     * =========================================================
     */
    public String resolve(
            ScoreRecord record,
            boolean withAwardLevel
    ) {

        if (record == null) {

            return FALLBACK_NAME;
        }

        String ruleName =
                resolveByRule(
                        record.getRuleId()
                );

        if (ruleName != null) {

            return ruleName;
        }

        return resolveSourceName(
                record.getSourceType(),
                record.getSourceId(),
                withAwardLevel
        );
    }

    /**
     * =========================================================
     * 批量预取
     *
     * 一次取出本次用到的规则、申报、调整单，
     * 导出等批量场景使用，避免逐条记录查库。
     * =========================================================
     */
    public PreloadedNames preload(
            Collection<ScoreRecord> records
    ) {

        Map<Long, String> ruleNames =
                new HashMap<>();

        Map<String, String> shortNames =
                new HashMap<>();

        Map<String, String> detailNames =
                new HashMap<>();

        if (records == null || records.isEmpty()) {

            return new PreloadedNames(
                    ruleNames,
                    shortNames,
                    detailNames
            );
        }

        Set<Long> ruleIds =
                new HashSet<>();

        Set<Long> applyIds =
                new HashSet<>();

        Set<Long> departmentIds =
                new HashSet<>();

        Set<Long> adjustmentIds =
                new HashSet<>();

        for (
                ScoreRecord record
                : records
        ) {

            if (record == null) {

                continue;
            }

            if (record.getRuleId() != null) {

                ruleIds.add(
                        record.getRuleId()
                );
            }

            if (record.getSourceType() == null
                    || record.getSourceId() == null) {

                continue;
            }

            String sourceType =
                    record.getSourceType();

            if (TYPE_CERTIFICATE.equals(sourceType)
                    || TYPE_APPLY.equals(sourceType)) {

                applyIds.add(
                        record.getSourceId()
                );

            } else if (TYPE_DEPARTMENT.equals(sourceType)) {

                departmentIds.add(
                        record.getSourceId()
                );

            } else if (TYPE_ADMIN_ADJUSTMENT.equals(sourceType)) {

                adjustmentIds.add(
                        record.getSourceId()
                );
            }
        }

        /*
         * 规则名称
         */
        if (!ruleIds.isEmpty()) {

            for (
                    ScoreRule rule
                    : scoreRuleMapper.selectBatchIds(
                    ruleIds
            )
            ) {

                if (rule == null) {

                    continue;
                }

                String name =
                        blankToNull(
                                rule.getName()
                        );

                if (name != null) {

                    ruleNames.put(
                            rule.getId(),
                            name
                    );
                }
            }
        }

        /*
         * 个人证书 / 个人申报
         *
         * 获奖名称、获奖级别都在 description 的 JSON 里，
         * 所以预取时解析一次，同时准备「短名称」和「带级别名称」。
         */
        if (!applyIds.isEmpty()) {

            for (
                    ScoreApply apply
                    : scoreApplyMapper.selectBatchIds(
                    applyIds
            )
            ) {

                if (apply == null) {

                    continue;
                }

                JsonNode node =
                        readJson(
                                apply.getDescription()
                        );

                String awardName =
                        text(
                                node,
                                "awardName"
                        );

                if (awardName == null) {

                    continue;
                }

                String awardLevel =
                        text(
                                node,
                                "awardLevel"
                        );

                for (
                        String sourceType
                        : new String[]{
                        TYPE_CERTIFICATE,
                        TYPE_APPLY
                }
                ) {

                    String key =
                            sourceKey(
                                    sourceType,
                                    apply.getId()
                            );

                    if (key == null) {

                        continue;
                    }

                    shortNames.put(
                            key,
                            awardName
                    );

                    detailNames.put(
                            key,
                            withLevel(
                                    awardName,
                                    awardLevel
                            )
                    );
                }
            }
        }

        /*
         * 部门申报标题
         */
        if (!departmentIds.isEmpty()) {

            for (
                    DepartmentScoreApply apply
                    : departmentScoreApplyMapper.selectBatchIds(
                    departmentIds
            )
            ) {

                if (apply == null) {

                    continue;
                }

                String title =
                        blankToNull(
                                apply.getTitle()
                        );

                String key =
                        sourceKey(
                                TYPE_DEPARTMENT,
                                apply.getId()
                        );

                if (title != null
                        && key != null) {

                    shortNames.put(
                            key,
                            title
                    );
                }
            }
        }

        /*
         * 管理员调整原因
         */
        if (!adjustmentIds.isEmpty()) {

            for (
                    ScoreAdminAdjustment adjustment
                    : scoreAdminAdjustmentMapper.selectBatchIds(
                    adjustmentIds
            )
            ) {

                if (adjustment == null) {

                    continue;
                }

                String reason =
                        blankToNull(
                                adjustment.getReason()
                        );

                String key =
                        sourceKey(
                                TYPE_ADMIN_ADJUSTMENT,
                                adjustment.getId()
                        );

                if (reason != null
                        && key != null) {

                    shortNames.put(
                            key,
                            reason
                    );
                }
            }
        }

        return new PreloadedNames(
                ruleNames,
                shortNames,
                detailNames
        );
    }

    /**
     * =========================================================
     * 规则名称
     * =========================================================
     */
    private String resolveByRule(
            Long ruleId
    ) {

        if (ruleId == null) {

            return null;
        }

        ScoreRule rule =
                scoreRuleMapper.selectById(
                        ruleId
                );

        if (rule == null) {

            return null;
        }

        return blankToNull(
                rule.getName()
        );
    }

    /**
     * =========================================================
     * 按来源业务解析（单条）
     * =========================================================
     */
    private String resolveSourceName(
            String sourceType,
            Long sourceId,
            boolean withAwardLevel
    ) {

        if (sourceType == null || sourceId == null) {

            return FALLBACK_NAME;
        }

        if (TYPE_CERTIFICATE.equals(sourceType)) {

            return applyName(
                    sourceId,
                    withAwardLevel,
                    "个人证书申报"
            );
        }

        if (TYPE_APPLY.equals(sourceType)) {

            return applyName(
                    sourceId,
                    withAwardLevel,
                    "个人加分申报"
            );
        }

        if (TYPE_DEPARTMENT.equals(sourceType)) {

            return departmentName(
                    sourceId
            );
        }

        if (TYPE_ADMIN_ADJUSTMENT.equals(sourceType)) {

            return adjustmentName(
                    sourceId
            );
        }

        return FALLBACK_NAME;
    }

    /**
     * 个人证书 / 个人申报
     */
    private String applyName(
            Long sourceId,
            boolean withAwardLevel,
            String fallback
    ) {

        ScoreApply apply =
                scoreApplyMapper.selectById(
                        sourceId
                );

        if (apply == null) {

            return fallback;
        }

        JsonNode node =
                readJson(
                        apply.getDescription()
                );

        String awardName =
                text(
                        node,
                        "awardName"
                );

        if (awardName == null) {

            return fallback;
        }

        if (!withAwardLevel) {

            return awardName;
        }

        return withLevel(
                awardName,
                text(
                        node,
                        "awardLevel"
                )
        );
    }

    /**
     * 部门申报标题
     */
    private String departmentName(
            Long sourceId
    ) {

        DepartmentScoreApply apply =
                departmentScoreApplyMapper.selectById(
                        sourceId
                );

        String title =
                apply == null
                        ? null
                        : blankToNull(
                        apply.getTitle()
                );

        return title != null
                ? title
                : "部门加减分申报";
    }

    /**
     * 管理员调整原因
     */
    private String adjustmentName(
            Long sourceId
    ) {

        ScoreAdminAdjustment adjustment =
                scoreAdminAdjustmentMapper.selectById(
                        sourceId
                );

        String reason =
                adjustment == null
                        ? null
                        : blankToNull(
                        adjustment.getReason()
                );

        return reason != null
                ? reason
                : "管理员加减分";
    }

    /**
     * =========================================================
     * 读取申报 description 的 JSON
     * =========================================================
     */
    private JsonNode readJson(
            String json
    ) {

        if (json == null || json.isBlank()) {

            return null;
        }

        try {

            return objectMapper.readTree(
                    json
            );

        } catch (Exception e) {

            return null;
        }
    }

    /**
     * 取 JSON 文本字段，取不到返回 null
     */
    private static String text(
            JsonNode node,
            String field
    ) {

        if (node == null) {

            return null;
        }

        JsonNode value =
                node.get(
                        field
                );

        if (value == null || value.isNull()) {

            return null;
        }

        return blankToNull(
                value.asText()
        );
    }

    /**
     * 名称 + （获奖级别）
     */
    private static String withLevel(
            String name,
            String level
    ) {

        if (level == null) {

            return name;
        }

        return name + "（" + level + "）";
    }

    /**
     * 预取缓存 key
     */
    private static String sourceKey(
            String sourceType,
            Long sourceId
    ) {

        if (sourceType == null || sourceId == null) {

            return null;
        }

        return sourceType + ":" + sourceId;
    }

    /**
     * 取不到名称时的兜底
     */
    private static String defaultName(
            String sourceType
    ) {

        if (TYPE_CERTIFICATE.equals(sourceType)) {

            return "个人证书申报";
        }

        if (TYPE_APPLY.equals(sourceType)) {

            return "个人加分申报";
        }

        if (TYPE_DEPARTMENT.equals(sourceType)) {

            return "部门加减分申报";
        }

        if (TYPE_ADMIN_ADJUSTMENT.equals(sourceType)) {

            return "管理员加减分";
        }

        return FALLBACK_NAME;
    }

    /**
     * =========================================================
     * 空字符串转 null
     * =========================================================
     */
    private static String blankToNull(
            String value
    ) {

        if (value == null || value.isBlank()) {

            return null;
        }

        return value.trim();
    }

    /**
     * =========================================================
     * 批量预取结果
     *
     * 由 {@link #preload(Collection)} 生成，
     * 与单条解析的结果保持一致。
     * =========================================================
     */
    public static class PreloadedNames {

        private final Map<Long, String> ruleNames;

        private final Map<String, String> shortNames;

        private final Map<String, String> detailNames;

        PreloadedNames(
                Map<Long, String> ruleNames,
                Map<String, String> shortNames,
                Map<String, String> detailNames
        ) {
            this.ruleNames = ruleNames;
            this.shortNames = shortNames;
            this.detailNames = detailNames;
        }

        public String resolve(
                ScoreRecord record
        ) {

            return resolve(
                    record,
                    false
            );
        }

        /**
         * @param withAwardLevel 个人证书是否附带获奖级别
         */
        public String resolve(
                ScoreRecord record,
                boolean withAwardLevel
        ) {

            if (record == null) {

                return FALLBACK_NAME;
            }

            if (record.getRuleId() != null) {

                String ruleName =
                        ruleNames.get(
                                record.getRuleId()
                        );

                if (ruleName != null) {

                    return ruleName;
                }
            }

            String key =
                    sourceKey(
                            record.getSourceType(),
                            record.getSourceId()
                    );

            if (key != null) {

                Map<String, String> names =
                        withAwardLevel
                                ? detailNames
                                : shortNames;

                String name =
                        names.get(
                                key
                        );

                if (name != null) {

                    return name;
                }
            }

            return defaultName(
                    record.getSourceType()
            );
        }
    }
}
