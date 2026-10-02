package com.student.studentscoresystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.ScoreApply;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.ScoreApplyMapper;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * =========================================================
 * 评分规则管理
 *
 * 负责管理员「评分项目管理」的评分规则落库，
 * 并在同一个事务内同步到部门加减分模板
 * （department_score_template），避免双写不一致。
 * =========================================================
 */
@Service
public class ScoreRuleManageService {

    private final IScoreRuleService scoreRuleService;

    private final DepartmentTemplateSyncService departmentTemplateSyncService;

    private final DepartmentMapper departmentMapper;

    private final ScoreRecordMapper scoreRecordMapper;

    private final ScoreApplyMapper scoreApplyMapper;

    public ScoreRuleManageService(
            IScoreRuleService scoreRuleService,
            DepartmentTemplateSyncService departmentTemplateSyncService,
            DepartmentMapper departmentMapper,
            ScoreRecordMapper scoreRecordMapper,
            ScoreApplyMapper scoreApplyMapper
    ) {

        this.scoreRuleService = scoreRuleService;
        this.departmentTemplateSyncService = departmentTemplateSyncService;
        this.departmentMapper = departmentMapper;
        this.scoreRecordMapper = scoreRecordMapper;
        this.scoreApplyMapper = scoreApplyMapper;
    }

    /**
     * =========================================================
     * 新增规则
     *
     * @return null 表示成功，否则为业务失败提示
     * =========================================================
     */
    @Transactional(rollbackFor = Exception.class)
    public String add(ScoreRule rule) {

        if (rule == null) {
            return "参数不能为空";
        }

        String message = checkRule(rule, null);

        if (message != null) {
            return message;
        }

        LocalDateTime now = LocalDateTime.now();

        rule.setId(null);
        rule.setName(rule.getName().trim());
        rule.setDescription(emptyToNull(rule.getDescription()));
        rule.setScore(scaleScore(rule.getScore()));
        rule.setStatus(
                rule.getStatus() == null
                        ? (short) 1
                        : rule.getStatus()
        );
        rule.setCreateTime(now);
        rule.setUpdateTime(now);

        if (!scoreRuleService.save(rule)) {
            return "规则新增失败";
        }

        /*
         * 同一事务内同步模板：
         * 规则落库成功但模板同步失败时会一起回滚。
         */
        departmentTemplateSyncService.sync(rule);

        return null;
    }

    /**
     * =========================================================
     * 修改规则
     *
     * @return null 表示成功，否则为业务失败提示
     * =========================================================
     */
    @Transactional(rollbackFor = Exception.class)
    public String update(Long id, ScoreRule rule) {

        if (id == null) {
            return "规则 ID 不能为空";
        }

        if (rule == null) {
            return "参数不能为空";
        }

        ScoreRule oldRule = scoreRuleService.getById(id);

        if (oldRule == null) {
            return "规则不存在";
        }

        String message = checkRule(rule, id);

        if (message != null) {
            return message;
        }

        /*
         * 记下修改前的部门 / 名称，用于同步模板改名。
         */
        Long oldDepartmentId = oldRule.getDepartmentId();
        String oldName = oldRule.getName();

        oldRule.setDepartmentId(rule.getDepartmentId());
        oldRule.setName(rule.getName().trim());
        oldRule.setScoreType(rule.getScoreType());
        oldRule.setDescription(emptyToNull(rule.getDescription()));
        oldRule.setScore(scaleScore(rule.getScore()));
        oldRule.setStatus(
                rule.getStatus() == null
                        ? oldRule.getStatus()
                        : rule.getStatus()
        );
        oldRule.setUpdateTime(LocalDateTime.now());

        if (!scoreRuleService.updateById(oldRule)) {
            return "规则修改失败";
        }

        departmentTemplateSyncService.syncRename(
                oldDepartmentId,
                oldName,
                oldRule
        );

        return null;
    }

    /**
     * =========================================================
     * 删除规则
     *
     * 已经被成绩记录、申报记录引用的规则不允许删除，
     * 避免破坏历史数据。
     *
     * @return null 表示成功，否则为业务失败提示
     * =========================================================
     */
    @Transactional(rollbackFor = Exception.class)
    public String delete(Long id) {

        if (id == null) {
            return "规则 ID 不能为空";
        }

        ScoreRule rule = scoreRuleService.getById(id);

        if (rule == null) {
            return "规则不存在";
        }

        Long recordCount = scoreRecordMapper.selectCount(
                new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getRuleId, id)
        );

        if (recordCount != null && recordCount > 0) {
            return "该规则已被成绩记录引用（" +
                    recordCount +
                    " 条），不能删除，建议改为停用";
        }

        Long applyCount = scoreApplyMapper.selectCount(
                new LambdaQueryWrapper<ScoreApply>()
                        .eq(ScoreApply::getRuleId, id)
        );

        if (applyCount != null && applyCount > 0) {
            return "该规则已被申报记录引用（" +
                    applyCount +
                    " 条），不能删除，建议改为停用";
        }

        if (!scoreRuleService.removeById(id)) {
            return "规则删除失败";
        }

        /*
         * 同步删除对应部门的加减分模板，
         * 避免学生端还能选到已删除的固定项目。
         */
        departmentTemplateSyncService.remove(rule);

        return null;
    }

    /**
     * =========================================================
     * 规则数据校验
     *
     * 返回 null 表示校验通过
     * =========================================================
     */
    private String checkRule(
            ScoreRule rule,
            Long excludeId
    ) {

        if (rule.getDepartmentId() == null) {
            return "请选择所属部门";
        }

        Department department = departmentMapper.selectById(
                rule.getDepartmentId()
        );

        if (department == null) {
            return "所属部门不存在";
        }

        String name = rule.getName() == null
                ? ""
                : rule.getName().trim();

        if (name.isEmpty()) {
            return "规则名称不能为空";
        }

        if (name.length() > 100) {
            return "规则名称不能超过 100 个字符";
        }

        if (rule.getScoreType() == null
                || (rule.getScoreType() != 1
                && rule.getScoreType() != -1)) {
            return "请选择加分或减分";
        }

        if (rule.getScore() == null) {
            return "请输入分值";
        }

        if (rule.getScore().compareTo(BigDecimal.ZERO) <= 0) {
            return "分值必须大于 0";
        }

        /*
         * 同一个部门下不允许出现同名规则，
         * 因为部门申报终审时是按 部门 + 规则名称 匹配正式规则的。
         */
        Long count = scoreRuleService.count(
                new LambdaQueryWrapper<ScoreRule>()
                        .eq(ScoreRule::getDepartmentId, rule.getDepartmentId())
                        .eq(ScoreRule::getName, name)
                        .ne(excludeId != null, ScoreRule::getId, excludeId)
        );

        if (count != null && count > 0) {
            return "该部门下已经存在同名规则：" + name;
        }

        return null;
    }

    /**
     * =========================================================
     * 分值保留两位小数
     * =========================================================
     */
    private BigDecimal scaleScore(BigDecimal score) {
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * =========================================================
     * 空字符串转 null
     * =========================================================
     */
    private String emptyToNull(String value) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
