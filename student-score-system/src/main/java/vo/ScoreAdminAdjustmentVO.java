package com.student.studentscoresystem.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ScoreAdminAdjustmentVO {

    private Long id;

    private Long studentId;

    private String studentName;

    private String studentNo;

    private Long adminId;

    private String adminName;

    /**
     * 1 加分
     * -1 减分
     */
    private Short adjustType;

    private BigDecimal score;

    private String reason;

    private LocalDateTime createTime;
}