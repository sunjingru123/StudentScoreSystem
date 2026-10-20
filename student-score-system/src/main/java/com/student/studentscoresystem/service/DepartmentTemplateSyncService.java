package com.student.studentscoresystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.DepartmentScoreTemplate;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.mapper.DepartmentScoreTemplateMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * =========================================================
 * 评分规则 -> 部门加减分模板 同步
 *
 * 管理员在「评分项目管理」维护的评分规则（score_rule），
 * 会自动同步成对应部门的加减分模板（department_score_template），
 * 这样学生端「部门学生加减分申报」的下拉框
 * 就能直接读取到管理员维护的固定评分项目。
 *
 * 身份键：
 *
 * 规则与模板统一按
 * 「部门 + 名称 + 加分/减分类型 + 分值」一一对应，
 * 与 sql/05_init_fixed_department_templates.sql 中的初始化规则一致。
 *
 * 因此同一部门下允许存在同名但类型/分值不同的规则，
 * 例如「教室卫生检查：卫生优良」可以同时有 +0.5 / +1 / +1.5。
 * =========================================================
 */
@Service
public class DepartmentTemplateSyncService {

    private final DepartmentScoreTemplateMapper templateMapper;

    public DepartmentTemplateSyncService(
            DepartmentScoreTemplateMapper templateMapper) {

        this.templateMapper = templateMapper;
    }

    /**
     * =========================================================
     * 新增 / 修改规则后同步模板
     *
     * 同部门、同名称、同类型、同分值的模板已存在则更新描述与状态，
     * 否则新增；同名但分值/类型不同的规则会各自拥有独立模板。
     * =========================================================
     */
    public void sync(ScoreRule rule) {

        if (!isSyncable(rule)) {

            return;
        }

        LocalDateTime now = LocalDateTime.now();

        DepartmentScoreTemplate template =
                findTemplate(
                        rule.getDepartmentId(),
                        rule.getName(),
                        rule.getScoreType(),
                        rule.getScore()
                );

        if (template == null) {

            template = new DepartmentScoreTemplate();

            template.setDepartmentId(rule.getDepartmentId());
            template.setName(rule.getName());
            template.setDescription(rule.getDescription());
            template.setScoreType(rule.getScoreType());
            template.setScore(normalizeScore(rule.getScore()));
            template.setStatus(
                    rule.getStatus() == null
                            ? (short) 1
                            : rule.getStatus()
            );
            template.setCreateTime(now);
            template.setUpdateTime(now);

            templateMapper.insert(template);

            return;
        }

        /*
         * 部门 / 名称 / 类型 / 分值 是身份键，
         * 命中时只同步描述与状态。
         */
        template.setDescription(rule.getDescription());

        if (rule.getStatus() != null) {
            template.setStatus(rule.getStatus());
        }

        template.setUpdateTime(now);

        templateMapper.updateById(template);
    }

    /**
     * =========================================================
     * 修改规则时同步模板
     *
     * 规则的部门 / 名称 / 类型 / 分值任一发生变化时，
     * 把旧模板原地更新为新身份（保留模板主键，
     * 因为历史申报通过 department_score_apply.template_id 引用它），
     * 避免学生端残留旧项目或丢失去重依据。
     * =========================================================
     */
    public void syncRename(
            Long oldDepartmentId,
            String oldName,
            Short oldScoreType,
            BigDecimal oldScore,
            ScoreRule rule
    ) {

        if (!isSyncable(rule)) {

            return;
        }

        if (oldDepartmentId == null
                || oldName == null) {

            sync(rule);

            return;
        }

        boolean identityChanged =
                !Objects.equals(
                        oldDepartmentId,
                        rule.getDepartmentId()
                )
                        || !Objects.equals(
                        oldName,
                        rule.getName()
                )
                        || !Objects.equals(
                        oldScoreType,
                        rule.getScoreType()
                )
                        || !sameScore(
                        oldScore,
                        rule.getScore()
                );

        if (!identityChanged) {

            sync(rule);

            return;
        }

        DepartmentScoreTemplate oldTemplate =
                findTemplate(
                        oldDepartmentId,
                        oldName,
                        oldScoreType,
                        oldScore
                );

        /*
         * 旧模板不存在（例如历史数据未同步过），
         * 按新身份直接同步即可。
         */
        if (oldTemplate == null) {

            sync(rule);

            return;
        }

        oldTemplate.setDepartmentId(rule.getDepartmentId());
        oldTemplate.setName(rule.getName());
        oldTemplate.setScoreType(rule.getScoreType());
        oldTemplate.setScore(normalizeScore(rule.getScore()));
        oldTemplate.setDescription(rule.getDescription());

        if (rule.getStatus() != null) {
            oldTemplate.setStatus(rule.getStatus());
        }

        oldTemplate.setUpdateTime(LocalDateTime.now());

        templateMapper.updateById(oldTemplate);
    }

    /**
     * =========================================================
     * 删除规则时同步删除对应的部门模板
     *
     * 只删除身份完全一致（部门 + 名称 + 类型 + 分值）的模板，
     * 不会连带删除同部门同名的其它分值模板。
     * =========================================================
     */
    public void remove(ScoreRule rule) {

        if (rule == null
                || rule.getDepartmentId() == null
                || rule.getName() == null) {

            return;
        }

        List<DepartmentScoreTemplate> templates =
                findTemplates(
                        rule.getDepartmentId(),
                        rule.getName()
                );

        for (DepartmentScoreTemplate template : templates) {

            if (!sameIdentity(
                    template,
                    rule.getScoreType(),
                    rule.getScore()
            )) {

                continue;
            }

            templateMapper.deleteById(template.getId());
        }
    }

    /**
     * =========================================================
     * 按身份键查询模板
     * =========================================================
     */
    private DepartmentScoreTemplate findTemplate(
            Long departmentId,
            String name,
            Short scoreType,
            BigDecimal score
    ) {

        for (DepartmentScoreTemplate template :
                findTemplates(departmentId, name)) {

            if (sameIdentity(template, scoreType, score)) {

                return template;
            }
        }

        return null;
    }

    /**
     * =========================================================
     * 查询同部门同名称的全部模板
     *
     * 同一名称可能配置了多个分值，必须全部取回后再精确比对身份。
     * =========================================================
     */
    private List<DepartmentScoreTemplate> findTemplates(
            Long departmentId,
            String name
    ) {

        if (departmentId == null
                || name == null) {

            return Collections.emptyList();
        }

        List<DepartmentScoreTemplate> templates =
                templateMapper.selectList(
                        new LambdaQueryWrapper<DepartmentScoreTemplate>()
                                .eq(
                                        DepartmentScoreTemplate::getDepartmentId,
                                        departmentId
                                )
                                .eq(
                                        DepartmentScoreTemplate::getName,
                                        name
                                )
                                .orderByAsc(
                                        DepartmentScoreTemplate::getId
                                )
                );

        if (templates == null) {

            return Collections.emptyList();
        }

        return new ArrayList<>(templates);
    }

    /**
     * =========================================================
     * 判断模板是否与给定身份一致
     *
     * 历史数据可能缺少类型 / 分值，此时退化为只比较已有字段，
     * 保证历史规则仍能同步。
     * =========================================================
     */
    private boolean sameIdentity(
            DepartmentScoreTemplate template,
            Short scoreType,
            BigDecimal score
    ) {

        if (template == null) {

            return false;
        }

        if (scoreType != null
                && !scoreType.equals(template.getScoreType())) {

            return false;
        }

        if (score != null
                && !sameScore(score, template.getScore())) {

            return false;
        }

        return true;
    }

    /**
     * =========================================================
     * 同步所需字段是否完整
     * =========================================================
     */
    private boolean isSyncable(ScoreRule rule) {

        return rule != null
                && rule.getDepartmentId() != null
                && rule.getName() != null
                && rule.getScore() != null
                && rule.getScoreType() != null;
    }

    /**
     * =========================================================
     * 分值归一化，与 score_rule / department_score_template
     * 的 NUMERIC(10,2) 列精度保持一致
     * =========================================================
     */
    private BigDecimal normalizeScore(BigDecimal score) {

        if (score == null) {

            return null;
        }

        return score.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * =========================================================
     * 分值比较（按数值比较，忽略 1 与 1.00 的精度差异）
     * =========================================================
     */
    private boolean sameScore(
            BigDecimal left,
            BigDecimal right
    ) {

        if (left == null || right == null) {

            return false;
        }

        return left.compareTo(right) == 0;
    }
}
