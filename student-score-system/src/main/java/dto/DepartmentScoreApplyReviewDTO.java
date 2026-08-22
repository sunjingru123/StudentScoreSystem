package com.student.studentscoresystem.dto;

import lombok.Data;

@Data
public class DepartmentScoreApplyReviewDTO {

    /**
     * 审核人
     */
    private Long reviewerId;

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