package com.student.studentscoresystem.controller;

import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.service.DepartmentScoreActivityImportService;
import com.student.studentscoresystem.vo.ActivityBatchPreviewVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 活动批量加分申报。
 *
 * 与原有「部门加减分申报」完全独立：
 * 独立模板、独立解析、独立导入，互不影响。
 *
 * 审批全部沿用系统现有线上流程：
 * 组织负责人 → 大社团 → 档案部 → 辅导员终审。
 */
@RestController
@RequestMapping("/departmentScoreActivity")
public class DepartmentScoreActivityController {

    private final DepartmentScoreActivityImportService importService;

    public DepartmentScoreActivityController(
            DepartmentScoreActivityImportService importService
    ) {

        this.importService = importService;
    }

    /**
     * 下载活动加分统计表模板。
     *
     * 直接下发学院在用的 Word 原表，不做任何改写；
     * 上传时系统自动识别该表结构。
     */
    @GetMapping("/template")
    public void template(
            HttpServletResponse response
    ) throws IOException {

        byte[] content =
                importService.buildTemplateDocument();

        String fileName =
                URLEncoder.encode(
                        "人工智能与计算机学院加分统计表.docx",
                        StandardCharsets.UTF_8
                ).replace("+", "%20");

        response.setContentType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );

        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        response.setHeader(
                "Content-Disposition",
                "attachment;filename*=UTF-8''" + fileName
        );

        response.setContentLength(content.length);

        response.getOutputStream().write(content);

        response.flushBuffer();
    }

    /**
     * 备用模板（Excel 版，与原表结构一致，同样可以导入）
     */
    @GetMapping("/template/xlsx")
    public void templateWorkbook(
            HttpServletResponse response
    ) throws IOException {

        byte[] content =
                importService.buildTemplateWorkbook();

        String fileName =
                URLEncoder.encode(
                        "活动批量加分申报模板.xlsx",
                        StandardCharsets.UTF_8
                ).replace("+", "%20");

        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );

        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        response.setHeader(
                "Content-Disposition",
                "attachment;filename*=UTF-8''" + fileName
        );

        response.setContentLength(content.length);

        response.getOutputStream().write(content);

        response.flushBuffer();
    }

    /**
     * 上传预览：只解析校验，不落库
     */
    @PostMapping("/preview")
    public Result<ActivityBatchPreviewVO> preview(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {

        Long userId = currentUserId(request);

        try {

            return Result.success(
                    importService.preview(
                            file,
                            userId
                    )
            );

        } catch (IllegalArgumentException e) {

            return Result.fail(e.getMessage());

        } catch (Exception e) {

            return Result.error(
                    "活动批量加分预览失败：" + e.getMessage()
            );
        }
    }

    /**
     * 导入：重新解析并保存有效学生记录
     */
    @PostMapping("/import")
    public Result<Map<String, Object>> importBatch(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {

        Long userId = currentUserId(request);

        try {

            return Result.success(
                    importService.importBatch(
                            file,
                            userId
                    )
            );

        } catch (IllegalArgumentException e) {

            return Result.fail(e.getMessage());

        } catch (Exception e) {

            return Result.error(
                    "活动批量加分导入失败：" + e.getMessage()
            );
        }
    }

    private Long currentUserId(
            HttpServletRequest request
    ) {

        Object userId =
                request.getAttribute("userId");

        if (userId == null) {

            return null;
        }

        try {

            return Long.valueOf(
                    userId.toString()
            );

        } catch (Exception e) {

            return null;
        }
    }
}
