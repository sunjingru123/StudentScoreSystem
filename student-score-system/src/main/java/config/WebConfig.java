package com.student.studentscoresystem.config;

import com.student.studentscoresystem.interceptor.JwtInterceptor;
import com.student.studentscoresystem.interceptor.RoleInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    private final RoleInterceptor roleInterceptor;

    /**
     * 文件上传目录
     */
    @Value("${file.upload-path:uploads}")
    private String uploadPath;

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
                        "/login",
                        "/uploads/**",
                        "/file/view/**"
                );


        // 角色权限验证
        registry.addInterceptor(roleInterceptor)

                .addPathPatterns("/**")

                .excludePathPatterns(
                        "/uploads/**",
                        "/file/view/**"
                );

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
    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry
    ) {

        String absolutePath =
                Paths.get(uploadPath)
                        .toAbsolutePath()
                        .normalize()
                        .toString();

        if (!absolutePath.endsWith(
                java.io.File.separator
        )) {

            absolutePath +=
                    java.io.File.separator;
        }

        registry.addResourceHandler(
                "/uploads/**"
        ).addResourceLocations(
                "file:" + absolutePath
        );
    }

}