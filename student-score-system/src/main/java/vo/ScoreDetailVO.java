package com.student.studentscoresystem.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩明细 VO
 *
 * 用于：
 * 1. 学生成绩明细
 * 2. 管理员成绩明细
 * 3. 成绩明细分页
 */
public class ScoreDetailVO {

    /**
     * 成绩记录 ID
     */
    private Long id;

    /**
     * 来源业务 ID
     */
    private Long sourceId;

    /**
     * 成绩规则名称
     */
    private String ruleName;

    /**
     * 分数
     */
    private BigDecimal score;

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 管理员隐藏状态
     *
     * 0 = 正常
     * 1 = 隐藏
     */
    private Short adminHidden;


    // =========================================================
    // id
    // =========================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    // =========================================================
    // sourceId
    // =========================================================

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }


    // =========================================================
    // ruleName
    // =========================================================

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }


    // =========================================================
    // score
    // =========================================================

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }


    // =========================================================
    // sourceType
    // =========================================================

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }


    // =========================================================
    // createTime
    // =========================================================

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }


    // =========================================================
    // adminHidden
    // =========================================================

    public Short getAdminHidden() {
        return adminHidden;
    }

    public void setAdminHidden(Short adminHidden) {
        this.adminHidden = adminHidden;
    }
}