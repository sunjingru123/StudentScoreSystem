package com.student.studentscoresystem.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.ScoreRuleExcelRow;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.ExcelImportService;
import com.student.studentscoresystem.service.IScoreRuleService;
import com.student.studentscoresystem.service.ScoreRuleManageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DuplicateKeyException;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/scoreRule")
public class ScoreRuleController {

    private final IScoreRuleService scoreRuleService;

    private final ScoreRuleManageService scoreRuleManageService;

    private final ExcelImportService excelImportService;

    private final SysPositionMapper sysPositionMapper;

    private final SysUserPositionMapper sysUserPositionMapper;

    public ScoreRuleController(
            IScoreRuleService scoreRuleService,
            ScoreRuleManageService scoreRuleManageService,
            ExcelImportService excelImportService,
            SysPositionMapper sysPositionMapper,
            SysUserPositionMapper sysUserPositionMapper
    ) {
        this.scoreRuleService = scoreRuleService;
        this.scoreRuleManageService = scoreRuleManageService;
        this.excelImportService = excelImportService;
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
     *
     * 规则落库与模板同步在同一个事务中完成。
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

        try {

            String message = scoreRuleManageService.add(rule);

            if (message != null) {
                return Result.fail(message);
            }

            return Result.success(null);

        } catch (DuplicateKeyException e) {

            /*
             * 并发下同一部门同名规则同时落库，
             * 由数据库唯一索引 uk_score_rule_dept_name 拦截。
             */
            return Result.fail(
                    "该部门下已经存在同名规则，请勿重复添加"
            );
        }
    }

    /**
     * =========================================================
     * 修改规则
     *
     * 规则落库与模板同步在同一个事务中完成。
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

        try {

            String message = scoreRuleManageService.update(id, rule);

            if (message != null) {
                return Result.fail(message);
            }

            return Result.success(null);

        } catch (DuplicateKeyException e) {

            /*
             * 修改后与同部门其他规则重名，
             * 由数据库唯一索引 uk_score_rule_dept_name 拦截。
             */
            return Result.fail(
                    "该部门下已经存在同名规则，请更换名称"
            );
        }
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

        String message = scoreRuleManageService.delete(id);

        if (message != null) {
            return Result.fail(message);
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
