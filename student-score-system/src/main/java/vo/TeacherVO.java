package com.student.studentscoresystem.vo;

import lombok.Data;

import java.util.List;

@Data
public class TeacherVO {

    private Long id;

    /**
     * 老师姓名
     */
    private String realName;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 部门ID（首个负责部门，兼容旧前端）
     */
    private Long departmentId;

    /**
     * 部门名称（首个负责部门，兼容旧前端）
     */
    private String departmentName;

    /**
     * 负责部门ID列表
     *
     * 辅导员可以同时分管多个部门。
     */
    private List<Long> departmentIds;

    /**
     * 负责部门名称列表
     */
    private List<String> departmentNames;

    /**
     * 状态
     * 1正常 0停用
     */
    private Short status;

    /**
     * 岗位名称
     * 辅导员 / 高级辅导员
     */
    private String positionName;
}