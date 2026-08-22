package com.student.studentscoresystem.config;


import com.student.studentscoresystem.interceptor.JwtInterceptor;
import com.student.studentscoresystem.interceptor.RoleInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig implements WebMvcConfigurer {


    private final JwtInterceptor jwtInterceptor;

    private final RoleInterceptor roleInterceptor;


    public WebConfig(
            JwtInterceptor jwtInterceptor,
            RoleInterceptor roleInterceptor
    ) {

        this.jwtInterceptor = jwtInterceptor;

        this.roleInterceptor = roleInterceptor;

    }


    @Override
    public void addInterceptors(
            InterceptorRegistry registry
    ) {


        // JWT 身份验证
        registry.addInterceptor(jwtInterceptor)

                .addPathPatterns("/**")

                .excludePathPatterns(
                        "/login"
                );


        // 角色权限验证
        registry.addInterceptor(roleInterceptor)

                .addPathPatterns("/**");


    }

}