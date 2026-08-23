package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("score_record")
public class ScoreRecord {

    @TableId(
            value = "id",
            type = IdType.AUTO
    )
    private Long id;

    /**
     * 成绩状态
     *
     * 1 = 有效
     * 0 = 作废
     */
    private Short status;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 管理员隐藏
     *
     * 0 = 正常显示
     * 1 = 隐藏
     */
    private Short adminHidden;

    /**
     * 加分规则ID
     */
    private Long ruleId;

    /**
     * 分数
     *
     * 正数 = 加分
     * 负数 = 减分
     */
    private BigDecimal score;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 来源类型
     *
     * CERTIFICATE
     * APPLY
     * ADMIN_ADJUSTMENT
     * DEPARTMENT_ACTIVITY
     */
    private String sourceType;

    /**
     * 来源业务ID
     */
    private Long sourceId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}