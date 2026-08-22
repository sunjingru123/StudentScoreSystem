package com.student.studentscoresystem.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminScoreDetailVO {

    /**
     * 成绩记录ID
     */
    private Long id;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 班级
     */
    private String className;

    /**
     * 评分项目名称
     */
    private String ruleName;

    /**
     * 分数
     */
    private BigDecimal score;

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 来源ID
     */
    private Long sourceId;

    /**
     * 管理员隐藏状态
     *
     * 0 = 正常
     * 1 = 管理员隐藏
     */
    private Short adminHidden;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}