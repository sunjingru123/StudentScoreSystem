package com.student.studentscoresystem.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET_RAW = requireSecret();
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_RAW.getBytes(java.nio.charset.StandardCharsets.UTF_8));

    private static final long EXPIRATION = parseExpiration();

    private static String requireSecret() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET environment variable must be configured");
        }
        if (secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
        }
        return secret;
    }

    private static long parseExpiration() {
        String configured = System.getenv("JWT_EXPIRATION");
        if (configured == null || configured.isBlank()) {
            return 86_400_000L;
        }
        try {
            long value = Long.parseLong(configured);
            if (value <= 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("JWT_EXPIRATION must be a positive number of milliseconds", ex);
        }
    }

    public static String createToken(Long userId, String username) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SECRET_KEY)
                .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
