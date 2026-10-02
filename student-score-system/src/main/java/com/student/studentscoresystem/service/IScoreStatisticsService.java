package com.student.studentscoresystem.service;

import com.student.studentscoresystem.vo.ScoreStatisticsVO;

public interface IScoreStatisticsService {

    /**
     * 计算指定学生的成绩统计信息（按当前生效学期过滤）。
     */
    ScoreStatisticsVO calculateStats(Long studentId);

    /**
     * 计算指定学生、指定学期的成绩统计信息。
     *
     * @param semesterId 为 null 时不按学期过滤（统计全部）
     */
    ScoreStatisticsVO calculateStats(Long studentId, Long semesterId);
}
