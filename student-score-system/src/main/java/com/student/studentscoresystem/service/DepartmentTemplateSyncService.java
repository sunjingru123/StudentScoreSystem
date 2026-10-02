package com.student.studentscoresystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.DepartmentScoreTemplate;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.mapper.DepartmentScoreTemplateMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
 * 说明：
 *
 * 规则与模板按「部门 + 名称」一一对应，
 * 与终审时匹配正式规则的方式保持一致。
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
     * 同部门同名模板已存在则更新，否则新增。
     * =========================================================
     */
    public void sync(ScoreRule rule) {

        if (rule == null
                || rule.getDepartmentId() == null
                || rule.getName() == null
                || rule.getScore() == null
                || rule.getScoreType() == null) {

            return;
        }

        LocalDateTime now = LocalDateTime.now();

        DepartmentScoreTemplate template =
                templateMapper.selectOne(
                        new LambdaQueryWrapper<DepartmentScoreTemplate>()
                                .eq(
                                        DepartmentScoreTemplate::getDepartmentId,
                                        rule.getDepartmentId()
                                )
                                .eq(
                                        DepartmentScoreTemplate::getName,
                                        rule.getName()
                                )
                                .last("LIMIT 1")
                );

        if (template == null) {

            template = new DepartmentScoreTemplate();

            template.setDepartmentId(rule.getDepartmentId());
            template.setName(rule.getName());
            template.setDescription(rule.getDescription());
            template.setScoreType(rule.getScoreType());
            template.setScore(rule.getScore());
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

        template.setDescription(rule.getDescription());
        template.setScoreType(rule.getScoreType());
        template.setScore(rule.getScore());

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
     * 规则改了部门 / 名称时，先把旧模板一起改名，
     * 避免学生端残留旧项目；再按新名称同步一次。
     * =========================================================
     */
    public void syncRename(
            Long oldDepartmentId,
            String oldName,
            ScoreRule rule
    ) {

        if (rule == null
                || oldDepartmentId == null
                || oldName == null) {

            sync(rule);

            return;
        }

        boolean moved =
                !Objects.equals(
                        oldDepartmentId,
                        rule.getDepartmentId()
                )
                        || !Objects.equals(
                        oldName,
                        rule.getName()
                );

        if (moved) {

            DepartmentScoreTemplate oldTemplate =
                    templateMapper.selectOne(
                            new LambdaQueryWrapper<DepartmentScoreTemplate>()
                                    .eq(
                                            DepartmentScoreTemplate::getDepartmentId,
                                            oldDepartmentId
                                    )
                                    .eq(
                                            DepartmentScoreTemplate::getName,
                                            oldName
                                    )
                                    .last("LIMIT 1")
                    );

            if (oldTemplate != null) {

                oldTemplate.setDepartmentId(rule.getDepartmentId());
                oldTemplate.setName(rule.getName());
                oldTemplate.setUpdateTime(LocalDateTime.now());

                templateMapper.updateById(oldTemplate);
            }
        }

        sync(rule);
    }

    /**
     * =========================================================
     * 删除规则时同步删除对应的部门模板
     * =========================================================
     */
    public void remove(ScoreRule rule) {

        if (rule == null
                || rule.getDepartmentId() == null
                || rule.getName() == null) {

            return;
        }

        templateMapper.delete(
                new LambdaQueryWrapper<DepartmentScoreTemplate>()
                        .eq(
                                DepartmentScoreTemplate::getDepartmentId,
                                rule.getDepartmentId()
                        )
                        .eq(
                                DepartmentScoreTemplate::getName,
                                rule.getName()
                        )
        );
    }
}
