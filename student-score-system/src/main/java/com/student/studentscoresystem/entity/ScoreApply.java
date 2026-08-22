package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 学生自主申报表
 * </p>
 *
 * @author 茹茹宝贝
 * @since 2026-08-05
 */
@Getter
@Setter
@TableName("score_apply")
public class ScoreApply implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long activityId;

    private Long ruleId;
    /**
     * 申请类型
     * CERTIFICATE：个人证书/个人荣誉，直接进入档案部审核
     * DEPARTMENT_ACTIVITY：部门活动加减分，进入部门审核流程
     */
    private String applyType;

    private BigDecimal applyScore;

    private String materialFile;

    private String description;

    private Short status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
