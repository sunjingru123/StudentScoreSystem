package com.student.studentscoresystem.dto;

import lombok.Data;

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
     * 管理部门
     */
    private Long departmentId;
}