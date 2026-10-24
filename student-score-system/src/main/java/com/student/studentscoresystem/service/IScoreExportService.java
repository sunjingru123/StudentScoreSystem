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


    /**
     * 按范围导出指定学期的加减分汇总。
     *
     * 与 {@link #export} 使用完全相同的 Excel 生成逻辑，
     * 保证导出格式一致。
     *
     * @param semesterId   学期ID
     * @param scope        范围：GRADE（全年级）/ DEPARTMENT（各部门）/ CLASS（各班）
     * @param grade        年级关键字，scope = GRADE 时使用，如「24级」
     * @param departmentId 部门ID，scope = DEPARTMENT 时使用
     * @param className    班级关键字，scope = CLASS 时使用
     * @param response     HTTP响应
     */
    void exportByScope(
            Long semesterId,
            String scope,
            String grade,
            Long departmentId,
            String className,
            HttpServletResponse response
    );
}
