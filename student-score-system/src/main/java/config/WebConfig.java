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


    /**
     * =========================================================
     * JWT 和角色权限拦截器
     * =========================================================
     */
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

                .addPathPatterns("/**")

                ;

    }


    /**
     * =========================================================
     * 文件静态资源映射
     * =========================================================
     *
     * 浏览器访问：
     *
     * /uploads/xxx.pdf
     *
     * 实际读取：
     *
     * 项目目录/uploads/xxx.pdf
     *
     * =========================================================
     */
}
