package com.student.studentscoresystem.vo;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
public class ScoreApplyVO {

    private Long id;

    private Long studentId;

    private String studentName;

    private String activityName;

    private String ruleName;

    private String applyType;

    private BigDecimal applyScore;

    private String materialFile;

    private String description;

    /**
     * 0 待审核
     * 1 已通过
     * 2 已拒绝
     */
    private Short status;

    private LocalDateTime createTime;
}