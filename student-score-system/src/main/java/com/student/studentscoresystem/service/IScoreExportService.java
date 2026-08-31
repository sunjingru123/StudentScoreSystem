package com.student.studentscoresystem.service;

import jakarta.servlet.http.HttpServletResponse;

public interface IScoreExportService {

    /**
     * 导出指定学期、指定班级的部门加减分汇总
     *
     * @param semesterId 学期ID
     * @param className 班级名称
     * @param response HTTP响应
     */
    void export(
            Long semesterId,
            String className,
            HttpServletResponse response
    );
}