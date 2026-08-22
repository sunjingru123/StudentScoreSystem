package com.student.studentscoresystem.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScoreAdminAdjustmentAddDTO {

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 1 加分
     * -1 减分
     */
    private Short adjustType;

    /**
     * 调整分值
     */
    private BigDecimal score;

    /**
     * 调整原因
     */
    private String reason;
}
