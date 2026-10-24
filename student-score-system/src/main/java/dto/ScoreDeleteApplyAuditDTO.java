package com.student.studentscoresystem.dto;

import lombok.Data;

/**
 * =========================================================
 * 成绩删除申请审核参数
 * =========================================================
 *
 * 由辅导员（档案部负责辅导员 / 高级辅导员）审核。
 * =========================================================
 */
@Data
public class ScoreDeleteApplyAuditDTO {

    /**
     * 审核意见（驳回时建议填写）
     */
    private String remark;
}
