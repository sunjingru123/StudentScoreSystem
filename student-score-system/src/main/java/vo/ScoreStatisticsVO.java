package com.student.studentscoresystem.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ScoreStatisticsVO {

    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * 基础最高上限
     */
    private BigDecimal baseLimit;

    /**
     * 加分总和
     */
    private BigDecimal bonusScore;

    /**
     * 减分总和
     *
     * 使用正数表示扣掉的上限。
     * 例如实际记录 -5，这里返回 5。
     */
    private BigDecimal deductScore;

    /**
     * 当前实际最高上限
     *
     * 40 - deductScore
     */
    private BigDecimal actualLimit;

    /**
     * 最终综合成绩
     *
     * min(bonusScore, actualLimit)
     */
    private BigDecimal totalScore;

    /**
     * 平均分
     *
     * 保留这个字段兼容你原来的前端。
     */
    private Double avgScore;

    /**
     * 最高单项分
     */
    private Integer maxScore;

    /**
     * 最低单项分
     */
    private Integer minScore;

    /**
     * 成绩明细
     */
    private List<com.student.studentscoresystem.vo.ScoreDetailVO> detail;
}