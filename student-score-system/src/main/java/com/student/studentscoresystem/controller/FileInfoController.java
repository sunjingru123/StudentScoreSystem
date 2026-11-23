package com.student.studentscoresystem.controller;

import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.entity.FileInfo;
import com.student.studentscoresystem.entity.ScoreApply;
import com.student.studentscoresystem.mapper.DepartmentScoreApplyMapper;
import com.student.studentscoresystem.mapper.FileInfoMapper;
import com.student.studentscoresystem.mapper.ScoreApplyMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/file")
public class FileInfoController {

    private final FileInfoMapper fileInfoMapper;
    private final SysUserPositionMapper userPositionMapper;
    private final SysPositionMapper positionMapper;

    /**
     * 用于判断「文件是否附在本人申报单上」，
     * 让学生能看到自己申报所附的凭证材料。
     */
    private final DepartmentScoreApplyMapper departmentScoreApplyMapper;

    private final ScoreApplyMapper scoreApplyMapper;

    private static final Logger log =
            LoggerFactory.getLogger(FileInfoController.class);

    /**
     * 文件上传目录
     */
    private final Path uploadPath =
            Paths.get("uploads");


    public FileInfoController(
            FileInfoMapper fileInfoMapper,
            SysUserPositionMapper userPositionMapper,
            SysPositionMapper positionMapper,
            DepartmentScoreApplyMapper departmentScoreApplyMapper,
            ScoreApplyMapper scoreApplyMapper
    ) {

        this.fileInfoMapper =
                fileInfoMapper;
        this.userPositionMapper = userPositionMapper;
        this.positionMapper = positionMapper;
        this.departmentScoreApplyMapper = departmentScoreApplyMapper;
        this.scoreApplyMapper = scoreApplyMapper;

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


        /*
         * 凭证材料支持：
         *   PDF、图片（jpg / jpeg / png）、
         *   Word 文档（doc / docx）
         */

        boolean extensionAllowed =
                lowerName.endsWith(".pdf")
                        ||
                        lowerName.endsWith(".jpg")
                        ||
                        lowerName.endsWith(".jpeg")
                        ||
                        lowerName.endsWith(".png")
                        ||
                        lowerName.endsWith(".doc")
                        ||
                        lowerName.endsWith(".docx");


        if (
                !extensionAllowed
        ) {

            return Result.error(
                    "只允许上传 PDF、JPG、PNG、Word（doc / docx）文件"
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
    public ResponseEntity<?> view(
            @PathVariable Long id,
            HttpServletRequest request
    ) {

        Object userIdObj = request.getAttribute("userId");
        if (userIdObj == null) {
            return jsonError(401, "请先登录后再查看材料");
        }
        Long currentUserId;
        try {
            currentUserId = Long.valueOf(userIdObj.toString());
        } catch (NumberFormatException ex) {
            return jsonError(401, "登录用户信息无效，请重新登录");
        }

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

            return jsonError(404, "材料不存在（文件记录已丢失）");

        }

        /*
         * =====================================================
         * 查看权限：
         *   1. 上传者本人；
         *   2. 管理员；
         *   3. 参与申报审核的岗位
         *      （辅导员、部长、副部长、干事）；
         *   4. 学生查看挂在自己申报单上的凭证。
         * =====================================================
         */

        if (!canViewFile(currentUserId, fileInfo)) {
            return jsonError(403, "无权查看该材料");
        }


        /*
         * =====================================================
         * 2. 获取文件路径
         * =====================================================
         */

        String storedPath =
                fileInfo.getFilePath();


        if (storedPath == null
                || storedPath.isBlank()) {

            return jsonError(404, "材料文件已丢失，请重新上传");

        }


        Path path;


        try {

            path =
                    Paths.get(
                            storedPath
                    );

        } catch (
                Exception e
        ) {

            log.warn(
                    "查看材料失败：文件路径非法 fileId={}, path={}",
                    id,
                    storedPath
            );

            return jsonError(404, "材料文件路径无效，请重新上传");

        }


        Resource resource;


        try {

            resource =
                    new UrlResource(
                            path.toUri()
                    );

        } catch (
                MalformedURLException e
        ) {

            return jsonError(404, "材料文件路径无效，请重新上传");

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

            /*
             * 磁盘上找不到文件时记录日志，
             * 方便定位「容器迁移 / 临时目录导致文件丢失」的问题。
             */
            log.warn(
                    "查看材料失败：文件不存在 fileId={}, path={}",
                    id,
                    storedPath
            );

            return jsonError(404, "材料文件已丢失，请重新上传");

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
         * 4.1 探测失败时的按扩展名兜底
         *
         * 精简容器里没有 /etc/mime.types 时，
         * Files.probeContentType 会返回 null，
         * 此时 Word / PDF 只能拿到
         * application/octet-stream，浏览器无法识别。
         * =====================================================
         */

        if (
                MediaType.APPLICATION_OCTET_STREAM.equals(
                        mediaType
                )
        ) {

            mediaType =
                    resolveMediaTypeByFileName(
                            fileInfo.getFileName()
                    );

        }


        /*
         * =====================================================
         * 5. 返回文件
         *
         * Content-Disposition 统一用 RFC 5987 编码，
         * 避免中文文件名直接写进响应头带来的兼容性问题。
         * =====================================================
         */

        ContentDisposition disposition =
                ContentDisposition.inline()
                        .filename(
                                fileInfo.getFileName() == null
                                        ? "material"
                                        : fileInfo.getFileName(),
                                StandardCharsets.UTF_8
                        )
                        .build();


        ResponseEntity.BodyBuilder builder =
                ResponseEntity.ok()
                        .contentType(
                                mediaType
                        )
                        .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                disposition.toString()
                        );


        try {

            long length =
                    resource.contentLength();


            if (length >= 0) {

                builder.contentLength(
                        length
                );
            }

        } catch (
                IOException ignored
        ) {

        }


        return builder.body(
                resource
        );

    }


    /**
     * 统一的错误响应体。
     *
     * 前端以 blob 方式请求文件，
     * 这里返回 JSON，便于前端解析出真实的失败原因。
     */
    private ResponseEntity<Map<String, Object>> jsonError(
            int status,
            String message
    ) {

        Map<String, Object> body =
                new LinkedHashMap<>();


        body.put(
                "code",
                status
        );


        body.put(
                "message",
                message
        );


        return ResponseEntity
                .status(status)
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .body(body);
    }

    /**
     * 允许查看申报材料的岗位。
     *
     * 取值与 sys_position.name 保持一致，
     * 不新增角色，仅覆盖现有审核岗位。
     */
    /**
     * 按文件名后缀推断 Content-Type。
     *
     * 只覆盖系统允许上传的凭证格式，
     * 未知后缀仍然返回 application/octet-stream。
     */
    private MediaType resolveMediaTypeByFileName(String fileName) {

        if (fileName == null || fileName.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }

        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }

        if (lowerName.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }

        if (lowerName.endsWith(".docx")) {
            return MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );
        }

        if (lowerName.endsWith(".doc")) {
            return MediaType.parseMediaType(
                    "application/msword"
            );
        }

        return MediaType.APPLICATION_OCTET_STREAM;

    }


    private static final Set<String> REVIEWER_POSITIONS = Set.of(
            "管理员",
            "辅导员",
            "部长",
            "副部长",
            "干事"
    );

    /**
     * 判断当前用户是否有权查看指定文件。
     *
     * 规则：
     *   1. 上传者本人可查看；
     *   2. 拥有管理员 / 审核岗位的用户可查看；
     *   3. 学生可查看自己申报单所附的凭证
     *      （例如部长代为提交，但学生本人仍需能看到自己的证明）。
     */
    private boolean canViewFile(Long currentUserId, FileInfo fileInfo) {
        if (currentUserId == null) {
            return false;
        }

        if (currentUserId.equals(fileInfo.getUploaderId())) {
            return true;
        }

        if (hasReviewerPosition(currentUserId)) {
            return true;
        }

        return isEvidenceOfOwnApplication(
                currentUserId,
                fileInfo.getId()
        );
    }

    /**
     * 文件是否挂在当前用户自己的申报单上。
     *
     * 部门加减分申报：department_score_apply.student_id / applicant_id
     * 个人证书申报：score_apply.student_id / material_file
     */
    private boolean isEvidenceOfOwnApplication(
            Long currentUserId,
            Long fileId
    ) {

        if (fileId == null) {
            return false;
        }

        String fileUrl =
                "/api/file/view/" + fileId;


        Long departmentApplyCount =
                departmentScoreApplyMapper.selectCount(
                        new LambdaQueryWrapper<DepartmentScoreApply>()
                                .eq(
                                        DepartmentScoreApply::getEvidenceUrl,
                                        fileUrl
                                )
                                .and(
                                        condition ->
                                                condition
                                                        .eq(
                                                                DepartmentScoreApply::getStudentId,
                                                                currentUserId
                                                        )
                                                        .or()
                                                        .eq(
                                                                DepartmentScoreApply::getApplicantId,
                                                                currentUserId
                                                        )
                                )
                );


        if (departmentApplyCount != null
                && departmentApplyCount > 0) {

            return true;
        }


        Long scoreApplyCount =
                scoreApplyMapper.selectCount(
                        new LambdaQueryWrapper<ScoreApply>()
                                .eq(
                                        ScoreApply::getMaterialFile,
                                        fileUrl
                                )
                                .eq(
                                        ScoreApply::getStudentId,
                                        currentUserId
                                )
                );


        return scoreApplyCount != null
                && scoreApplyCount > 0;
    }

    /**
     * 判断是否拥有管理员 / 审核岗位
     */
    private boolean hasReviewerPosition(
            Long currentUserId
    ) {

        List<SysUserPosition> userPositions = userPositionMapper.selectList(
                new LambdaQueryWrapper<SysUserPosition>()
                        .eq(SysUserPosition::getUserId, currentUserId)
        );

        if (userPositions == null || userPositions.isEmpty()) {
            return false;
        }

        List<Long> positionIds = new ArrayList<>();

        for (SysUserPosition userPosition : userPositions) {
            if (userPosition != null && userPosition.getPositionId() != null) {
                positionIds.add(userPosition.getPositionId());
            }
        }

        if (positionIds.isEmpty()) {
            return false;
        }

        Long count = positionMapper.selectCount(
                new LambdaQueryWrapper<SysPosition>()
                        .in(SysPosition::getId, positionIds)
                        .and(
                                condition ->
                                        condition
                                                .in(
                                                        SysPosition::getName,
                                                        REVIEWER_POSITIONS
                                                )
                                                .or()
                                                .in(
                                                        SysPosition::getLevel,
                                                        REVIEWER_POSITIONS
                                                )
                        )
        );

        return count != null && count > 0;
    }

}
