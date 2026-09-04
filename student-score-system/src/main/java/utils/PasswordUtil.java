package com.student.studentscoresystem.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类
 *
 * 统一使用 BCrypt 加密密码。
 */
public final class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER =
            new BCryptPasswordEncoder();

    private PasswordUtil() {
    }

    /**
     * 加密密码
     */
    public static String encode(String rawPassword) {

        if (rawPassword == null) {
            return null;
        }

        return ENCODER.encode(rawPassword);
    }

    /**
     * 校验密码
     */
    public static boolean matches(
            String rawPassword,
            String encodedPassword
    ) {

        if (rawPassword == null
                || encodedPassword == null
                || encodedPassword.isEmpty()) {

            return false;
        }

        return ENCODER.matches(
                rawPassword,
                encodedPassword
        );
    }

    /**
     * 判断是否已经是 BCrypt 密文
     */
    public static boolean isBcrypt(
            String password
    ) {

        if (password == null) {
            return false;
        }

        return password.startsWith("$2a$")
                || password.startsWith("$2b$")
                || password.startsWith("$2y$");
    }
}