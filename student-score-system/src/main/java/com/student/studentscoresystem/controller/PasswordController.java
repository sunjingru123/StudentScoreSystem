package com.student.studentscoresystem.controller;

import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.service.ISysUserService;
import com.student.studentscoresystem.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 密码管理
 *
 * 学生、辅导员均可使用。
 */
@RestController
@RequestMapping("/user/password")
public class PasswordController {

    private final ISysUserService sysUserService;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public PasswordController(
            ISysUserService sysUserService
    ) {

        this.sysUserService =
                sysUserService;
    }

    /**
     * 修改密码
     *
     * POST /user/password/change
     */
    @PostMapping("/change")
    public Result<Void> changePassword(
            @RequestBody ChangePasswordDTO dto,
            HttpServletRequest request
    ) {

        // =====================================================
        // 1. 获取当前用户 ID
        // =====================================================

        Long userId =
                getUserId(request);

        if (userId == null) {

            return Result.fail(
                    "请先登录"
            );
        }

        // =====================================================
        // 2. 参数检查
        // =====================================================

        if (dto == null) {

            return Result.fail(
                    "参数不能为空"
            );
        }

        if (dto.getOldPassword() == null
                || dto.getOldPassword()
                .isEmpty()) {

            return Result.fail(
                    "请输入原密码"
            );
        }

        if (dto.getNewPassword() == null
                || dto.getNewPassword()
                .isEmpty()) {

            return Result.fail(
                    "请输入新密码"
            );
        }

        if (dto.getConfirmPassword() == null
                || dto.getConfirmPassword()
                .isEmpty()) {

            return Result.fail(
                    "请确认新密码"
            );
        }

        // =====================================================
        // 3. 新密码长度
        // =====================================================

        if (dto.getNewPassword().length() < 6) {

            return Result.fail(
                    "新密码长度不能少于6位"
            );
        }

        if (dto.getNewPassword().length() > 50) {

            return Result.fail(
                    "新密码长度不能超过50位"
            );
        }

        // =====================================================
        // 4. 两次密码一致
        // =====================================================

        if (!dto.getNewPassword()
                .equals(dto.getConfirmPassword())) {

            return Result.fail(
                    "两次输入的新密码不一致"
            );
        }

        // =====================================================
        // 5. 查询当前用户
        // =====================================================

        SysUser user =
                sysUserService.getById(
                        userId
                );

        if (user == null) {

            return Result.fail(
                    "用户不存在"
            );
        }

        // =====================================================
        // 6. 校验原密码
        //
        // 同样兼容旧版明文密码。
        // =====================================================

        String databasePassword =
                user.getPassword();

        boolean correct;

        if (databasePassword == null
                || databasePassword.isEmpty()) {

            return Result.fail(
                    "当前账号密码异常，请联系管理员"
            );
        }

        if (databasePassword.startsWith("$2a$")
                || databasePassword.startsWith("$2b$")
                || databasePassword.startsWith("$2y$")) {

            correct =
                    passwordEncoder.matches(
                            dto.getOldPassword(),
                            databasePassword
                    );

        } else {

            correct =
                    databasePassword.equals(
                            dto.getOldPassword()
                    );
        }

        if (!correct) {

            return Result.fail(
                    "原密码错误"
            );
        }

        // =====================================================
        // 7. 新旧密码不能相同
        // =====================================================

        if (dto.getOldPassword()
                .equals(dto.getNewPassword())) {

            return Result.fail(
                    "新密码不能与原密码相同"
            );
        }

        // =====================================================
        // 8. BCrypt 加密
        // =====================================================

        String encodedPassword =
                passwordEncoder.encode(
                        dto.getNewPassword()
                );

        user.setPassword(
                encodedPassword
        );

        // =====================================================
        // 9. 首次登录标记改为正常
        // =====================================================

        user.setFirstLogin(
                (short) 0
        );

        // =====================================================
        // 10. 保存
        // =====================================================

        boolean success =
                sysUserService.updateById(
                        user
                );

        if (!success) {

            return Result.fail(
                    "密码修改失败"
            );
        }

        return Result.success(null);
    }

    /**
     * 从 Token 获取用户 ID
     */
    private Long getUserId(
            HttpServletRequest request
    ) {

        String authorization =
                request.getHeader(
                        "Authorization"
                );

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            return null;
        }

        String token =
                authorization.substring(7);

        try {

            Claims claims =
                    JwtUtil.parseToken(
                            token
                    );

            return claims.get(
                    "userId",
                    Long.class
            );

        } catch (Exception e) {

            return null;
        }
    }

    /**
     * 修改密码 DTO
     */
    @Data
    public static class ChangePasswordDTO {

        private String oldPassword;

        private String newPassword;

        private String confirmPassword;
    }
}