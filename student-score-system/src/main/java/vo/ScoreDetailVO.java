package com.student.studentscoresystem.vo;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * 综合测评明细VO
 */
@Data
public class ScoreDetailVO {


    /**
     * 测评规则名称
     */
    private String ruleName;
    /**
     * 管理员隐藏状态
     * 0 = 正常
     * 1 = 已隐藏
     */
    private Short adminHidden;


    /**
     * 获得分数
     */
    private BigDecimal score;



    /**
     * 来源类型
     *
     * apply  自主申报
     * activity 活动
     * manual 手动录入
     */
    private String sourceType;



    /**
     * 来源ID
     */
    private Long sourceId;



    /**
     * 创建时间
     */
    private LocalDateTime createTime;


}