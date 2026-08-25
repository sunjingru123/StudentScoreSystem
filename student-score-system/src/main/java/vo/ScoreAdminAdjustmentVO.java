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

    private Integer adjustType;

    private BigDecimal score;

    private String reason;

    private Long adminId;

    private String adminName;

    private LocalDateTime createTime;
}