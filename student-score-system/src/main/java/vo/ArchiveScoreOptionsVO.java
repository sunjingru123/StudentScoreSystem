package com.student.studentscoresystem.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================
 * 档案部审核部门申报时，调整分值弹窗所需的参考信息
 *
 * 档案部可以自行填写任意大于 0 的分值，
 * suggestedScores 只是该项目正式评分规则里已配置的分值，
 * 供审核人参考，不构成强制约束。
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
     * 该项目正式规则里已配置的分值（升序去重，仅作参考）
     */
    private List<BigDecimal> suggestedScores =
            new ArrayList<>();
}
