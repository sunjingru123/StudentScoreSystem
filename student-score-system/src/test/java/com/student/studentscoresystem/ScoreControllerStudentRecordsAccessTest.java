package com.student.studentscoresystem;

import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.controller.ScoreController;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.CourseMapper;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.IScoreService;
import com.student.studentscoresystem.service.ScoreProjectNameResolver;
import com.student.studentscoresystem.vo.ScoreDetailVO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * =========================================================
 * /score/student/{studentId}/records 访问控制回归测试
 *
 * 覆盖点（对应安全整改后教师端「查看详情」失效的回归）：
 *
 * 1. 学生查看本人                     -> 允许
 * 2. 学生查看他人                     -> 拒绝
 * 3. 辅导员（教师端）查看学生         -> 允许
 * 4. 管理员查看学生                   -> 允许
 * 5. 未登录                           -> 拒绝
 * =========================================================
 */
class ScoreControllerStudentRecordsAccessTest {

    private static final long STUDENT_ID = 100L;
    private static final long OTHER_STUDENT_ID = 200L;
    private static final long COUNSELOR_ID = 300L;
    private static final long ADMIN_ID = 400L;

    private IScoreService scoreService;
    private SysUserMapper sysUserMapper;
    private CourseMapper courseMapper;
    private ScoreRecordMapper scoreRecordMapper;
    private ScoreProjectNameResolver scoreProjectNameResolver;
    private SysPositionMapper sysPositionMapper;
    private SysUserPositionMapper sysUserPositionMapper;

    private ScoreController controller;

    @BeforeEach
    void setUp() {

        scoreService = mock(IScoreService.class);
        sysUserMapper = mock(SysUserMapper.class);
        courseMapper = mock(CourseMapper.class);
        scoreRecordMapper = mock(ScoreRecordMapper.class);
        scoreProjectNameResolver = mock(ScoreProjectNameResolver.class);
        sysPositionMapper = mock(SysPositionMapper.class);
        sysUserPositionMapper = mock(SysUserPositionMapper.class);

        controller = new ScoreController(
                scoreService,
                sysUserMapper,
                courseMapper,
                scoreRecordMapper,
                scoreProjectNameResolver,
                sysPositionMapper,
                sysUserPositionMapper
        );
    }

    /**
     * 让给定用户只拥有一个指定名称的岗位。
     */
    private void givenUserPositions(long userId, Long positionId, String positionName) {

        SysUserPosition relation = new SysUserPosition();
        relation.setUserId(userId);
        relation.setPositionId(positionId);

        when(sysUserPositionMapper.selectList(any()))
                .thenReturn(List.of(relation));

        SysPosition position = new SysPosition();
        position.setId(positionId);
        position.setName(positionName);

        when(sysPositionMapper.selectById(any()))
                .thenReturn(position);
    }

    private HttpServletRequest requestOf(Long userId) {

        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getAttribute("userId"))
                .thenReturn(userId);

        return request;
    }

    /**
     * 让「查看成功」路径上的依赖返回可用数据。
     */
    private void givenRecordsReadable() {

        SysUser student = new SysUser();
        student.setId(STUDENT_ID);

        when(sysUserMapper.selectById(any()))
                .thenReturn(student);

        ScoreRecord certificate = new ScoreRecord();
        certificate.setId(1L);
        certificate.setStudentId(STUDENT_ID);
        certificate.setScore(new java.math.BigDecimal("5"));
        certificate.setSourceType(ScoreProjectNameResolver.TYPE_CERTIFICATE);
        certificate.setSourceId(900L);

        when(scoreRecordMapper.selectList(any()))
                .thenReturn(List.of(certificate));

        ScoreProjectNameResolver.PreloadedNames names =
                mock(ScoreProjectNameResolver.PreloadedNames.class);

        when(names.resolve(any(ScoreRecord.class)))
                .thenReturn("全国大学生数学建模竞赛");

        when(names.resolveSourceLabel(any(ScoreRecord.class)))
                .thenReturn("证书");

        when(scoreProjectNameResolver.preload(any()))
                .thenReturn(names);
    }

    @Test
    void studentCanViewOwnRecords() {

        givenUserPositions(STUDENT_ID, 5L, "学生");
        givenRecordsReadable();

        Result<List<ScoreDetailVO>> result =
                controller.studentRecords(STUDENT_ID, requestOf(STUDENT_ID));

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("全国大学生数学建模竞赛", result.getData().get(0).getRuleName());
    }

    @Test
    void studentCannotViewOthersRecords() {

        givenUserPositions(STUDENT_ID, 5L, "学生");

        Result<List<ScoreDetailVO>> result =
                controller.studentRecords(OTHER_STUDENT_ID, requestOf(STUDENT_ID));

        assertEquals(500, result.getCode());
        assertEquals("无权访问该学生成绩流水", result.getMessage());
        verify(scoreRecordMapper, never()).selectList(any());
    }

    @Test
    void counselorCanViewStudentRecords() {

        givenUserPositions(COUNSELOR_ID, 3L, "辅导员");
        givenRecordsReadable();

        Result<List<ScoreDetailVO>> result =
                controller.studentRecords(STUDENT_ID, requestOf(COUNSELOR_ID));

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
    }

    @Test
    void adminCanViewStudentRecords() {

        givenUserPositions(ADMIN_ID, 1L, "管理员");
        givenRecordsReadable();

        Result<List<ScoreDetailVO>> result =
                controller.studentRecords(STUDENT_ID, requestOf(ADMIN_ID));

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
    }

    @Test
    void anonymousIsRejected() {

        Result<List<ScoreDetailVO>> result =
                controller.studentRecords(STUDENT_ID, requestOf(null));

        assertEquals(500, result.getCode());
        assertEquals("请先登录", result.getMessage());
        verify(scoreRecordMapper, never()).selectList(any());
    }

    @Test
    void userWithoutPositionsCannotViewOthers() {

        when(sysUserPositionMapper.selectList(any()))
                .thenReturn(Collections.emptyList());

        Result<List<ScoreDetailVO>> result =
                controller.studentRecords(OTHER_STUDENT_ID, requestOf(STUDENT_ID));

        assertEquals(500, result.getCode());
        assertEquals("无权访问该学生成绩流水", result.getMessage());
        verify(scoreRecordMapper, never()).selectList(any());
    }
}
