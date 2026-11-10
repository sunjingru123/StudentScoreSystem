package com.student.studentscoresystem.controller;

import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.annotation.RequireRole;
import com.student.studentscoresystem.service.ExcelImportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Excel 数据导入控制器
 */
@RestController
@RequestMapping("/excel/import")
public class ExcelImportController {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

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
    @RequireRole("管理员")
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
    @RequireRole("管理员")
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

    /**
     * 下载学生名单导入模板
     *
     * GET /excel/import/students/template
     */
    @GetMapping("/students/template")
    @RequireRole("管理员")
    public void downloadStudentTemplate(
            HttpServletResponse response
    ) throws IOException {

        writeWorkbook(
                response,
                excelImportService
                        .buildStudentTemplateWorkbook(),
                "学生名单导入模板.xlsx"
        );
    }

    /**
     * 下载部门成员名单导入模板
     *
     * GET /excel/import/department-members/template
     */
    @GetMapping("/department-members/template")
    @RequireRole("管理员")
    public void downloadDepartmentMemberTemplate(
            HttpServletResponse response
    ) throws IOException {

        writeWorkbook(
                response,
                excelImportService
                        .buildDepartmentMemberTemplateWorkbook(),
                "部门成员名单导入模板.xlsx"
        );
    }

    /**
     * =========================================================
     * 输出 xlsx 下载响应
     * =========================================================
     *
     * 模板内容先完整生成再写响应头，
     * 生成失败时由全局异常处理返回 JSON，
     * 不会出现“已经给人下载了半个文件”的情况。
     */
    private void writeWorkbook(
            HttpServletResponse response,
            byte[] content,
            String fileName
    ) throws IOException {

        String encodedFileName =
                URLEncoder.encode(
                        fileName,
                        StandardCharsets.UTF_8
                ).replace("+", "%20");

        response.setContentType(
                XLSX_CONTENT_TYPE
        );

        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        response.setHeader(
                "Content-Disposition",
                "attachment;filename*=UTF-8''" + encodedFileName
        );

        response.setContentLength(
                content.length
        );

        response.getOutputStream().write(
                content
        );

        response.flushBuffer();
    }
}
