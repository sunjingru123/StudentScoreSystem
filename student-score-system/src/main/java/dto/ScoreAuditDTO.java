package com.student.studentscoresystem.dto;


import lombok.Data;


@Data
public class ScoreAuditDTO {


    /**
     * 申请ID
     */
    private Long applyId;


    /**
     * 审核状态
     *
     * 1通过
     * 2驳回
     */
    private Short auditStatus;


    /**
     * 审核意见
     */
    private String auditComment;


}