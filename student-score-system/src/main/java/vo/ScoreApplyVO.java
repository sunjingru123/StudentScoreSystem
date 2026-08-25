package com.student.studentscoresystem.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ScoreApplyVO {

    private Long id;

    private Long studentId;

    private String studentName;

    private String studentNo;

    private String ruleName;

    private BigDecimal applyScore;

    private Integer status;

    private LocalDateTime createTime;
}