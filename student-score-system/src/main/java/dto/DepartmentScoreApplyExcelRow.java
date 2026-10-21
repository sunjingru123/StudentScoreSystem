package com.student.studentscoresystem.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 部门加减分申报导入 Excel 行
 *
 * 列（固定顺序）：
 *
 * 学号 | 姓名 | 日期 | 项目 | 分值 | 备注
 *
 * 说明：
 *
 * 1. 「项目」列在模板中通过数据有效性下拉限制，
 *    只能选择当前部门已启用的加减分项目；
 * 2. 全部使用 String 接收，兼容 Excel 的文本单元格、
 *    数字单元格和日期单元格，单个单元格格式错误
 *    不会中断整份文件的读取；
 * 3. 具体类型（加分 / 减分）由所选项目决定，
 *    导入时不允许手动指定。
 */
@Data
public class DepartmentScoreApplyExcelRow {

    /**
     * 被加减分学生学号
     */
    @ExcelProperty("学号")
    private String studentNo;

    /**
     * 学生姓名，用于和学号交叉校验
     */
    @ExcelProperty("姓名")
    private String name;

    /**
     * 活动日期，支持 2024-05-01 / 2024/5/1 / 20240501
     */
    @ExcelProperty("日期")
    private String date;

    /**
     * 项目名称，必须来自模板下拉（本部门已启用项目）
     */
    @ExcelProperty("项目")
    private String project;

    /**
     * 分值，必须与所选项目的分值一致
     */
    @ExcelProperty("分值")
    private String score;

    /**
     * 备注，写入申报说明
     */
    @ExcelProperty("备注")
    private String remark;
}
