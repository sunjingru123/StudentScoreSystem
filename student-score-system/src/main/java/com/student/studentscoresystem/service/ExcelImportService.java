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

    /**
     * 导入社团成员
     *
     * Excel 列：大社团名称 / 小社团名称 / 姓名 / 学号 / 职位
     */
    Map<String, Object> importClubMembers(
            MultipartFile file
    );

    /**
     * 生成「学生名单导入」Excel 模板
     */
    byte[] buildStudentTemplateWorkbook();

    /**
     * 生成「社团成员名单导入」Excel 模板
     */
    byte[] buildClubMemberTemplateWorkbook();

    /**
     * 生成「部门成员名单导入」Excel 模板
     */
    byte[] buildDepartmentMemberTemplateWorkbook();

    /**
     * 导入评分项目（规则）
     *
     * 部门 + 规则名称 已存在则更新，不存在则新增
     */
    Map<String, Object> importScoreRules(
            MultipartFile file
    );
}
