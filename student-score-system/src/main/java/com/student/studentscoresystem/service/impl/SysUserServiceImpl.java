package com.student.studentscoresystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.ISysUserService;
import com.student.studentscoresystem.service.IScoreStatisticsService; // 现在可以找到了
import com.student.studentscoresystem.vo.SysUserVO;
import com.student.studentscoresystem.vo.ScoreStatisticsVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Autowired
    private IScoreStatisticsService scoreStatisticsService;

    @Override
    public Page<SysUserVO> getStudentPageWithScores(long pageNum, long pageSize, String keyword) {
        // 1. 分页查询学生
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getStudentNo, keyword)
                    .or().like(SysUser::getClassName, keyword));
        }
        wrapper.orderByAsc(SysUser::getStudentNo);
        this.page(page, wrapper);

        // 2. 填充 VO 数据
        Page<SysUserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());

        voPage.setRecords(page.getRecords().stream().map(user -> {
            SysUserVO vo = new SysUserVO();
            BeanUtils.copyProperties(user, vo);

            // 计算分数
            ScoreStatisticsVO stats = scoreStatisticsService.calculateStats(user.getId());
            vo.setTotalScore(stats.getTotalScore());
            vo.setBonusScore(stats.getBonusScore());
            vo.setDeductScore(stats.getDeductScore());
            vo.setActualLimit(stats.getActualLimit());

            return vo;
        }).collect(Collectors.toList()));

        return voPage;
    }
}