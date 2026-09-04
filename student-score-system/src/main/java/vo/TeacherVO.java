package com.student.studentscoresystem.vo;

import lombok.Data;

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
     * 部门ID
     */
    private Long departmentId;

    /**
     * 部门名称
     */
    private String departmentName;

    /**
     * 状态
     * 1正常 0停用
     */
    private Short status;
}