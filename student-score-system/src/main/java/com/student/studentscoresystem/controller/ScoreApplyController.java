package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.ScoreApply;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.ScoreApplyMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.vo.ScoreApplyVO;
import org.springframework.web.bind.annotation.*;

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

    public ScoreApplyController(ScoreApplyMapper scoreApplyMapper, SysUserMapper sysUserMapper) {
        this.scoreApplyMapper = scoreApplyMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @GetMapping("/pending")
    public Result<Page<ScoreApplyVO>> pending(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;

        Page<ScoreApply> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ScoreApply> wrapper = new LambdaQueryWrapper<>();

        // 显式转为 short 匹配实体类字段
        wrapper.eq(ScoreApply::getStatus, (short) 0)
                .orderByDesc(ScoreApply::getCreateTime);

        Page<ScoreApply> applyPage = scoreApplyMapper.selectPage(page, wrapper);

        Page<ScoreApplyVO> voPage = new Page<>(
                applyPage.getCurrent(),
                applyPage.getSize(),
                applyPage.getTotal()
        );

        List<ScoreApplyVO> voList = new ArrayList<>();
        for (ScoreApply apply : applyPage.getRecords()) {
            if (apply == null) continue;

            ScoreApplyVO vo = new ScoreApplyVO();
            vo.setId(apply.getId());
            vo.setStudentId(apply.getStudentId());
            vo.setApplyScore(apply.getApplyScore());

            // 【修复点】将 Short 转换为 Integer
            if (apply.getStatus() != null) {
                vo.setStatus(apply.getStatus().intValue());
            }

            vo.setCreateTime(apply.getCreateTime());
            // =========仅这里修改，消除编译报错，其他全部不动=========
            vo.setRuleName(null);

            if (apply.getStudentId() != null) {
                SysUser student = sysUserMapper.selectById(apply.getStudentId());
                if (student != null) {
                    vo.setStudentName(student.getRealName());
                    vo.setStudentNo(student.getStudentNo());
                }
            }
            voList.add(vo);
        }

        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    @PostMapping("/audit")
    public Result<Void> audit(@RequestBody AuditRequest request) {
        if (request == null || request.getId() == null) return Result.error("参数错误");
        if (request.getStatus() == null || (request.getStatus() != 1 && request.getStatus() != 2)) {
            return Result.error("审核状态不正确");
        }

        ScoreApply apply = scoreApplyMapper.selectById(request.getId());
        if (apply == null) return Result.error("申请不存在");

        // 【修复点】使用 intValue() 比较
        if (apply.getStatus() != null && apply.getStatus().intValue() != 0) {
            return Result.error("该申请已经审核过了");
        }

        // 【修复点】将 Integer 转为 short
        apply.setStatus(request.getStatus().shortValue());

        int result = scoreApplyMapper.updateById(apply);
        return result > 0 ? Result.success(null) : Result.error("审核失败");
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