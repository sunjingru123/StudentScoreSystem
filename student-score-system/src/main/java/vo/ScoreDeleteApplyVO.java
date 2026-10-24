package com.student.studentscoresystem.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * =========================================================
 * 成绩删除申请展示对象
 * =========================================================
 *
 * 同时用于：
 *   1. 档案部成员查看「我的删除申请」；
 *   2. 辅导员查看「待我审核 / 我已处理」的删除申请。
 * =========================================================
 */
@Data
public class ScoreDeleteApplyVO {

    private Long id;

    /**
     * 成绩记录ID
     */
    private Long scoreRecordId;

    /**
     * 学生信息
     */
    private Long studentId;

    private String studentName;

    private String studentNo;

    private String className;

    /**
     * 成绩信息
     */
    private BigDecimal score;

    private String sourceType;

    private String sourceName;

    /**
     * 成绩项目名称（规则 / 证书 / 部门申报标题）
     */
    private String projectName;

    private LocalDateTime recordCreateTime;

    private Short recordStatus;

    private Short recordAdminHidden;

    /**
     * 申请人
     */
    private Long applicantId;

    private String applicantName;

    private String reason;

    /**
     * 审核状态：0 待审核 / 1 已通过 / 2 已驳回
     */
    private Short status;

    private String statusText;

    /**
     * 审核人
     */
    private Long reviewerId;

    private String reviewerName;

    private String reviewRemark;

    private LocalDateTime reviewTime;

    private LocalDateTime createTime;
}
