package com.student.studentscoresystem.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================
 * 档案部调整部门申报分值时的可选用值
 *
 * restricted = true 表示该项目在当前部门存在正式加减分规则，
 * 调整后的分值只能从 allowedScores 中选择：
 * 辅导员终审按「部门 + 名称 + 分值 + 类型」匹配正式规则，
 * 若分值落在规则之外，终审会因
 * 「部门申报分值与正式加减分规则不一致」而无法生成成绩。
 *
 * restricted = false 表示该项目没有正式规则（部门自建活动），
 * 分值以申报单上填写的为准，可以自由填写正数。
 * =========================================================
 */
@Data
public class ArchiveScoreOptionsVO {

    /**
     * 1 = 加分
     * -1 = 减分
     */
    private Short scoreType;

    /**
     * 当前申报分值
     */
    private BigDecimal currentScore;

    /**
     * 是否只能从正式规则分值中选择
     */
    private boolean restricted;

    /**
     * 允许调整的分值（升序去重）
     */
    private List<BigDecimal> allowedScores =
            new ArrayList<>();
}
