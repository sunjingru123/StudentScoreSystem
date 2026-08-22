package com.student.studentscoresystem.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET_RAW = "student-score-system-jwt-secret-key-2026-springboot-postgresql";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_RAW.getBytes());

    // token有效期24小时
    private static final long EXPIRATION = 1000 * 60 * 60 * 24;

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