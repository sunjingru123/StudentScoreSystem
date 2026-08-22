package com.student.studentscoresystem.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@TableName("score_record")
public class ScoreRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Short status;

    private Long studentId;

    // 数据库真实列 adminhidden，下划线转驼峰MyBatis‑Plus自动映射，不用写@TableField
    private Short adminHidden;

    private Long ruleId;
    private BigDecimal score;
    private Long semesterId;

    private String sourceType;
    private Long sourceId;

    private LocalDateTime createTime;
}