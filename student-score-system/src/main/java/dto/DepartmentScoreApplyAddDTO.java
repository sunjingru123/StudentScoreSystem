package com.student.studentscoresystem.dto;

import lombok.Data;

@Data
public class DepartmentScoreApplyAddDTO {
    /**
     * 学期ID
     */
    private Long semesterId;
    /**
     * 申报部门
     */
    private Long departmentId;

    /**
     * 被加减分学生
     */
    private Long studentId;

    /**
     * 部门加减分模板
     */
    private Long templateId;

    /**
     * 证明材料地址
     */
    private String evidenceUrl;
}