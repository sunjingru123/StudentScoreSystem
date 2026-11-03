package com.student.studentscoresystem;

import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.PovertyImportPermissionService;
import com.student.studentscoresystem.service.ScoreManagerPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
 *   2. 姓名命中配置名单的辅导员（默认「邹」）
 *
 * 拒绝：
 *   3. 其他辅导员
 *   4. 普通学生
 *   5. 未登录
 * =========================================================
 */
class PovertyImportPermissionServiceTest {

    private static final long ADMIN_ID = 1L;
    private static final long ZOU_COUNSELOR_ID = 2L;
    private static final long OTHER_COUNSELOR_ID = 3L;
    private static final long STUDENT_ID = 4L;

    private SysUserMapper sysUserMapper;

    private ScoreManagerPermissionService scoreManagerPermissionService;

    private PovertyImportPermissionService service;

    @BeforeEach
    void setUp() {

        sysUserMapper = mock(SysUserMapper.class);

        scoreManagerPermissionService =
                mock(ScoreManagerPermissionService.class);

        service = new PovertyImportPermissionService(
                sysUserMapper,
                scoreManagerPermissionService,
                "邹"
        );
    }

    private void givenUser(long userId, String realName) {

        SysUser user = new SysUser();
        user.setId(userId);
        user.setRealName(realName);

        when(sysUserMapper.selectById(any()))
                .thenReturn(user);
    }

    @Test
    void adminCanImport() {

        when(scoreManagerPermissionService.isAdmin(ADMIN_ID))
                .thenReturn(true);

        assertTrue(service.canImportPovertyList(ADMIN_ID));
    }

    @Test
    void counselorWithConfiguredNameCanImport() {

        when(scoreManagerPermissionService.isCounselor(ZOU_COUNSELOR_ID))
                .thenReturn(true);

        givenUser(ZOU_COUNSELOR_ID, "邹老师");

        assertTrue(service.canImportPovertyList(ZOU_COUNSELOR_ID));
    }

    @Test
    void counselorWithOtherNameCannotImport() {

        when(scoreManagerPermissionService.isCounselor(OTHER_COUNSELOR_ID))
                .thenReturn(true);

        givenUser(OTHER_COUNSELOR_ID, "李老师");

        assertFalse(service.canImportPovertyList(OTHER_COUNSELOR_ID));
    }

    @Test
    void plainStudentCannotImport() {

        givenUser(STUDENT_ID, "邹同学");

        assertFalse(service.canImportPovertyList(STUDENT_ID));
    }

    @Test
    void anonymousCannotImport() {

        assertFalse(service.canImportPovertyList(null));
    }
}