package com.student.studentscoresystem.dto;

import lombok.Data;

/**
 * =========================================================
 * 成绩删除申请提交参数
 * =========================================================
 *
 * 由档案部部长 / 副部长提交。
 * =========================================================
 */
@Data
public class ScoreDeleteApplySubmitDTO {

    /**
     * 被申请删除的成绩记录ID
     */
    private Long recordId;

    /**
     * 申请理由
     */
    private String reason;
}
