package com.student.studentscoresystem.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ScoreApplyVO {

    /**
     * 申请ID
     */
    private Long id;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 申请类型
     *
     * 当前系统：
     * CERTIFICATE = 个人证书加分申请
     */
    private String applyType;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 申请分值
     *
     * 学生提交时为空
     * 档案部审核通过后填写
     */
    private BigDecimal applyScore;

    /**
     * 页面显示分值
     */
    private BigDecimal score;

    /**
     * 加减分类型
     *
     * 1 = 加分
     * -1 = 减分
     *
     * 个人证书申请固定为 1
     */
    private Integer scoreType;

    /**
     * 项目名称
     *
     * 个人证书申请显示获奖名称
     */
    private String title;

    /**
     * 部门名称
     *
     * 当前个人证书申请暂不使用
     */
    private String departmentName;

    /**
     * 材料文件
     */
    private String materialFile;

    /**
     * 审核状态
     *
     * 0 = 待审核
     * 1 = 通过
     * 2 = 驳回
     */
    private Integer status;

    /**
     * 最终审核状态
     *
     * 当前个人证书申请暂不单独使用
     */
    private Integer finalStatus;

    /**
     * 部门审核意见
     *
     * 当前个人证书申请暂不使用
     */
    private String reviewRemark;

    /**
     * 辅导员终审意见
     *
     * 当前个人证书申请暂不使用
     */
    private String finalReviewRemark;

    /**
     * 个人证书：获奖类别
     */
    private String awardCategory;

    /**
     * 个人证书：获奖名称
     */
    private String awardName;

    /**
     * 个人证书：获奖级别
     */
    private String awardLevel;

    /**
     * 个人证书：获奖等级
     */
    private String awardGrade;

    /**
     * 个人证书：其他获奖等级
     */
    private String awardGradeOther;

    /**
     * 个人证书：获奖时间
     */
    private String awardTime;

    /**
     * 个人证书：奖项性质
     *
     * PERSONAL = 个人奖
     * GROUP = 团体奖
     */
    private String awardType;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 是否有获奖凭证
     *
     * YES = 有
     * NO = 无
     */
    private String hasCertificate;

    /**
     * 无获奖凭证原因
     */
    private String certificateReason;

    /**
     * 申请说明
     */
    private String description;

    /**
     * 申报时间
     */
    private LocalDateTime createTime;
}