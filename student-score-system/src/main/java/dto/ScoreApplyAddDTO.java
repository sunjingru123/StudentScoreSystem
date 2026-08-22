package com.student.studentscoresystem.dto;


import lombok.Data;

import java.math.BigDecimal;


@Data
public class ScoreApplyAddDTO {


    private Long studentId;


    private Long activityId;


    private Long ruleId;


    private BigDecimal applyScore;


    private String materialFile;


    private String description;


}