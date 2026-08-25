package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.ScoreAdminAdjustment;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.ScoreAdminAdjustmentMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.vo.ScoreAdminAdjustmentVO;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员成绩调整
 */
@RestController
@RequestMapping("/admin/scoreAdjustment")
public class ScoreAdminAdjustmentController {

    private final ScoreAdminAdjustmentMapper adjustmentMapper;
    private final SysUserMapper sysUserMapper;

    public ScoreAdminAdjustmentController(
            ScoreAdminAdjustmentMapper adjustmentMapper,
            SysUserMapper sysUserMapper
    ) {
        this.adjustmentMapper = adjustmentMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @GetMapping("/list")
    public Result<Page<ScoreAdminAdjustmentVO>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;

        Page<ScoreAdminAdjustment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ScoreAdminAdjustment> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ScoreAdminAdjustment::getCreateTime);

        Page<ScoreAdminAdjustment> adjustmentPage = adjustmentMapper.selectPage(page, wrapper);

        Page<ScoreAdminAdjustmentVO> voPage = new Page<>(
                adjustmentPage.getCurrent(),
                adjustmentPage.getSize(),
                adjustmentPage.getTotal()
        );

        List<ScoreAdminAdjustmentVO> voList = new ArrayList<>();

        for (ScoreAdminAdjustment adjustment : adjustmentPage.getRecords()) {
            if (adjustment == null) continue;

            ScoreAdminAdjustmentVO vo = new ScoreAdminAdjustmentVO();
            vo.setId(adjustment.getId());
            vo.setStudentId(adjustment.getStudentId());

            // 【修复点】将 Short 转换为 Integer
            if (adjustment.getAdjustType() != null) {
                vo.setAdjustType(adjustment.getAdjustType().intValue());
            }

            vo.setScore(adjustment.getScore());
            vo.setReason(adjustment.getReason());
            vo.setAdminId(adjustment.getAdminId());
            vo.setCreateTime(adjustment.getCreateTime());

            if (adjustment.getStudentId() != null) {
                SysUser student = sysUserMapper.selectById(adjustment.getStudentId());
                if (student != null) {
                    vo.setStudentName(student.getRealName());
                    vo.setStudentNo(student.getStudentNo());
                }
            }

            if (adjustment.getAdminId() != null) {
                SysUser admin = sysUserMapper.selectById(adjustment.getAdminId());
                if (admin != null) {
                    vo.setAdminName(admin.getRealName());
                }
            }
            voList.add(vo);
        }

        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody ScoreAdminAdjustment adjustment) {
        if (adjustment == null) return Result.error("参数不能为空");
        if (adjustment.getStudentId() == null) return Result.error("请选择学生");
        if (adjustment.getAdjustType() == null) return Result.error("请选择调整类型");

        // 【修复点】使用 intValue() 比较
        int type = adjustment.getAdjustType().intValue();
        if (type != 1 && type != -1) {
            return Result.error("调整类型只能是加分(1)或减分(-1)");
        }

        if (adjustment.getScore() == null || adjustment.getScore().doubleValue() <= 0) {
            return Result.error("调整分数必须大于0");
        }

        if (adjustment.getReason() == null || adjustment.getReason().trim().isEmpty()) {
            return Result.error("请输入调整原因");
        }

        SysUser student = sysUserMapper.selectById(adjustment.getStudentId());
        if (student == null) return Result.error("学生不存在");

        int result = adjustmentMapper.insert(adjustment);
        return result > 0 ? Result.success(null) : Result.error("成绩调整失败");
    }
}