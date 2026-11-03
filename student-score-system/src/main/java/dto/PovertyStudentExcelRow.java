package com.student.studentscoresystem.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 贫困生名单导入 Excel 行
 *
 * 列（固定顺序）：
 *
 * 学号 | 姓名
 *
 * 「学号」必填，用于唯一匹配 sys_user；
 * 「姓名」仅用于校验 / 提示，不参与匹配。
 */
@Data
public class PovertyStudentExcelRow {

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String realName;
}