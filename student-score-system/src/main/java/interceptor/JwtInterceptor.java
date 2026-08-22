package com.student.studentscoresystem.interceptor;

import com.student.studentscoresystem.utils.JwtUtil;
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
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {

            return true;

        }


        // ==============================
        // 2. 获取 Authorization
        // ==============================
        String token =
                request.getHeader("Authorization");


        // ==============================
        // 3. 没有 Token
        // ==============================
        if (token == null ||
                !token.startsWith("Bearer ")) {


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
        // 4. 验证 Token
        // ==============================
        try {


            String jwt =
                    token.substring(7);


            JwtUtil.parseToken(jwt);


        } catch (Exception e) {


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
        // 5. Token 正确
        // ==============================
        return true;

    }

}