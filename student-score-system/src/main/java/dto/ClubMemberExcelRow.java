package com.student.studentscoresystem.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 社团成员导入行。
 *
 * Excel 列：
 *
 * 大社团名称 | 小社团名称 | 姓名 | 学号 | 职位
 *
 * 约定：
 *
 * 1. 大社团名称留空时，继承上一行的大社团；
 * 2. 小社团名称留空时，表示该成员直接属于大社团
 *    （例如理事长 / 副理事长 / 主席 / 副主席）；
 * 3. 小社团名称不为空时，表示该成员属于该小社团。
 */
@Data
public class ClubMemberExcelRow {

    @ExcelProperty("大社团名称")
    private String bigClubName;

    @ExcelProperty("小社团名称")
    private String smallClubName;

    @ExcelProperty("姓名")
    private String realName;

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("职位")
    private String position;
}
