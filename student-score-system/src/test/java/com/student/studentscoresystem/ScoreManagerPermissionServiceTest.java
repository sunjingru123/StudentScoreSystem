package com.student.studentscoresystem;

import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.ScoreManagerPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * =========================================================
 * 学生成绩维护权限矩阵回归测试
 * =========================================================
 *
 * 允许：
 *   1. 管理员
 *   2. 档案部部长
 *   3. 档案部副部长
 *
 * 拒绝：
 *   4. 档案部干事
 *   5. 普通学生
 *   6. 辅导员
 *   7. 其他部门部长
 *   8. 档案部已停用
 *   9. 未登录
 * =========================================================
 */
class ScoreManagerPermissionServiceTest {

    private static final long ADMIN_ID = 1L;
    private static final long ARCHIVE_LEADER_ID = 2L;
    private static final long ARCHIVE_DEPUTY_ID = 3L;
    private static final long ARCHIVE_MEMBER_ID = 4L;
    private static final long STUDENT_ID = 5L;
    private static final long COUNSELOR_ID = 6L;
    private static final long OTHER_LEADER_ID = 7L;

    private static final long ARCHIVE_DEPT_ID = 10L;
    private static final long OTHER_DEPT_ID = 11L;

    private SysUserPositionMapper userPositionMapper;
    private SysPositionMapper positionMapper;
    private SysUserDepartmentMapper userDepartmentMapper;
    private DepartmentMapper departmentMapper;

    private ScoreManagerPermissionService service;

    @BeforeEach
    void setUp() {

        userPositionMapper = mock(SysUserPositionMapper.class);
        positionMapper = mock(SysPositionMapper.class);
        userDepartmentMapper = mock(SysUserDepartmentMapper.class);
        departmentMapper = mock(DepartmentMapper.class);

        service = new ScoreManagerPermissionService(
                userPositionMapper,
                positionMapper,
                userDepartmentMapper,
                departmentMapper
        );

        // 默认：没有任何部门关系
        when(userDepartmentMapper.selectList(any()))
                .thenReturn(Collections.emptyList());

        // 默认：没有任何岗位关系
        when(userPositionMapper.selectList(any()))
                .thenReturn(Collections.emptyList());
    }

    private void givenPosition(long userId, long positionId, String name, Short status) {

        SysUserPosition relation = new SysUserPosition();
        relation.setUserId(userId);
        relation.setPositionId(positionId);

        when(userPositionMapper.selectList(any()))
                .thenReturn(List.of(relation));

        SysPosition position = new SysPosition();
        position.setId(positionId);
        position.setName(name);
        position.setStatus(status);

        when(positionMapper.selectById(any()))
                .thenReturn(position);
    }

    private void givenDepartment(
            long userId,
            long departmentId,
            String position,
            String departmentName,
            Short departmentStatus
    ) {

        SysUserDepartment relation = new SysUserDepartment();
        relation.setUserId(userId);
        relation.setDepartmentId(departmentId);
        relation.setPosition(position);
        relation.setStatus((short) 1);

        when(userDepartmentMapper.selectList(any()))
                .thenReturn(List.of(relation));

        Department department = new Department();
        department.setId(departmentId);
        department.setName(departmentName);
        department.setStatus(departmentStatus);

        when(departmentMapper.selectById(any()))
                .thenReturn(department);
    }

    @Test
    void adminCanManageStudentScores() {

        givenPosition(ADMIN_ID, 100L, "管理员", (short) 1);

        assertTrue(service.isAdmin(ADMIN_ID));
        assertTrue(service.canManageStudentScores(ADMIN_ID));
    }

    @Test
    void archiveLeaderCanManageStudentScores() {

        givenDepartment(
                ARCHIVE_LEADER_ID,
                ARCHIVE_DEPT_ID,
                "部长",
                "档案部",
                (short) 1
        );

        assertTrue(service.isArchiveLeader(ARCHIVE_LEADER_ID));
        assertTrue(service.canManageStudentScores(ARCHIVE_LEADER_ID));
    }

    @Test
    void archiveDeputyCanManageStudentScores() {

        givenDepartment(
                ARCHIVE_DEPUTY_ID,
                ARCHIVE_DEPT_ID,
                "副部长",
                "档案部",
                (short) 1
        );

        assertTrue(service.isArchiveLeader(ARCHIVE_DEPUTY_ID));
        assertTrue(service.canManageStudentScores(ARCHIVE_DEPUTY_ID));
    }

    @Test
    void archiveMemberCannotManageStudentScores() {

        givenDepartment(
                ARCHIVE_MEMBER_ID,
                ARCHIVE_DEPT_ID,
                "干事",
                "档案部",
                (short) 1
        );

        assertFalse(service.isArchiveLeader(ARCHIVE_MEMBER_ID));
        assertFalse(service.canManageStudentScores(ARCHIVE_MEMBER_ID));
    }

    @Test
    void plainStudentCannotManageStudentScores() {

        givenPosition(STUDENT_ID, 101L, "学生", (short) 1);

        assertFalse(service.isAdmin(STUDENT_ID));
        assertFalse(service.canManageStudentScores(STUDENT_ID));
    }

    @Test
    void counselorCannotManageStudentScores() {

        givenPosition(COUNSELOR_ID, 102L, "辅导员", (short) 1);

        assertFalse(service.canManageStudentScores(COUNSELOR_ID));
    }

    @Test
    void otherDepartmentLeaderCannotManageStudentScores() {

        givenDepartment(
                OTHER_LEADER_ID,
                OTHER_DEPT_ID,
                "部长",
                "体育部",
                (short) 1
        );

        assertFalse(service.isArchiveLeader(OTHER_LEADER_ID));
        assertFalse(service.canManageStudentScores(OTHER_LEADER_ID));
    }

    @Test
    void disabledArchiveDepartmentIsRejected() {

        givenDepartment(
                ARCHIVE_LEADER_ID,
                ARCHIVE_DEPT_ID,
                "部长",
                "档案部",
                (short) 0
        );

        assertFalse(service.isArchiveLeader(ARCHIVE_LEADER_ID));
    }

    @Test
    void disabledAdminPositionIsRejected() {

        givenPosition(ADMIN_ID, 100L, "管理员", (short) 0);

        assertFalse(service.isAdmin(ADMIN_ID));
    }

    @Test
    void anonymousIsRejected() {

        assertFalse(service.canManageStudentScores(null));
        assertFalse(service.isAdmin(null));
        assertFalse(service.isArchiveLeader(null));
    }
}
