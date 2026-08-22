package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("score_record_operation_log")
public class ScoreRecordOperationLog {

    private Long id;

    /**
     * 成绩记录ID
     */
    private Long scoreRecordId;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * HIDE / RESTORE
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