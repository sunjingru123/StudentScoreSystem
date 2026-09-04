package com.student.studentscoresystem.vo;

import lombok.Data;

import java.util.List;

/**
 * 登录返回对象
 */
@Data
public class LoginVO {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 当前角色
     */
    private String role;

    /**
     * JWT Token
     */
    private String token;

    /**
     * 所属部门
     */
    private List<com.student.studentscoresystem.vo.DepartmentMemberVO> departments;

    /**
     * 是否首次登录
     *
     * true：
     *     必须修改密码
     *
     * false：
     *     正常进入系统
     */
    private Boolean firstLogin;
}