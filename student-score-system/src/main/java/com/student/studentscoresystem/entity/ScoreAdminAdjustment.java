package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("score_admin_adjustment")
public class ScoreAdminAdjustment {

    @TableId(
            value = "id",
            type = IdType.AUTO
    )
    private Long id;

    /**
     * 被调整的学生
     */
    private Long studentId;

    /**
     * 操作管理员
     */
    private Long adminId;

    /**
     * 1 = 加分
     * -1 = 减分
     */
    private Short adjustType;

    /**
     * 调整分值
     */
    private BigDecimal score;

    /**
     * 调整原因
     */
    private String reason;

    /**
     * 操作时间
     */
    private LocalDateTime createTime;
}