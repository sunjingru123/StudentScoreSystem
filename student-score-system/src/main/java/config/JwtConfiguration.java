package com.student.studentscoresystem.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * 在 Spring Boot 启动阶段校验 JWT 配置，避免应用在缺少密钥时仍然启动。
 */
@Configuration
public class JwtConfiguration {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    @PostConstruct
    public void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not configured");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
        }
        if (expiration <= 0) {
            throw new IllegalStateException("JWT_EXPIRATION must be a positive number of milliseconds");
        }
    }
}
