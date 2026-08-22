package com.student.studentscoresystem.interceptor;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.annotation.RequireRole;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class RoleInterceptor implements HandlerInterceptor {


    private final SysUserPositionMapper sysUserPositionMapper;

    private final SysPositionMapper sysPositionMapper;


    public RoleInterceptor(
            SysUserPositionMapper sysUserPositionMapper,
            SysPositionMapper sysPositionMapper
    ) {

        this.sysUserPositionMapper =
                sysUserPositionMapper;

        this.sysPositionMapper =
                sysPositionMapper;

    }


    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {


        // ==================================
        // 1. 放行跨域预检 OPTIONS 请求
        // ==================================
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {

            return true;

        }


        // ==================================
        // 2. 不是 Controller 方法，直接放行
        // ==================================
        if (!(handler instanceof HandlerMethod)) {

            return true;

        }


        HandlerMethod method =
                (HandlerMethod) handler;


        // ==================================
        // 3. 获取方法上的 RequireRole 注解
        // ==================================
        RequireRole requireRole =
                method.getMethodAnnotation(
                        RequireRole.class
                );


        // ==================================
        // 4. 没有权限要求，直接通过
        // ==================================
        if (requireRole == null) {

            return true;

        }


        // ==================================
        // 5. 获取 Token
        // ==================================
        String token =
                request.getHeader("Authorization");


        // 理论上 JwtInterceptor 已经检查过了
        // 这里再保险一下
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


        token =
                token.substring(7);


        // ==================================
        // 6. 解析 Token
        // ==================================
        Claims claims;

        try {

            claims =
                    JwtUtil.parseToken(token);

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


        // ==================================
        // 7. 获取用户 ID
        // ==================================
        Long userId =
                claims.get(
                        "userId",
                        Long.class
                );


        if (userId == null) {

            response.setStatus(401);

            response.setContentType(
                    "application/json;charset=UTF-8"
            );

            response.getWriter().write(
                    "{\"message\":\"token中没有用户信息\"}"
            );

            return false;

        }


        // ==================================
        // 8. 查询用户岗位
        // ==================================
        SysUserPosition userPosition =
                sysUserPositionMapper.selectOne(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .eq(
                                        SysUserPosition::getUserId,
                                        userId
                                )
                );


        if (userPosition == null) {

            response.setStatus(403);

            response.setContentType(
                    "application/json;charset=UTF-8"
            );

            response.getWriter().write(
                    "{\"message\":\"没有权限\"}"
            );

            return false;

        }


        // ==================================
        // 9. 查询岗位
        // ==================================
        SysPosition position =
                sysPositionMapper.selectById(
                        userPosition.getPositionId()
                );


        if (position == null) {

            response.setStatus(403);

            response.setContentType(
                    "application/json;charset=UTF-8"
            );

            response.getWriter().write(
                    "{\"message\":\"岗位不存在\"}"
            );

            return false;

        }


        // ==================================
        // 10. 判断岗位权限
        // ==================================
        if (!position.getName()
                .equals(requireRole.value())) {


            response.setStatus(403);

            response.setContentType(
                    "application/json;charset=UTF-8"
            );

            response.getWriter().write(
                    "{\"message\":\"权限不足\"}"
            );

            return false;

        }


        // ==================================
        // 11. 权限通过
        // ==================================
        return true;

    }

}