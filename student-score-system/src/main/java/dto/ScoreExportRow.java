package com.student.studentscoresystem.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 加减分导出明细
 */
@Data
public class ScoreExportRow {

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 学生姓名
     */
    private String realName;

    /**
     * 班级
     */
    private String className;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 学期名称
     */
    private String semesterName;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 来源业务ID
     */
    private Long sourceId;

    /**
     * 分值
     *
     * 正数 = 加分
     * 负数 = 减分
     */
    private BigDecimal score;

    /**
     * 部门申报标题
     */
    private String departmentApplyTitle;

    /**
     * 部门申报说明
     */
    private String departmentApplyDescription;
}