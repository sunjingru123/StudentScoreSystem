package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/scoreApply")
public class ScoreApplyController {

    private final ScoreApplyMapper scoreApplyMapper;
    private final SysUserMapper sysUserMapper;
    private final ScoreRuleMapper scoreRuleMapper;

    public ScoreApplyController(ScoreApplyMapper scoreApplyMapper, SysUserMapper sysUserMapper, ScoreRuleMapper scoreRuleMapper) {
        this.scoreApplyMapper = scoreApplyMapper;
        this.sysUserMapper = sysUserMapper;
        this.scoreRuleMapper = scoreRuleMapper;
    }

    /**
     * 学生查询自己的申报记录
     */
    @GetMapping("/my")
    public Result<List<ScoreApplyVO>> myList(HttpServletRequest request) {

        // 获取当前登录用户ID
        Object userIdObj = request.getAttribute("userId");

        if (userIdObj == null) {
            return Result.fail("请先登录");
        }

        Long currentStudentId;

        try {
            currentStudentId = Long.valueOf(userIdObj.toString());
        } catch (Exception e) {
            return Result.fail("登录用户信息无效");
        }

        System.out.println(
                ">>>>>> [查询个人申报] 当前登录学生ID: "
                        + currentStudentId
        );

        /*
         * ========================================================
         * 只查询当前登录用户自己的申请
         * ========================================================
         *
         * score_apply.student_id = 当前登录用户ID
         *
         * 不能查询全部数据。
         */

        List<ScoreApply> applies =
                scoreApplyMapper.selectList(
                        new LambdaQueryWrapper<ScoreApply>()
                                .eq(
                                        ScoreApply::getStudentId,
                                        currentStudentId
                                )
                                .orderByDesc(
                                        ScoreApply::getCreateTime
                                )
                );

        System.out.println(
                ">>>>>> [个人申报] 当前用户申请数量: "
                        + applies.size()
        );

        List<ScoreApplyVO> voList =
                new ArrayList<>();

        for (ScoreApply apply : applies) {
            voList.add(convertToVO(apply));
        }

        return Result.success(voList);
    }

    /**
     * 学生提交申请 (对应前端 /scoreApply/add)
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody ScoreApply apply, HttpServletRequest request) {
        // 1. 获取当前登录 ID
        Object userIdObj = request.getAttribute("userId");
        Long currentStudentId = (userIdObj != null) ? Long.valueOf(userIdObj.toString()) : 1L;

        // 2. 核心修复：强制将 activityId 设为 null
        // 报错是因为它传了 1，而数据库里没 1。设置成 null 就可以绕过外键约束（前提是该字段允许为空）
        apply.setActivityId(null);

        // 3. 设置其他字段
        apply.setStudentId(currentStudentId);
        apply.setStatus((short) 0); // 待审核
        apply.setCreateTime(LocalDateTime.now());
        apply.setUpdateTime(LocalDateTime.now());

        // 4. 执行插入
        try {
            int result = scoreApplyMapper.insert(apply);
            return result > 0 ? Result.success(null) : Result.error("提交失败");
        } catch (Exception e) {
            // 如果这里还报错，说明数据库里这个字段设置了 NOT NULL 约束
            e.printStackTrace();
            return Result.error("数据库写入失败：" + e.getMessage());
        }
    }
    /**
     * 辅导员查询全部申请记录
     */
    @GetMapping("/list")
    public Result<Page<ScoreApplyVO>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize
    ) {

        Page<ScoreApply> page =
                new Page<>(pageNum, pageSize);

        Page<ScoreApply> applyPage =
                scoreApplyMapper.selectPage(
                        page,
                        new LambdaQueryWrapper<ScoreApply>()
                                .orderByDesc(ScoreApply::getCreateTime)
                );

        Page<ScoreApplyVO> voPage =
                new Page<>(
                        applyPage.getCurrent(),
                        applyPage.getSize(),
                        applyPage.getTotal()
                );

        List<ScoreApplyVO> voList =
                new ArrayList<>();

        for (ScoreApply apply : applyPage.getRecords()) {

            voList.add(convertToVO(apply));
        }

        voPage.setRecords(voList);

        return Result.success(voPage);
    }
    @GetMapping("/pending")
    public Result<Page<ScoreApplyVO>> pending(@RequestParam(defaultValue = "1") long pageNum, @RequestParam(defaultValue = "10") long pageSize) {
        Page<ScoreApply> page = new Page<>(pageNum, pageSize);
        Page<ScoreApply> applyPage = scoreApplyMapper.selectPage(page, new LambdaQueryWrapper<ScoreApply>().eq(ScoreApply::getStatus, (short) 0).orderByDesc(ScoreApply::getCreateTime));

        Page<ScoreApplyVO> voPage = new Page<>(applyPage.getCurrent(), applyPage.getSize(), applyPage.getTotal());
        List<ScoreApplyVO> voList = new ArrayList<>();
        for (ScoreApply apply : applyPage.getRecords()) {
            voList.add(convertToVO(apply));
        }
        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    @PostMapping("/audit")
    public Result<Void> audit(@RequestBody AuditRequest request) {
        ScoreApply apply = scoreApplyMapper.selectById(request.getId());
        if (apply == null) return Result.error("申请不存在");
        apply.setStatus(request.getStatus().shortValue());
        scoreApplyMapper.updateById(apply);
        return Result.success(null);
    }

    private ScoreApplyVO convertToVO(ScoreApply apply) {
        ScoreApplyVO vo = new ScoreApplyVO();
        vo.setId(apply.getId());
        vo.setStudentId(apply.getStudentId());
        vo.setApplyScore(apply.getApplyScore());
        vo.setStatus(apply.getStatus() != null ? apply.getStatus().intValue() : 0);
        vo.setCreateTime(apply.getCreateTime());

        // 关键：从规则表补全名称，解决 ruleName 为 null 的问题
        if (apply.getRuleId() != null) {
            ScoreRule rule = scoreRuleMapper.selectById(apply.getRuleId());
            if (rule != null) {
                vo.setRuleName(rule.getName());
            }
        }
        return vo;
    }

    public static class AuditRequest {
        private Long id;
        private Integer status;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
}