package com.student.studentscoresystem.dto;

import lombok.Data;

import java.util.List;

@Data
public class TeacherAddDTO {

    /**
     * 老师姓名
     */
    private String realName;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 登录密码
     */
    private String password;

    /**
     * 管理部门（单个，兼容旧前端）
     *
     * 请优先使用 departmentIds。
     */
    private Long departmentId;

    /**
     * 管理部门（可多选）
     *
     * 辅导员可以同时分管多个部门。
     * departmentIds 优先，为空时回退到 departmentId。
     */
    private List<Long> departmentIds;

    /**
     * 岗位名称
     *
     * 默认「辅导员」；
     * 传入「高级辅导员」时创建高级辅导员账号，
     * 此时管理部门可以为空。
     */
    private String positionName;
}