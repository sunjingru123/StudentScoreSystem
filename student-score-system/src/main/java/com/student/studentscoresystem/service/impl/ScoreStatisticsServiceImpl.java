package com.student.studentscoresystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.service.IScoreStatisticsService;
import com.student.studentscoresystem.vo.ScoreStatisticsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ScoreStatisticsServiceImpl implements IScoreStatisticsService {

    @Autowired
    private ScoreRecordMapper scoreRecordMapper;

    @Override
    public ScoreStatisticsVO calculateStats(Long studentId) {
        ScoreStatisticsVO vo = new ScoreStatisticsVO();

        // 1. 查询所有未隐藏的成绩记录
        List<ScoreRecord> records = scoreRecordMapper.selectList(
                new LambdaQueryWrapper<ScoreRecord>()
                        .eq(ScoreRecord::getStudentId, studentId)
                        .eq(ScoreRecord::getAdminHidden, (short) 0)
        );

        BigDecimal bonus = BigDecimal.ZERO;
        BigDecimal deduct = BigDecimal.ZERO;

        for (ScoreRecord record : records) {
            BigDecimal score = record.getScore();
            if (score.compareTo(BigDecimal.ZERO) >= 0) {
                bonus = bonus.add(score);
            } else {
                deduct = deduct.add(score.abs());
            }
        }

        // 2. 这里的计算逻辑应与你之前的 ScoreStatisticsController 保持一致
        BigDecimal baseLimit = new BigDecimal("40");
        BigDecimal total = baseLimit.add(bonus).subtract(deduct);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        vo.setBonusScore(bonus);
        vo.setDeductScore(deduct);
        vo.setTotalScore(total);
        vo.setBaseLimit(baseLimit);
        vo.setActualLimit(baseLimit); // 假设目前上限等于基础上限

        return vo;
    }
}