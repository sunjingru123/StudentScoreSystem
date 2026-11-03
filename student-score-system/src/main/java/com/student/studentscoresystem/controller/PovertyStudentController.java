package com.student.studentscoresystem.controller;

import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.service.PovertyImportPermissionService;
import com.student.studentscoresystem.service.PovertyStudentImportService;
import com.student.studentscoresystem.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * =========================================================
 * 贫困生管理
 * =========================================================
 *
 * 辅导员端（指定辅导员）导入贫困生名单，
 * 用于后续按「贫困生 / 非贫困生」筛选学生参加的比赛。
 *
 * 所有权限均以后端判定为准，前端只控制按钮显示。
 * =========================================================
 */
@RestController
@RequestMapping("/poverty")
public class PovertyStudentController {

    private final PovertyStudentImportService povertyStudentImportService;

    private final PovertyImportPermissionService povertyImportPermissionService;

    public PovertyStudentController(
            PovertyStudentImportService povertyStudentImportService,
            PovertyImportPermissionService povertyImportPermissionService
    ) {

        this.povertyStudentImportService =
                povertyStudentImportService;

        this.povertyImportPermissionService =
                povertyImportPermissionService;
    }

    /**
     * 当前用户是否可以导入贫困生名单
     */
    @GetMapping("/permission")
    public Result<Map<String, Object>> permission(
            HttpServletRequest request
    ) {

        Long currentUserId;

        try {

            currentUserId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(
                    e.getMessage()
            );
        }


        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "canImport",
                povertyImportPermissionService.canImportPovertyList(
                        currentUserId
                )
        );

        return Result.success(
                data
        );
    }

    /**
     * 下载贫困生名单导入模板
     */
    @GetMapping("/template")
    public void template(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        Long currentUserId;

        try {

            currentUserId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            writeDownloadError(
                    response,
                    e.getMessage()
            );

            return;
        }


        byte[] content;

        try {

            content =
                    povertyStudentImportService
                            .buildTemplateWorkbook(
                                    currentUserId
                            );

        } catch (IllegalArgumentException e) {

            writeDownloadError(
                    response,
                    e.getMessage()
            );

            return;
        } catch (SecurityException e) {

            writeDownloadError(
                    response,
                    e.getMessage()
            );

            return;
        }


        String fileName =
                URLEncoder.encode(
                        "贫困生名单导入模板.xlsx",
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

        response.setContentLength(
                content.length
        );

        response.getOutputStream().write(
                content
        );

        response.flushBuffer();
    }

    /**
     * 导入贫困生名单
     */
    @PostMapping("/import")
    public Result<Map<String, Object>> importPovertyStudents(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {

        Long currentUserId;

        try {

            currentUserId =
                    getCurrentUserId(request);

        } catch (Exception e) {

            return Result.fail(
                    e.getMessage()
            );
        }


        try {

            return Result.success(
                    povertyStudentImportService
                            .importPovertyStudents(
                                    currentUserId,
                                    file
                            )
            );

        } catch (IllegalArgumentException e) {

            return Result.fail(
                    e.getMessage()
            );

        } catch (SecurityException e) {

            return Result.fail(
                    e.getMessage()
            );
        }
    }

    /**
     * 下载失败时返回 JSON，
     * 前端 blob 请求可以提示真实原因。
     */
    private void writeDownloadError(
            HttpServletResponse response,
            String message
    ) throws IOException {

        response.reset();

        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        response.setContentType(
                "application/json;charset=UTF-8"
        );

        String safeMessage =
                message == null
                        ? "下载导入模板失败"
                        : message
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"");

        response.getWriter().write(
                "{\"code\":400,\"message\":\""
                        + safeMessage
                        + "\",\"data\":null}"
        );
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId(
            HttpServletRequest request
    ) {

        String token =
                request.getHeader("Authorization");

        if (token == null
                || !token.startsWith("Bearer ")) {

            throw new IllegalArgumentException(
                    "请先登录"
            );
        }

        try {

            Claims claims =
                    JwtUtil.parseToken(
                            token.substring(7)
                    );

            Object userId =
                    claims.get("userId");

            if (userId == null) {

                throw new IllegalArgumentException(
                        "登录状态无效"
                );
            }

            return Long.valueOf(
                    userId.toString()
            );

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "登录状态无效，请重新登录"
            );
        }
    }
}