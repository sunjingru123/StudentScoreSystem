package com.student.studentscoresystem.controller;

import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.FileInfo;
import com.student.studentscoresystem.mapper.FileInfoMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/file")
public class FileInfoController {

    private final FileInfoMapper fileInfoMapper;

    /**
     * 文件上传目录
     */
    private final Path uploadPath =
            Paths.get("uploads");


    public FileInfoController(
            FileInfoMapper fileInfoMapper
    ) {

        this.fileInfoMapper =
                fileInfoMapper;

    }


    /**
     * ========================================================
     * 上传文件
     * ========================================================
     *
     * POST /file/upload
     *
     * 前端：
     *
     * <el-upload
     *     action="/api/file/upload"
     *     name="file"
     * >
     *
     * ========================================================
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {

        /*
         * =====================================================
         * 1. 获取登录用户 ID
         * =====================================================
         */

        Object userIdObj =
                request.getAttribute("userId");


        if (
                userIdObj == null
        ) {

            return Result.error(
                    "请先登录"
            );

        }


        Long uploaderId;

        try {

            uploaderId =
                    Long.parseLong(
                            userIdObj.toString()
                    );

        } catch (
                Exception e
        ) {

            return Result.error(
                    "登录用户信息无效"
            );

        }


        /*
         * =====================================================
         * 2. 检查文件
         * =====================================================
         */

        if (
                file == null ||
                        file.isEmpty()
        ) {

            return Result.error(
                    "上传文件不能为空"
            );

        }


        /*
         * =====================================================
         * 3. 检查文件大小
         * =====================================================
         */

        long maxSize =
                10L * 1024 * 1024;


        if (
                file.getSize() >
                        maxSize
        ) {

            return Result.error(
                    "文件大小不能超过10MB"
            );

        }


        /*
         * =====================================================
         * 4. 获取原始文件名
         * =====================================================
         */

        String originalFilename =
                file.getOriginalFilename();


        if (
                originalFilename == null ||
                        originalFilename.isBlank()
        ) {

            return Result.error(
                    "文件名称无效"
            );

        }


        /*
         * =====================================================
         * 5. 检查扩展名
         * =====================================================
         */

        String lowerName =
                originalFilename.toLowerCase();


        boolean extensionAllowed =
                lowerName.endsWith(".pdf")
                        ||
                        lowerName.endsWith(".jpg")
                        ||
                        lowerName.endsWith(".jpeg")
                        ||
                        lowerName.endsWith(".png");


        if (
                !extensionAllowed
        ) {

            return Result.error(
                    "只允许上传 PDF、JPG、PNG 文件"
            );

        }


        /*
         * =====================================================
         * 6. 创建 uploads 目录
         * =====================================================
         */

        try {

            Files.createDirectories(
                    uploadPath
            );

        } catch (
                IOException e
        ) {

            e.printStackTrace();

            return Result.error(
                    "无法创建文件上传目录"
            );

        }


        /*
         * =====================================================
         * 7. 获取扩展名
         * =====================================================
         */

        String extension =
                "";


        int dotIndex =
                originalFilename.lastIndexOf(".");


        if (
                dotIndex >= 0
        ) {

            extension =
                    originalFilename.substring(
                            dotIndex
                    );

        }


        /*
         * =====================================================
         * 8. 生成唯一文件名
         * =====================================================
         */

        String newFileName =
                UUID.randomUUID()
                        .toString()
                        .replace(
                                "-",
                                ""
                        )
                        +
                        extension;


        /*
         * =====================================================
         * 9. 保存文件
         * =====================================================
         */

        Path targetPath =
                uploadPath.resolve(
                        newFileName
                );


        try {

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (
                IOException e
        ) {

            e.printStackTrace();

            return Result.error(
                    "文件保存失败：" +
                            e.getMessage()
            );

        }


        /*
         * =====================================================
         * 10. 保存数据库记录
         * =====================================================
         */

        FileInfo fileInfo =
                new FileInfo();


        fileInfo.setFileName(
                originalFilename
        );


        String contentType =
                file.getContentType();


        fileInfo.setFileType(
                contentType != null
                        ? contentType
                        : extension
        );


        fileInfo.setFileSize(
                file.getSize()
        );


        /*
         * =====================================================
         * 注意：
         *
         * 数据库里面保存相对路径
         *
         * uploads/xxxx.pdf
         *
         * =====================================================
         */

        String relativePath =
                uploadPath
                        .resolve(
                                newFileName
                        )
                        .toString()
                        .replace(
                                "\\",
                                "/"
                        );


        fileInfo.setFilePath(
                relativePath
        );


        fileInfo.setUploaderId(
                uploaderId
        );


        fileInfo.setBusinessType(
                "CERTIFICATE"
        );


        fileInfo.setCreateTime(
                LocalDateTime.now()
        );


        try {

            fileInfoMapper.insert(
                    fileInfo
            );

        } catch (
                Exception e
        ) {

            e.printStackTrace();


            /*
             * 数据库失败
             * 删除刚才保存的文件
             */

            try {

                Files.deleteIfExists(
                        targetPath
                );

            } catch (
                    IOException ignored
            ) {

            }


            return Result.error(
                    "文件信息保存失败：" +
                            e.getMessage()
            );

        }


        /*
         * =====================================================
         * 11. 生成访问地址
         * =====================================================
         */

        String fileUrl =
                "/api/file/view/" +
                        fileInfo.getId();


        /*
         * =====================================================
         * 12. 构造返回数据
         * =====================================================
         */

        Map<String, Object> data =
                new HashMap<>();


        data.put(
                "id",
                fileInfo.getId()
        );


        data.put(
                "fileName",
                fileInfo.getFileName()
        );


        data.put(
                "fileType",
                fileInfo.getFileType()
        );


        data.put(
                "fileSize",
                fileInfo.getFileSize()
        );


        data.put(
                "filePath",
                fileInfo.getFilePath()
        );


        data.put(
                "url",
                fileUrl
        );


        /*
         * =====================================================
         * 13. 返回统一 Result
         * =====================================================
         */

        return Result.success(
                data
        );

    }


    /**
     * ========================================================
     * 查看文件
     * ========================================================
     *
     * GET /file/view/{id}
     *
     * ========================================================
     */
    @GetMapping("/view/{id}")
    public ResponseEntity<Resource> view(
            @PathVariable Long id
    ) {

        /*
         * =====================================================
         * 1. 查询文件
         * =====================================================
         */

        FileInfo fileInfo =
                fileInfoMapper.selectById(
                        id
                );


        if (
                fileInfo == null
        ) {

            return ResponseEntity
                    .notFound()
                    .build();

        }


        /*
         * =====================================================
         * 2. 获取文件路径
         * =====================================================
         */

        Path path =
                Paths.get(
                        fileInfo.getFilePath()
                );


        Resource resource;


        try {

            resource =
                    new UrlResource(
                            path.toUri()
                    );

        } catch (
                MalformedURLException e
        ) {

            return ResponseEntity
                    .notFound()
                    .build();

        }


        /*
         * =====================================================
         * 3. 判断文件是否存在
         * =====================================================
         */

        if (
                !resource.exists() ||
                        !resource.isReadable()
        ) {

            return ResponseEntity
                    .notFound()
                    .build();

        }


        /*
         * =====================================================
         * 4. 判断文件类型
         * =====================================================
         */

        MediaType mediaType =
                MediaType.APPLICATION_OCTET_STREAM;


        try {

            String detectedType =
                    Files.probeContentType(
                            path
                    );


            if (
                    detectedType != null
            ) {

                mediaType =
                        MediaType.parseMediaType(
                                detectedType
                        );

            }

        } catch (
                Exception ignored
        ) {

        }


        /*
         * =====================================================
         * 5. 返回文件
         * =====================================================
         */

        return ResponseEntity
                .ok()
                .contentType(
                        mediaType
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" +
                                fileInfo.getFileName() +
                                "\""
                )
                .body(
                        resource
                );

    }

}