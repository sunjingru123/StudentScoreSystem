package com.student.studentscoresystem.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@TableName("score_flow")
public class ScoreFlow {


    @TableId(
            value = "id",
            type = IdType.AUTO
    )
    private Long id;


    /**
     * 学生id
     */
    private Long studentId;


    /**
     * 变化分数
     */
    private BigDecimal changeScore;


    /**
     * 修改前
     */
    private BigDecimal beforeScore;


    /**
     * 修改后
     */
    private BigDecimal afterScore;


    /**
     * 类型
     *
     * apply
     * modify
     * delete
     */
    private String changeType;


    /**
     * 描述
     */
    private String description;


    private LocalDateTime createTime;

}