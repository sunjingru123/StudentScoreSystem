package com.student.studentscoresystem;

import com.student.studentscoresystem.controller.FileInfoController;
import com.student.studentscoresystem.entity.FileInfo;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.DepartmentScoreApplyMapper;
import com.student.studentscoresystem.mapper.FileInfoMapper;
import com.student.studentscoresystem.mapper.ScoreApplyMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 材料查看接口（GET /file/view/{id}）权限与健壮性测试。
 *
 * 覆盖：
 *
 * 1. 上传者本人可查看自己的材料；
 * 2. 无关学生被拒绝，并返回可展示的具体原因；
 * 3. 学生可查看挂在自己部门加减分申报上的凭证
 *    （例如由部长代为上传）；
 * 4. 磁盘文件丢失时返回 404 与明确提示，而不是空响应；
 * 5. 未登录返回 401。
 */
class FileInfoControllerViewTest {

    private static final Long FILE_ID = 7L;

    private static final Long STUDENT_ID = 100L;

    private static final Long OTHER_STUDENT_ID = 200L;

    private static final Long REVIEWER_ID = 300L;

    @TempDir
    Path tempDir;

    private FileInfoMapper fileInfoMapper;

    private SysUserPositionMapper userPositionMapper;

    private SysPositionMapper positionMapper;

    private DepartmentScoreApplyMapper departmentScoreApplyMapper;

    private ScoreApplyMapper scoreApplyMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        fileInfoMapper = mock(FileInfoMapper.class);
        userPositionMapper = mock(SysUserPositionMapper.class);
        positionMapper = mock(SysPositionMapper.class);
        departmentScoreApplyMapper = mock(DepartmentScoreApplyMapper.class);
        scoreApplyMapper = mock(ScoreApplyMapper.class);

        FileInfoController controller =
                new FileInfoController(
                        fileInfoMapper,
                        userPositionMapper,
                        positionMapper,
                        departmentScoreApplyMapper,
                        scoreApplyMapper
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void uploaderCanViewOwnFile() throws Exception {

        stubFile(fileInfo(STUDENT_ID, existingFile()));

        mockMvc.perform(
                        get("/file/view/" + FILE_ID)
                                .requestAttr("userId", STUDENT_ID)
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                "Content-Disposition",
                                org.hamcrest.Matchers.containsString("inline")
                        )
                );
    }

    @Test
    void unrelatedStudentIsRejectedWithMessage() throws Exception {

        stubFile(fileInfo(STUDENT_ID, existingFile()));

        /* 该学生只有「学生」岗位，不是任何审核角色 */
        stubStudentPosition(OTHER_STUDENT_ID);

        /* 文件不属于该学生的任何申报 */
        when(departmentScoreApplyMapper.selectCount(any())).thenReturn(0L);
        when(scoreApplyMapper.selectCount(any())).thenReturn(0L);

        mockMvc.perform(
                        get("/file/view/" + FILE_ID)
                                .requestAttr("userId", OTHER_STUDENT_ID)
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        content().contentTypeCompatibleWith("application/json")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("无权查看该材料")
                );
    }

    @Test
    void studentCanViewEvidenceOfOwnApplication() throws Exception {

        stubFile(fileInfo(200L, existingFile()));

        stubStudentPosition(OTHER_STUDENT_ID);

        /* 文件挂在本人部门加减分申报上 */
        when(departmentScoreApplyMapper.selectCount(any())).thenReturn(1L);

        mockMvc.perform(
                        get("/file/view/" + FILE_ID)
                                .requestAttr("userId", OTHER_STUDENT_ID)
                )
                .andExpect(status().isOk());
    }

    @Test
    void reviewerCanViewOthersFile() throws Exception {

        stubFile(fileInfo(STUDENT_ID, existingFile()));

        SysUserPosition relation = new SysUserPosition();
        relation.setUserId(REVIEWER_ID);
        relation.setPositionId(9L);

        when(userPositionMapper.selectList(any()))
                .thenReturn(Collections.singletonList(relation));

        SysPosition position = new SysPosition();
        position.setId(9L);
        position.setName("部长");

        when(positionMapper.selectCount(any())).thenReturn(1L);

        mockMvc.perform(
                        get("/file/view/" + FILE_ID)
                                .requestAttr("userId", REVIEWER_ID)
                )
                .andExpect(status().isOk());
    }

    @Test
    void missingPhysicalFileReturnsClearMessage() throws Exception {

        stubFile(
                fileInfo(
                        STUDENT_ID,
                        tempDir.resolve("not-exists.pdf").toString()
                )
        );

        mockMvc.perform(
                        get("/file/view/" + FILE_ID)
                                .requestAttr("userId", STUDENT_ID)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.message")
                                .value("材料文件已丢失，请重新上传")
                );
    }

    @Test
    void missingFileRecordReturnsClearMessage() throws Exception {

        when(fileInfoMapper.selectById(FILE_ID)).thenReturn(null);

        mockMvc.perform(
                        get("/file/view/" + FILE_ID)
                                .requestAttr("userId", STUDENT_ID)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.message")
                                .value("材料不存在（文件记录已丢失）")
                );
    }

    @Test
    void anonymousRequestIsRejected() throws Exception {

        mockMvc.perform(
                        get("/file/view/" + FILE_ID)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.message")
                                .value("请先登录后再查看材料")
                );
    }

    /* =====================================================
     * 辅助
     * ===================================================== */

    private void stubFile(FileInfo fileInfo) {

        when(fileInfoMapper.selectById(FILE_ID)).thenReturn(fileInfo);
    }

    private void stubStudentPosition(Long userId) {

        SysUserPosition relation = new SysUserPosition();
        relation.setUserId(userId);
        relation.setPositionId(3L);

        when(userPositionMapper.selectList(any()))
                .thenReturn(Collections.singletonList(relation));

        when(positionMapper.selectCount(any())).thenReturn(0L);
    }

    private FileInfo fileInfo(Long uploaderId, String filePath) {

        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(FILE_ID);
        fileInfo.setFileName("获奖证明.pdf");
        fileInfo.setFileType("application/pdf");
        fileInfo.setFilePath(filePath);
        fileInfo.setUploaderId(uploaderId);

        return fileInfo;
    }

    private String existingFile() throws Exception {

        Path file =
                tempDir.resolve("evidence.pdf");

        Files.write(
                file,
                "pdf-content".getBytes(StandardCharsets.UTF_8)
        );

        return file.toString();
    }
}
