package com.student.studentscoresystem.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * =========================================================
 * 兼容历史口令的密码编码器
 *
 * 1. 新口令统一使用 BCrypt 加密存储。
 *
 * 2. 校验口令时始终通过 PasswordEncoder.matches：
 *
 *    - 已是 BCrypt 密文：使用 BCrypt 恒定时间校验；
 *
 *    - 历史遗留的未哈希口令：使用 MessageDigest.isEqual
 *      做恒定时间比较，彻底废除 String.equals 明文比对，
 *      抵御计时侧信道攻击；校验成功后由调用方升级为 BCrypt。
 * =========================================================
 */
public class LegacyCompatPasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bcrypt =
            new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {

        return bcrypt.encode(
                rawPassword
        );
    }

    @Override
    public boolean matches(
            CharSequence rawPassword,
            String encodedPassword
    ) {

        if (rawPassword == null
                || encodedPassword == null
                || encodedPassword.isEmpty()) {

            return false;
        }

        if (isBcrypt(encodedPassword)) {

            try {

                return bcrypt.matches(
                        rawPassword,
                        encodedPassword
                );

            } catch (Exception e) {

                return false;
            }
        }

        /*
         * 历史未哈希口令：恒定时间比较，不使用 String.equals。
         */
        return MessageDigest.isEqual(
                rawPassword.toString()
                        .getBytes(StandardCharsets.UTF_8),
                encodedPassword.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 是否已经是 BCrypt 密文
     */
    public static boolean isBcrypt(String password) {

        if (password == null) {

            return false;
        }

        return password.startsWith("$2a$")
                || password.startsWith("$2b$")
                || password.startsWith("$2y$");
    }

    /**
     * 是否需要升级为 BCrypt 哈希
     */
    public static boolean needsUpgrade(String password) {

        return password != null
                && !password.isEmpty()
                && !isBcrypt(password);
    }
}
