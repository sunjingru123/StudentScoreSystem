package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("score_record_operation_log")
public class ScoreRecordOperationLog {

    @TableId(
            value = "id",
            type = IdType.AUTO
    )
    private Long id;

    /**
     * 被操作的成绩记录
     */
    private Long scoreRecordId;

    /**
     * 操作人
     */
    private Long operatorId;

    /**
     * 操作类型
     *
     * HIDE   隐藏
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