package com.student.studentscoresystem.interceptor;

import com.student.studentscoresystem.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class JwtInterceptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {


        // ==============================
        // 1. 放行浏览器的跨域预检请求
        // ==============================
        if ("OPTIONS".equalsIgnoreCase(
                request.getMethod()
        )) {

            return true;

        }


        // ==============================
        // 2. 获取 Token
        // ==============================
        //
        // 优先从 Authorization Header 获取
        //
        // Authorization:
        // Bearer xxxxxxxxx
        //
        // 如果 Header 没有，
        // 再从 Cookie 中获取 token
        // ==============================

        String token = null;


        String authHeader =
                request.getHeader(
                        "Authorization"
                );


        if (
                authHeader != null &&
                        authHeader.startsWith("Bearer ")
        ) {

            token =
                    authHeader.substring(7);

        }


        // ==============================
        // 3. Header 没有 Token
        // 尝试从 Cookie 获取
        // ==============================

        if (
                token == null ||
                        token.isBlank()
        ) {

            Cookie[] cookies =
                    request.getCookies();


            if (
                    cookies != null
            ) {

                for (
                        Cookie cookie :
                        cookies
                ) {

                    if (
                            "token".equals(
                                    cookie.getName()
                            )
                    ) {

                        token =
                                cookie.getValue();

                        break;

                    }

                }

            }

        }


        // ==============================
        // 4. 没有 Token
        // ==============================

        if (
                token == null ||
                        token.isBlank()
        ) {

            response.setStatus(401);

            response.setContentType(
                    "application/json;charset=UTF-8"
            );

            response.getWriter().write(
                    "{\"message\":\"请先登录\"}"
            );

            return false;

        }


        // ==============================
        // 5. 解析并验证 Token
        // ==============================

        Claims claims;


        try {

            claims =
                    JwtUtil.parseToken(
                            token
                    );

        } catch (
                Exception e
        ) {

            response.setStatus(401);

            response.setContentType(
                    "application/json;charset=UTF-8"
            );

            response.getWriter().write(
                    "{\"message\":\"token无效\"}"
            );

            return false;

        }


        // ==============================
        // 6. 从 Token 获取 userId
        // ==============================

        Long userId = null;


        try {

            userId =
                    claims.get(
                            "userId",
                            Long.class
                    );

        } catch (
                Exception e
        ) {

            e.printStackTrace();

        }


        // ==============================
        // 7. Token 中没有 userId
        // ==============================

        if (
                userId == null
        ) {

            response.setStatus(401);

            response.setContentType(
                    "application/json;charset=UTF-8"
            );

            response.getWriter().write(
                    "{\"message\":\"token中没有用户信息\"}"
            );

            return false;

        }


        // ==============================
        // 8. 将 userId 放入 request
        // ==============================
        //
        // 后面的 Controller 可以直接：
        //
        // request.getAttribute("userId")
        //
        // 获取当前登录用户 ID
        // ==============================

        request.setAttribute(
                "userId",
                userId
        );


        // ==============================
        // 9. Token 验证通过
        // ==============================

        return true;

    }

}