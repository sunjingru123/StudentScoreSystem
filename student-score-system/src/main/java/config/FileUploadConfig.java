package com.student.studentscoresystem.config;

import org.springframework.context.annotation.Configuration;

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
        {
    // 不再将 uploads 映射为匿名静态目录，文件统一经 FileInfoController 鉴权后读取。
}
