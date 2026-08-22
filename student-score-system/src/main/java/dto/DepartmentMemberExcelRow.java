package com.student.studentscoresystem.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class DepartmentMemberExcelRow {

    @ExcelProperty("部门")
    private String departmentName;

    @ExcelProperty("姓名")
    private String realName;

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("职位")
    private String position;
}