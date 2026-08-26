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

/**
 * 学生加分申请
 */
@RestController
@RequestMapping("/scoreApply")
public class ScoreApplyController {

    private final ScoreApplyMapper scoreApplyMapper;
    private final SysUserMapper sysUserMapper;
    private final ScoreRuleMapper scoreRuleMapper; // 注入 RuleMapper 解决名称显示问题

    public ScoreApplyController(
            ScoreApplyMapper scoreApplyMapper,
            SysUserMapper sysUserMapper,
            ScoreRuleMapper scoreRuleMapper
    ) {
        this.scoreApplyMapper = scoreApplyMapper;
        this.sysUserMapper = sysUserMapper;
        this.scoreRuleMapper = scoreRuleMapper;
    }

    /**
     * =========================================================
     * 【新增】学生提交申报记录
     * =========================================================
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody ScoreApply apply, HttpServletRequest request) {
        // 这里假设你已经有了从 Token/Session 获取当前登录学生ID的逻辑
        // 如果你没有拦截器处理，暂时从 header 或者手动 mock 一个 ID 进行测试
        // 建议：此处应获取当前登录人的 ID
        Long currentStudentId = (Long) request.getAttribute("userId");

        if (currentStudentId == null) {
            return Result.error("用户未登录或登录已过期");
        }

        apply.setStudentId(currentStudentId);
        apply.setCreateTime(LocalDateTime.now());
        apply.setUpdateTime(LocalDateTime.now());
        apply.setStatus((short) 0); // 默认为待审核状态

        int result = scoreApplyMapper.insert(apply);
        return result > 0 ? Result.success(null) : Result.error("申报提交失败");
    }

    /**
     * =========================================================
     * 【新增】学生查询自己的申报记录
     * =========================================================
     */
    @GetMapping("/my")
    public Result<List<ScoreApplyVO>> myList(HttpServletRequest request) {
        Long currentStudentId = (Long) request.getAttribute("userId");

        if (currentStudentId == null) {
            return Result.error("无法获取当前用户信息");
        }

        // 查询该学生的所有记录
        List<ScoreApply> applies = scoreApplyMapper.selectList(
                new LambdaQueryWrapper<ScoreApply>()
                        .eq(ScoreApply::getStudentId, currentStudentId)
                        .orderByDesc(ScoreApply::getCreateTime)
        );

        List<ScoreApplyVO> voList = new ArrayList<>();
        for (ScoreApply apply : applies) {
            voList.add(convertToVO(apply));
        }

        return Result.success(voList);
    }

    /**
     * 分页查询待审核申请 (管理员用)
     */
    @GetMapping("/pending")
    public Result<Page<ScoreApplyVO>> pending(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        Page<ScoreApply> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ScoreApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScoreApply::getStatus, (short) 0).orderByDesc(ScoreApply::getCreateTime);

        Page<ScoreApply> applyPage = scoreApplyMapper.selectPage(page, wrapper);
        Page<ScoreApplyVO> voPage = new Page<>(applyPage.getCurrent(), applyPage.getSize(), applyPage.getTotal());

        List<ScoreApplyVO> voList = new ArrayList<>();
        for (ScoreApply apply : applyPage.getRecords()) {
            voList.add(convertToVO(apply));
        }
        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    /**
     * 审核申请
     */
    @PostMapping("/audit")
    public Result<Void> audit(@RequestBody AuditRequest request) {
        if (request == null || request.getId() == null) return Result.error("参数错误");
        ScoreApply apply = scoreApplyMapper.selectById(request.getId());
        if (apply == null) return Result.error("申请不存在");
        if (apply.getStatus() != null && apply.getStatus().intValue() != 0) return Result.error("该申请已经审核过了");

        apply.setStatus(request.getStatus().shortValue());
        apply.setUpdateTime(LocalDateTime.now());
        int result = scoreApplyMapper.updateById(apply);
        return result > 0 ? Result.success(null) : Result.error("审核失败");
    }

    /**
     * 内部转换方法：将 Entity 转为 VO 并补全学生名和规则名
     */
    private ScoreApplyVO convertToVO(ScoreApply apply) {
        if (apply == null) return null;
        ScoreApplyVO vo = new ScoreApplyVO();
        vo.setId(apply.getId());
        vo.setStudentId(apply.getStudentId());
        vo.setApplyScore(apply.getApplyScore());
        vo.setStatus(apply.getStatus() != null ? apply.getStatus().intValue() : 0);
        vo.setCreateTime(apply.getCreateTime());

        // 1. 补全规则名称 (解决你之前的报错)
        if (apply.getRuleId() != null) {
            ScoreRule rule = scoreRuleMapper.selectById(apply.getRuleId());
            if (rule != null) {
                vo.setRuleName(rule.getName()); // 假设规则表名称字段是 name
            }
        }

        // 2. 补全学生信息
        if (apply.getStudentId() != null) {
            SysUser student = sysUserMapper.selectById(apply.getStudentId());
            if (student != null) {
                vo.setStudentName(student.getRealName());
                vo.setStudentNo(student.getStudentNo());
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