package com.student.studentscoresystem.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 评分项目（规则）导入 Excel 行
 *
 * 列：
 *
 * 部门 | 规则名称 | 分类 | 分值 | 描述 | 状态
 */
@Data
public class ScoreRuleExcelRow {

    @ExcelProperty("部门")
    private String departmentName;

    @ExcelProperty("规则名称")
    private String name;

    @ExcelProperty("分类")
    private String category;

    /**
     * 使用 String 接收，
     * 兼容 Excel 中的文本单元格和数字单元格，
     * 单个单元格格式错误时不会中断整份文件的读取。
     */
    @ExcelProperty("分值")
    private String score;

    @ExcelProperty("描述")
    private String description;

    /**
     * 启用 / 停用，
     * 留空默认为启用
     */
    @ExcelProperty("状态")
    private String status;
}
