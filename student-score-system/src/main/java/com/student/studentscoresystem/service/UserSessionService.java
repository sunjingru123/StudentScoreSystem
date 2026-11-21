package com.student.studentscoresystem.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * =========================================================
 * 单账号登录会话
 * =========================================================
 *
 * 业务规则：
 *
 * 同一个账号同一时间只允许一处登录。
 *
 * 实现方式：
 *
 * 1. 登录成功后生成一个随机会话 ID，
 *    写入 sys_user.active_session_id；
 *
 * 2. 会话 ID 作为 JWT 的 sid 声明随 Token 下发；
 *
 * 3. 每次请求由 JwtInterceptor 校验：
 *    Token 中的 sid 必须等于数据库里该账号当前的 active_session_id。
 *
 * 因此：
 *
 * - 后登录的设备会覆盖 active_session_id，
 *   先前设备的 Token 立即失效（返回 401，前端跳转登录页）；
 * - 服务重启后状态仍然有效（存在数据库里，不依赖内存）；
 * - 不改变 JWT 签名密钥、有效期与角色体系。
 * =========================================================
 */
@Component
public class UserSessionService {

    /**
     * 用户 Mapper
     */
    private final SysUserMapper sysUserMapper;

    public UserSessionService(
            SysUserMapper sysUserMapper
    ) {

        this.sysUserMapper =
                sysUserMapper;
    }

    /**
     * 登录成功后开启新会话
     *
     * 会覆盖该账号之前的会话，使旧 Token 立即失效。
     *
     * @param userId 登录用户 ID
     * @return 新的会话 ID
     */
    public String startSession(
            Long userId
    ) {

        if (userId == null) {

            throw new IllegalArgumentException(
                    "userId must not be null"
            );
        }

        String sessionId =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "");

        /*
         * 只更新 active_session_id 一个字段，
         * 避免把整行用户数据（密码、首登标记等）一起写回。
         */
        LambdaUpdateWrapper<SysUser> wrapper =
                new LambdaUpdateWrapper<>();

        wrapper.eq(
                        SysUser::getId,
                        userId
                )
                .set(
                        SysUser::getActiveSessionId,
                        sessionId
                );

        sysUserMapper.update(
                null,
                wrapper
        );

        return sessionId;
    }

    /**
     * 判断 Token 中的会话 ID 是否仍为该账号当前有效会话
     *
     * @param userId    用户 ID
     * @param sessionId Token 中的 sid 声明
     * @return true = 当前有效会话
     */
    public boolean isActiveSession(
            Long userId,
            String sessionId
    ) {

        if (userId == null) {

            return false;
        }

        if (sessionId == null
                || sessionId.isBlank()) {

            return false;
        }

        SysUser user =
                sysUserMapper.selectById(
                        userId
                );

        if (user == null) {

            return false;
        }

        /*
         * 已软删除的学生立即失效登录态，
         * 避免已删除账号继续携带旧 Token 访问业务接口。
         */
        if (user.getDeleted() != null
                && user.getDeleted() == 1) {

            return false;
        }

        String activeSessionId =
                user.getActiveSessionId();

        /*
         * 数据库中没有会话 ID：
         *
         * 说明该账号还没有在新规则下登录过，
         * 旧 Token 一律失效，必须重新登录。
         */
        return activeSessionId != null
                && activeSessionId.equals(sessionId);
    }
}
