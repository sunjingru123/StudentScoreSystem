package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("department_score_apply")
public class DepartmentScoreApply {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 被加分/扣分的学生
     */
    private Long studentId;

    /**
     * 申报人
     * 干事 / 副部长 / 部长
     */
    private Long applicantId;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 学生姓名，仅用于前端展示
     */
    @TableField(exist = false)
    private String studentName;

    /**
     * 申报人姓名，仅用于前端展示
     */
    @TableField(exist = false)
    private String applicantName;

    /**
     * 部门名称，仅用于前端展示
     */
    @TableField(exist = false)
    private String departmentName;

    /**
     * 所属部门
     */
    private Long departmentId;

    /**
     * 1 加分
     * -1 减分
     */
    private Short scoreType;

    /**
     * 申报分值，数据库保存正数
     */
    private BigDecimal score;

    /**
     * 申报标题
     */
    private String title;

    /**
     * 说明
     */
    private String description;

    /**
     * 证明材料
     */
    private String evidenceUrl;

    /**
     * 0 待审核
     * 1 审核通过
     * 2 审核驳回
     */
    private Short status;

    /**
     * 部门初审人
     */
    private Long reviewerId;

    /**
     * 部门初审意见
     */
    private String reviewRemark;

    /**
     * 部门初审时间
     */
    private LocalDateTime reviewTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 最终审核状态
     * 0 = 待辅导员审核
     * 1 = 辅导员审核通过
     * 2 = 辅导员审核驳回
     */
    private Short finalStatus;

    /**
     * 最终审核人
     */
    private Long finalReviewerId;

    /**
     * 最终审核意见
     */
    private String finalReviewRemark;

    /**
     * 最终审核时间
     */
    private LocalDateTime finalReviewTime;
}