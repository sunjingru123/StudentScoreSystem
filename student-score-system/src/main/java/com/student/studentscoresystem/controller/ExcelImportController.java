package com.student.studentscoresystem.controller;

import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.service.ExcelImportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Excel 数据导入控制器
 */
@RestController
@RequestMapping("/excel/import")
public class ExcelImportController {

    private final ExcelImportService excelImportService;

    public ExcelImportController(
            ExcelImportService excelImportService
    ) {
        this.excelImportService = excelImportService;
    }

    /**
     * 导入学生名单
     *
     * POST /excel/import/students
     */
    @PostMapping("/students")
    public Result<Map<String, Object>> importStudents(
            @RequestParam("file") MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要导入的 Excel 文件");
        }

        try {

            Map<String, Object> result =
                    excelImportService.importStudents(file);

            return Result.success(result);

        } catch (Exception e) {

            return Result.fail(
                    "学生名单导入失败：" + e.getMessage()
            );
        }
    }

    /**
     * 导入部门成员
     *
     * POST /excel/import/department-members
     */
    @PostMapping("/department-members")
    public Result<Map<String, Object>> importDepartmentMembers(
            @RequestParam("file") MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要导入的 Excel 文件");
        }

        try {

            Map<String, Object> result =
                    excelImportService.importDepartmentMembers(file);

            return Result.success(result);

        } catch (Exception e) {

            return Result.fail(
                    "部门成员导入失败：" + e.getMessage()
            );
        }
    }
}