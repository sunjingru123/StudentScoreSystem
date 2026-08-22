package com.student.studentscoresystem.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepartmentScoreApplyDTO {

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 1 加分
     * -1 减分
     */
    private Short scoreType;

    /**
     * 正数
     */
    private BigDecimal score;

    /**
     * 项目名称
     */
    private String title;

    /**
     * 项目说明
     */
    private String description;

    /**
     * 凭证地址
     */
    private String evidenceUrl;
}