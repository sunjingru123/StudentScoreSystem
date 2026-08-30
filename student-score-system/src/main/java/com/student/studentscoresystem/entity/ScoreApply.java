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
 * 当前系统：
 *
 * 1. 个人证书 / 个人获奖加分申报
 *
 * 部门活动加减分使用：
 * department_score_apply
 *
 * 与本表无关。
 *
 * @author 茹茹宝贝
 * @since 2026-08-05
 */
@Getter
@Setter
@TableName("score_apply")
public class ScoreApply implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(
            value = "id",
            type = IdType.AUTO
    )
    private Long id;

    /**
     * 学生ID
     *
     * 从当前登录用户获取。
     *
     * 学生不能自己提交 studentId。
     */
    private Long studentId;

    /**
     * 活动ID
     *
     * 个人证书申报不属于活动申报。
     *
     * 因此个人证书申请时为空。
     */
    private Long activityId;

    /**
     * 加分规则ID
     *
     * 个人证书申报时：
     *
     * 学生不选择规则。
     *
     * 档案部审核时根据获奖情况确定最终加分。
     *
     * 因此允许为空。
     */
    private Long ruleId;

    /**
     * 申请类型
     *
     * 当前系统：
     *
     * CERTIFICATE
     * = 个人证书 / 个人获奖加分申请
     *
     * 该字段由后端自动设置，
     * 学生不能自行提交。
     */
    private String applyType;

    /**
     * 最终申请分值
     *
     * 学生提交时为空。
     *
     * 档案部审核通过后，
     * 由档案部填写最终加分。
     */
    private BigDecimal applyScore;

    /**
     * 获奖凭证材料
     *
     * 有凭证：
     * 保存上传后的文件地址。
     *
     * 无凭证：
     * 为空。
     */
    private String materialFile;

    /**
     * 个人证书申报详细信息
     *
     * 当前 score_apply 表没有单独拆分：
     *
     * awardCategory
     * awardName
     * awardLevel
     * awardGrade
     * awardGradeOther
     * awardTime
     * awardType
     * hasCertificate
     * certificateReason
     * description
     *
     * 因此统一使用 JSON
     * 保存到 description 字段。
     *
     * 示例：
     *
     * {
     *   "awardCategory": "A",
     *   "awardName": "全国大学生XXX竞赛一等奖",
     *   "awardLevel": "国家级",
     *   "awardGrade": "一等奖",
     *   "awardGradeOther": "",
     *   "awardTime": "2026-07-01",
     *   "awardType": "个人奖",
     *   "hasCertificate": "YES",
     *   "certificateReason": "",
     *   "description": "其他补充说明"
     * }
     */
    private String description;

    /**
     * 审核状态
     *
     * 0 = 待审核
     * 1 = 审核通过
     * 2 = 审核驳回
     *
     * 学生提交后：
     * 直接进入档案部审核。
     */
    private Short status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}