package com.student.studentscoresystem.controller;

import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.service.DepartmentMemberImportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/department-member")
public class DepartmentMemberController {

    private final DepartmentMemberImportService importService;

    public DepartmentMemberController(
            DepartmentMemberImportService importService
    ) {
        this.importService = importService;
    }

    /**
     * 导入部门成员 Excel
     */
    @PostMapping("/import")
    public Result<Map<String, Object>> importExcel(
            @RequestParam("file") MultipartFile file
    ) {

        try {

            DepartmentMemberImportService.ImportResult result =
                    importService.importExcel(file);

            Map<String, Object> data = new HashMap<>();

            data.put(
                    "successCount",
                    result.getSuccessCount()
            );

            data.put(
                    "newCount",
                    result.getNewCount()
            );

            data.put(
                    "updateCount",
                    result.getUpdateCount()
            );

            data.put(
                    "errorCount",
                    result.getErrorCount()
            );

            data.put(
                    "errors",
                    result.getErrors()
            );

            if (result.getErrorCount() > 0) {

                return Result.success(data);
            }

            return Result.success(data);

        } catch (IllegalArgumentException e) {

            return Result.fail(e.getMessage());

        } catch (Exception e) {

            return Result.fail(
                    "Excel 导入失败：" + e.getMessage()
            );
        }
    }
}