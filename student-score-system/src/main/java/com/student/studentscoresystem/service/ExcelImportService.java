package com.student.studentscoresystem.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface ExcelImportService {

    /**
     * 导入学生名单
     */
    Map<String, Object> importStudents(
            MultipartFile file
    );

    /**
     * 导入部门成员
     */
    Map<String, Object> importDepartmentMembers(
            MultipartFile file
    );
}