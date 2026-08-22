package com.student.studentscoresystem.dto;


import lombok.Data;


@Data
public class ScoreApplyAuditDTO {


    /**
     * 申请id
     */
    private Long id;


    /**
     * 审核状态
     * 1 通过
     * 2 拒绝
     */
    private Short status;


}