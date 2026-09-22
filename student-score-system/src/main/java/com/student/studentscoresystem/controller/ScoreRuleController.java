package com.student.studentscoresystem.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.ScoreRuleExcelRow;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.ScoreApply;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.ScoreApplyMapper;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.ExcelImportService;
import com.student.studentscoresystem.service.IScoreRuleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/scoreRule")
public class ScoreRuleController {

    private final IScoreRuleService scoreRuleService;

    private final ExcelImportService excelImportService;

    private final DepartmentMapper departmentMapper;

    private final ScoreRecordMapper scoreRecordMapper;

    private final ScoreApplyMapper scoreApplyMapper;

    private final SysPositionMapper sysPositionMapper;

    private final SysUserPositionMapper sysUserPositionMapper;

    public ScoreRuleController(
            IScoreRuleService scoreRuleService,
            ExcelImportService excelImportService,
            DepartmentMapper departmentMapper,
            ScoreRecordMapper scoreRecordMapper,
            ScoreApplyMapper scoreApplyMapper,
            SysPositionMapper sysPositionMapper,
            SysUserPositionMapper sysUserPositionMapper
    ) {
        this.scoreRuleService = scoreRuleService;
        this.excelImportService = excelImportService;
        this.departmentMapper = departmentMapper;
        this.scoreRecordMapper = scoreRecordMapper;
        this.scoreApplyMapper = scoreApplyMapper;
        this.sysPositionMapper = sysPositionMapper;
        this.sysUserPositionMapper = sysUserPositionMapper;
    }

    /**
     * =========================================================
     * 规则列表
     *
     * 仅返回部门加减分的正式规则（department_id 不为空）
     * =========================================================
     */
    @GetMapping("/list")
    public Result<List<ScoreRule>> list() {
        List<ScoreRule> rules = scoreRuleService.list(
                new LambdaQueryWrapper<ScoreRule>()
                        .isNotNull(ScoreRule::getDepartmentId)
                        .orderByDesc(ScoreRule::getCreateTime)
        );
        return Result.success(rules);
    }

    /**
     * =========================================================
     * 规则详情
     * =========================================================
     */
    @GetMapping("/{id}")
    public Result<ScoreRule> detail(@PathVariable Long id) {
        ScoreRule rule = scoreRuleService.getById(id);
        if (rule == null) {
            return Result.fail("规则不存在");
        }
        return Result.success(rule);
    }

    /**
     * =========================================================
     * 新增规则
     * =========================================================
     */
    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody ScoreRule rule,
            HttpServletRequest request
    ) {

        if (!isAdmin(request)) {
            return Result.fail("没有管理员权限");
        }

        if (rule == null) {
            return Result.fail("参数不能为空");
        }

        String message = checkRule(rule, null);

        if (message != null) {
            return Result.fail(message);
        }

        LocalDateTime now = LocalDateTime.now();

        rule.setId(null);
        rule.setName(rule.getName().trim());
        rule.setCategory(emptyToNull(rule.getCategory()));
        rule.setDescription(emptyToNull(rule.getDescription()));
        rule.setScore(scaleScore(rule.getScore()));
        rule.setStatus(rule.getStatus() == null ? (short) 1 : rule.getStatus());
        rule.setCreateTime(now);
        rule.setUpdateTime(now);

        if (!scoreRuleService.save(rule)) {
            return Result.fail("规则新增失败");
        }

        return Result.success(null);
    }

    /**
     * =========================================================
     * 修改规则
     * =========================================================
     */
    @PutMapping("/update/{id}")
    public Result<Void> update(
            @PathVariable Long id,
            @RequestBody ScoreRule rule,
            HttpServletRequest request
    ) {

        if (!isAdmin(request)) {
            return Result.fail("没有管理员权限");
        }

        if (id == null) {
            return Result.fail("规则 ID 不能为空");
        }

        if (rule == null) {
            return Result.fail("参数不能为空");
        }

        ScoreRule oldRule = scoreRuleService.getById(id);

        if (oldRule == null) {
            return Result.fail("规则不存在");
        }

        String message = checkRule(rule, id);

        if (message != null) {
            return Result.fail(message);
        }

        oldRule.setDepartmentId(rule.getDepartmentId());
        oldRule.setName(rule.getName().trim());
        oldRule.setCategory(emptyToNull(rule.getCategory()));
        oldRule.setDescription(emptyToNull(rule.getDescription()));
        oldRule.setScore(scaleScore(rule.getScore()));
        oldRule.setStatus(
                rule.getStatus() == null
                        ? oldRule.getStatus()
                        : rule.getStatus()
        );
        oldRule.setUpdateTime(LocalDateTime.now());

        if (!scoreRuleService.updateById(oldRule)) {
            return Result.fail("规则修改失败");
        }

        return Result.success(null);
    }

    /**
     * =========================================================
     * 删除规则
     *
     * 已经被成绩记录、申报记录引用的规则不允许删除，
     * 避免破坏历史数据。
     * =========================================================
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(
            @PathVariable Long id,
            HttpServletRequest request
    ) {

        if (!isAdmin(request)) {
            return Result.fail("没有管理员权限");
        }

        if (id == null) {
            return Result.fail("规则 ID 不能为空");
        }

        ScoreRule rule = scoreRuleService.getById(id);

        if (rule == null) {
            return Result.fail("规则不存在");
        }

        Long recordCount = scoreRecordMapper.selectCount(
                new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getRuleId, id)
        );

        if (recordCount != null && recordCount > 0) {
            return Result.fail(
                    "该规则已被成绩记录引用（" +
                            recordCount +
                            " 条），不能删除，建议改为停用"
            );
        }

        Long applyCount = scoreApplyMapper.selectCount(
                new LambdaQueryWrapper<ScoreApply>()
                        .eq(ScoreApply::getRuleId, id)
        );

        if (applyCount != null && applyCount > 0) {
            return Result.fail(
                    "该规则已被申报记录引用（" +
                            applyCount +
                            " 条），不能删除，建议改为停用"
            );
        }

        if (!scoreRuleService.removeById(id)) {
            return Result.fail("规则删除失败");
        }

        return Result.success(null);
    }

    /**
     * =========================================================
     * 导入规则
     *
     * 部门 + 规则名称 已存在则更新，否则新增
     * =========================================================
     */
    @PostMapping("/import")
    public Result<Map<String, Object>> importRules(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {

        if (!isAdmin(request)) {
            return Result.fail("没有管理员权限");
        }

        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要导入的 Excel 文件");
        }

        try {

            return Result.success(
                    excelImportService.importScoreRules(file)
            );

        } catch (Exception e) {

            return Result.fail(
                    "规则导入失败：" + e.getMessage()
            );
        }
    }

    /**
     * =========================================================
     * 下载导入模板
     * =========================================================
     */
    @GetMapping("/import/template")
    public void importTemplate(
            HttpServletResponse response
    ) throws IOException {

        String fileName = URLEncoder.encode(
                "评分项目导入模板.xlsx",
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

        /*
         * 只写表头，数据行由使用者自己填写，
         * 表头与 ScoreRuleExcelRow 保持一致。
         */
        EasyExcel.write(
                        response.getOutputStream(),
                        ScoreRuleExcelRow.class
                )
                .sheet("规则导入模板")
                .doWrite(new ArrayList<ScoreRuleExcelRow>());

        response.flushBuffer();
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

        if (rule.getCategory() != null
                && rule.getCategory().trim().length() > 50) {
            return "分类不能超过 50 个字符";
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

    /**
     * =========================================================
     * 判断当前登录用户是否为管理员
     * =========================================================
     */
    private boolean isAdmin(HttpServletRequest request) {

        Object userIdAttr = request.getAttribute("userId");

        if (userIdAttr == null) {
            return false;
        }

        Long userId;

        try {

            userId = Long.valueOf(userIdAttr.toString());

        } catch (Exception e) {

            return false;
        }

        SysPosition adminPosition = sysPositionMapper.selectOne(
                new LambdaQueryWrapper<SysPosition>()
                        .eq(SysPosition::getName, "管理员")
                        .orderByAsc(SysPosition::getId)
                        .last("LIMIT 1")
        );

        if (adminPosition == null) {
            return false;
        }

        Long count = sysUserPositionMapper.selectCount(
                new LambdaQueryWrapper<SysUserPosition>()
                        .eq(SysUserPosition::getUserId, userId)
                        .eq(SysUserPosition::getPositionId, adminPosition.getId())
        );

        return count != null && count > 0;
    }
}
