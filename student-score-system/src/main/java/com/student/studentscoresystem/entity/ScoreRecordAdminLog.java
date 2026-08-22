package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("score_record_admin_log")
public class ScoreRecordAdminLog {

    private Long id;

    /**
     * 被操作的成绩记录
     */
    private Long scoreRecordId;

    /**
     * 操作管理员
     */
    private Long adminId;

    /**
     * VOID 作废
     * RESTORE 恢复
     */
    private String operation;

    /**
     * 操作原因
     */
    private String reason;

    /**
     * 操作时间
     */
    private LocalDateTime createTime;
}