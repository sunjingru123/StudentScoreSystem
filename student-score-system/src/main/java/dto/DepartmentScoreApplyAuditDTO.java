package com.student.studentscoresystem.dto;

import lombok.Data;

@Data
public class DepartmentScoreApplyAuditDTO {

    /**
     * 1 通过
     * 2 驳回
     */
    private Short status;

    /**
     * 审核意见
     */
    private String reviewRemark;
}