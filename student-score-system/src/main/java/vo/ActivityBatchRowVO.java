package com.student.studentscoresystem.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 活动批量加分申报：学生明细行（预览/校验结果）。
 */
@Data
public class ActivityBatchRowVO {

    /**
     * Excel 行号（从 1 开始，便于用户定位）
     */
    private int rowNumber;

    private String realName;

    private String className;

    private String studentNo;

    /**
     * 系统内学生 ID（校验通过后回填，不对外使用）
     */
    @JsonIgnore
    private Long studentId;

    private BigDecimal score;

    private String evidence;

    /**
     * 校验错误；为空表示该行可以导入
     */
    private String error;
}
