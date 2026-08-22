package com.student.studentscoresystem.dto;

import lombok.Data;

@Data
public class DepartmentScoreFinalAuditDTO {

    /**
     * 1 = 通过
     * 2 = 驳回
     */
    private Short status;

    /**
     * 辅导员审核意见
     */
    private String reviewRemark;
}