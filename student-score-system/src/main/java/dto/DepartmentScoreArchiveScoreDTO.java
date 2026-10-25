package com.student.studentscoresystem.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 档案部审核部门加减分申报时，调整申报分值的请求对象。
 *
 * 分值统一保存为正数，加分 / 减分由申报自身的 scoreType 决定。
 */
@Data
public class DepartmentScoreArchiveScoreDTO {

    /**
     * 调整后的分值，必须为正数
     */
    private BigDecimal score;
}
