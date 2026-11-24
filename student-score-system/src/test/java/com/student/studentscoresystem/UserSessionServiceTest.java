package com.student.studentscoresystem;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.UserSessionService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * =========================================================
 * 单账号登录会话测试
 * =========================================================
 *
 * 业务规则：同一账号同一时间只允许一处登录。
 *
 * 本测试保证：
 *
 * 1. 登录会生成并落库新的会话 ID；
 * 2. 只有与会话 ID 完全一致的 Token 才放行；
 * 3. 旧 Token、空会话 ID、不存在的用户一律拒绝；
 * 4. 不会把整行用户数据一起写回（只更新 active_session_id）。
 * =========================================================
 */
class UserSessionServiceTest {

    private static final long USER_ID = 9L;

    @BeforeAll
    static void initTableInfo() {

        /*
         * LambdaUpdateWrapper 需要实体的表元信息，
         * 单元测试中没有 Spring 容器，手动初始化。
         */
        if (TableInfoHelper.getTableInfo(SysUser.class) != null) {

            return;
        }

        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(
                        new MybatisConfiguration(),
                        ""
                );

        assistant.setCurrentNamespace(
                SysUserMapper.class.getName()
        );

        TableInfoHelper.initTableInfo(
                assistant,
                SysUser.class
        );
    }

    @Test
    void startSessionWritesGeneratedSessionId() {

        SysUserMapper mapper =
                mock(SysUserMapper.class);

        UserSessionService service =
                new UserSessionService(mapper);

        String sessionId =
                service.startSession(USER_ID);

        assertNotNull(sessionId, "必须生成会话 ID");
        assertFalse(sessionId.isBlank(), "会话 ID 不能为空");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<SysUser>> captor =
                ArgumentCaptor.forClass(
                        LambdaUpdateWrapper.class
                );

        verify(mapper).update(
                isNull(),
                captor.capture()
        );

        assertTrue(
                captor.getValue()
                        .getSqlSet()
                        .contains("active_session_id"),
                "必须显式更新 active_session_id"
        );
    }

    @Test
    void startSessionRejectsNullUserId() {

        UserSessionService service =
                new UserSessionService(
                        mock(SysUserMapper.class)
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.startSession(null),
                "缺少用户 ID 时不允许开启会话"
        );
    }

    @Test
    void activeSessionAcceptsMatchingSessionId() {

        SysUserMapper mapper =
                mock(SysUserMapper.class);

        SysUser user =
                new SysUser();

        user.setId(USER_ID);
        user.setActiveSessionId("abc123");

        when(mapper.selectById(USER_ID))
                .thenReturn(user);

        UserSessionService service =
                new UserSessionService(mapper);

        assertTrue(
                service.isActiveSession(USER_ID, "abc123"),
                "会话 ID 一致应放行"
        );
    }

    @Test
    void activeSessionRejectsPreviousSessionId() {

        SysUserMapper mapper =
                mock(SysUserMapper.class);

        SysUser user =
                new SysUser();

        user.setId(USER_ID);
        user.setActiveSessionId("new-session");

        when(mapper.selectById(USER_ID))
                .thenReturn(user);

        UserSessionService service =
                new UserSessionService(mapper);

        assertFalse(
                service.isActiveSession(USER_ID, "old-session"),
                "其他设备登录后旧 Token 必须失效"
        );
    }

    @Test
    void activeSessionRejectsMissingSessionId() {

        SysUserMapper mapper =
                mock(SysUserMapper.class);

        SysUser user =
                new SysUser();

        user.setId(USER_ID);
        user.setActiveSessionId("abc123");

        when(mapper.selectById(USER_ID))
                .thenReturn(user);

        UserSessionService service =
                new UserSessionService(mapper);

        assertFalse(
                service.isActiveSession(USER_ID, null),
                "Token 中没有 sid 时必须拒绝"
        );

        assertFalse(
                service.isActiveSession(USER_ID, "  "),
                "空 sid 必须拒绝"
        );
    }

    @Test
    void activeSessionRejectsUserWithoutSession() {

        SysUserMapper mapper =
                mock(SysUserMapper.class);

        SysUser user =
                new SysUser();

        user.setId(USER_ID);

        when(mapper.selectById(USER_ID))
                .thenReturn(user);

        UserSessionService service =
                new UserSessionService(mapper);

        assertFalse(
                service.isActiveSession(USER_ID, "abc123"),
                "数据库中还没有会话时必须拒绝"
        );
    }

    @Test
    void activeSessionRejectsUnknownUser() {

        SysUserMapper mapper =
                mock(SysUserMapper.class);

        when(mapper.selectById(USER_ID))
                .thenReturn(null);

        UserSessionService service =
                new UserSessionService(mapper);

        assertFalse(
                service.isActiveSession(USER_ID, "abc123"),
                "用户不存在时必须拒绝"
        );
    }

    @Test
    void activeSessionRejectsNullUserId() {

        UserSessionService service =
                new UserSessionService(
                        mock(SysUserMapper.class)
                );

        assertFalse(
                service.isActiveSession(null, "abc123"),
                "缺少用户 ID 时必须拒绝"
        );
    }

    /* =====================================================
     * 单设备登录：退出登录 / 账号停用
     * ===================================================== */

    @Test
    void endSessionClearsActiveSessionId() {

        SysUserMapper mapper =
                mock(SysUserMapper.class);

        UserSessionService service =
                new UserSessionService(mapper);

        service.endSession(USER_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<SysUser>> captor =
                ArgumentCaptor.forClass(
                        LambdaUpdateWrapper.class
                );

        verify(mapper).update(
                isNull(),
                captor.capture()
        );

        assertTrue(
                captor.getValue()
                        .getSqlSet()
                        .contains("active_session_id"),
                "退出登录必须清空 active_session_id"
        );
    }

    @Test
    void disabledUserSessionIsRejected() {

        SysUserMapper mapper =
                mock(SysUserMapper.class);

        SysUser user =
                new SysUser();

        user.setId(USER_ID);
        user.setStatus((short) 0);
        user.setActiveSessionId("abc123");

        when(mapper.selectById(USER_ID))
                .thenReturn(user);

        UserSessionService service =
                new UserSessionService(mapper);

        assertFalse(
                service.isActiveSession(USER_ID, "abc123"),
                "停用账号的会话必须立即失效"
        );
    }
}
