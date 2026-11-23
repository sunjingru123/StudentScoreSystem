package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
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
     * 活动日期
     *
     * 单条申报可以为空；
     * Excel 批量导入时由导入模板的「日期」列写入。
     */
    private LocalDate activityDate;

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

    /**
     * 档案部审核状态
     * 0 = 待档案部审核
     * 1 = 档案部审核通过
     * 2 = 档案部审核驳回
     */
    private Short archiveStatus;

    /**
     * 档案部审核人（档案部部长 / 副部长）
     */
    private Long archiveReviewerId;

    /**
     * 档案部审核意见
     */
    private String archiveReviewRemark;

    /**
     * 档案部审核时间
     */
    private LocalDateTime archiveReviewTime;

    /**
     * 大社团审核状态（仅小社团申报使用）
     *
     * null = 不经过大社团审核（部门 / 大社团负责人提交）
     * 0    = 待大社团审核
     * 1    = 大社团审核通过
     * 2    = 大社团驳回
     */
    private Short bigClubStatus;

    /**
     * 大社团审核人
     */
    private Long bigClubReviewerId;

    /**
     * 大社团审核意见
     */
    private String bigClubReviewRemark;

    /**
     * 大社团审核时间
     */
    private LocalDateTime bigClubReviewTime;

    /**
     * 当前审核阶段（非数据库字段，仅用于前端展示）
     *
     * SMALL_CLUB = 待小社团 / 部门初审
     * BIG_CLUB   = 待大社团审核
     */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String reviewStage;
}