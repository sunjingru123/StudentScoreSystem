package com.student.studentscoresystem;

import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.service.PovertyImportPermissionService;
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
 * 贫困生名单导入权限回归测试
 * =========================================================
 *
 * 允许：
 *   1. 管理员
 *   2. 勤工助学部部长
 *   3. 勤工助学部副部长
 *
 * 拒绝：
 *   4. 勤工助学部干事
 *   5. 其他部门部长
 *   6. 普通学生
 *   7. 未登录
 * =========================================================
 */
class PovertyImportPermissionServiceTest {

    private static final long ADMIN_ID = 1L;

    private static final long WORK_STUDY_MINISTER_ID = 2L;

    private static final long WORK_STUDY_DEPUTY_ID = 3L;

    private static final long WORK_STUDY_CLERK_ID = 4L;

    private static final long OTHER_DEPARTMENT_MINISTER_ID = 5L;

    private static final long STUDENT_ID = 6L;

    private static final long WORK_STUDY_DEPARTMENT_ID = 10L;

    private static final long OTHER_DEPARTMENT_ID = 20L;

    private ScoreManagerPermissionService scoreManagerPermissionService;

    private SysUserDepartmentMapper userDepartmentMapper;

    private DepartmentMapper departmentMapper;

    private PovertyImportPermissionService service;

    @BeforeEach
    void setUp() {

        scoreManagerPermissionService =
                mock(ScoreManagerPermissionService.class);

        userDepartmentMapper = mock(SysUserDepartmentMapper.class);

        departmentMapper = mock(DepartmentMapper.class);

        service = new PovertyImportPermissionService(
                scoreManagerPermissionService,
                userDepartmentMapper,
                departmentMapper
        );
    }

    private void givenMember(
            long userId,
            String position,
            long departmentId,
            String departmentName
    ) {

        SysUserDepartment relation =
                new SysUserDepartment();

        relation.setUserId(userId);
        relation.setDepartmentId(departmentId);
        relation.setPosition(position);
        relation.setStatus((short) 1);

        when(userDepartmentMapper.selectList(any()))
                .thenReturn(List.of(relation));


        Department department =
                new Department();

        department.setId(departmentId);
        department.setName(departmentName);
        department.setStatus((short) 1);

        when(departmentMapper.selectById(departmentId))
                .thenReturn(department);
    }

    private void givenNoDepartmentRelation() {

        when(userDepartmentMapper.selectList(any()))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void adminCanImport() {

        when(scoreManagerPermissionService.isAdmin(ADMIN_ID))
                .thenReturn(true);

        assertTrue(service.canImportPovertyList(ADMIN_ID));
    }

    @Test
    void workStudyMinisterCanImport() {

        givenMember(
                WORK_STUDY_MINISTER_ID,
                "部长",
                WORK_STUDY_DEPARTMENT_ID,
                "勤工助学部"
        );

        assertTrue(
                service.canImportPovertyList(WORK_STUDY_MINISTER_ID)
        );
    }

    @Test
    void workStudyDeputyCanImport() {

        givenMember(
                WORK_STUDY_DEPUTY_ID,
                "副部长",
                WORK_STUDY_DEPARTMENT_ID,
                "勤工助学部"
        );

        assertTrue(
                service.canImportPovertyList(WORK_STUDY_DEPUTY_ID)
        );
    }

    @Test
    void workStudyClerkCannotImport() {

        givenMember(
                WORK_STUDY_CLERK_ID,
                "干事",
                WORK_STUDY_DEPARTMENT_ID,
                "勤工助学部"
        );

        assertFalse(
                service.canImportPovertyList(WORK_STUDY_CLERK_ID)
        );
    }

    @Test
    void otherDepartmentMinisterCannotImport() {

        givenMember(
                OTHER_DEPARTMENT_MINISTER_ID,
                "部长",
                OTHER_DEPARTMENT_ID,
                "档案部"
        );

        assertFalse(
                service.canImportPovertyList(OTHER_DEPARTMENT_MINISTER_ID)
        );
    }

    @Test
    void plainStudentCannotImport() {

        givenNoDepartmentRelation();

        assertFalse(service.canImportPovertyList(STUDENT_ID));
    }

    @Test
    void anonymousCannotImport() {

        assertFalse(service.canImportPovertyList(null));
    }
}
