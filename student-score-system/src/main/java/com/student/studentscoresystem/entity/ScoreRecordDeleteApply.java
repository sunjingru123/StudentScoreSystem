package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * =========================================================
 * 成绩删除申请
 * =========================================================
 *
 * 档案部部长 / 副部长删除学生加减分时，
 * 先提交一条删除申请，由辅导员（档案部负责辅导员 /
 * 高级辅导员）审核通过后，才真正执行逻辑删除。
 *
 * status：
 *   0 = 待审核
 *   1 = 已通过（审核人已执行删除）
 *   2 = 已驳回
 * =========================================================
 */
@Data
@TableName("score_record_delete_apply")
public class ScoreRecordDeleteApply {

    @TableId(
            value = "id",
            type = IdType.AUTO
    )
    private Long id;

    /**
     * 被申请删除的成绩记录
     */
    private Long scoreRecordId;

    /**
     * 该成绩所属学生
     */
    private Long studentId;

    /**
     * 提交申请的档案部成员
     */
    private Long applicantId;

    /**
     * 申请理由
     */
    private String reason;

    /**
     * 审核状态：0 待审核 / 1 已通过 / 2 已驳回
     */
    private Short status;

    /**
     * 审核人
     */
    private Long reviewerId;

    /**
     * 审核意见
     */
    private String reviewRemark;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
