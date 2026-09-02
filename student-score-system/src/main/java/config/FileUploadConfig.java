package com.student.studentscoresystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 文件访问配置
 *
 * 将：
 *
 * /uploads/**
 *
 * 映射到本地：
 *
 * uploads/
 *
 * 目录。
 */
@Configuration
public class FileUploadConfig
        implements WebMvcConfigurer {

    @Value("${file.upload-path:uploads}")
    private String uploadPath;


    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry
    ) {

        String absolutePath =
                Paths.get(
                                uploadPath
                        )
                        .toAbsolutePath()
                        .normalize()
                        .toUri()
                        .toString();


        registry
                .addResourceHandler(
                        "/uploads/**"
                )
                .addResourceLocations(
                        absolutePath
                );

    }

}