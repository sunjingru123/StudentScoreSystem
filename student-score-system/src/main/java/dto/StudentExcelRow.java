package com.student.studentscoresystem.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class StudentExcelRow {

    @ExcelProperty("姓名")
    private String realName;

    @ExcelProperty("班级")
    private String className;

    @ExcelProperty("学号")
    private String studentNo;
}