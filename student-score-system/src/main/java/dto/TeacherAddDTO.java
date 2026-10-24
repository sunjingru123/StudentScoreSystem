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

    /**
     * 岗位名称
     *
     * 默认「辅导员」；
     * 传入「高级辅导员」时创建高级辅导员账号，
     * 此时管理部门可以为空。
     */
    private String positionName;
}