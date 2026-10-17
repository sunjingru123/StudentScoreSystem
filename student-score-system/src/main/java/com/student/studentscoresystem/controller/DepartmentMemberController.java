package com.student.studentscoresystem.controller;

import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.annotation.RequireRole;
import com.student.studentscoresystem.dto.DepartmentMemberAddDTO;
import com.student.studentscoresystem.service.DepartmentMemberImportService;
import com.student.studentscoresystem.service.DepartmentMemberManageService;
import com.student.studentscoresystem.vo.DepartmentMemberDetailVO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/department-member")
public class DepartmentMemberController {

    private final DepartmentMemberImportService importService;

    private final DepartmentMemberManageService manageService;

    public DepartmentMemberController(
            DepartmentMemberImportService importService,
            DepartmentMemberManageService manageService
    ) {
        this.importService = importService;
        this.manageService = manageService;
    }

    /**
     * 导入部门成员 Excel
     */
    @PostMapping("/import")
    @RequireRole("管理员")
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

    /**
     * 查询某部门的成员列表
     *
     * GET /department-member/department/{departmentId}
     */
    @GetMapping("/department/{departmentId}")
    @RequireRole("管理员")
    public Result<List<DepartmentMemberDetailVO>> listMembers(
            @PathVariable Long departmentId
    ) {

        return Result.success(
                manageService.listMembers(
                        departmentId
                )
        );
    }

    /**
     * 向部门新增一名成员
     *
     * POST /department-member/department/{departmentId}
     */
    @PostMapping("/department/{departmentId}")
    @RequireRole("管理员")
    public Result<Void> addMember(
            @PathVariable Long departmentId,
            @RequestBody DepartmentMemberAddDTO body
    ) {

        try {

            manageService.addMember(
                    departmentId,
                    body == null ? null : body.getUserId(),
                    body == null ? null : body.getPosition()
            );

            return Result.success(null);

        } catch (IllegalArgumentException e) {

            return Result.fail(e.getMessage());
        }
    }

    /**
     * 移出部门成员
     *
     * DELETE /department-member/{memberId}
     */
    @DeleteMapping("/{memberId}")
    @RequireRole("管理员")
    public Result<Void> removeMember(
            @PathVariable Long memberId
    ) {

        try {

            manageService.removeMember(
                    memberId
            );

            return Result.success(null);

        } catch (IllegalArgumentException e) {

            return Result.fail(e.getMessage());
        }
    }
}
