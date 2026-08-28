package com.student.studentscoresystem.service;

import com.student.studentscoresystem.vo.ScoreStatisticsVO;

public interface IScoreStatisticsService {
    /**
     * 计算指定学生的成绩统计信息
     */
    ScoreStatisticsVO calculateStats(Long studentId);
}