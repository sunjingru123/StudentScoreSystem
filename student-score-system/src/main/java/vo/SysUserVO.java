package com.student.studentscoresystem.vo;


import lombok.Data;

@Data
public class SysUserVO {


    private Long id;


    private String studentNo;


    private String username;


    private String realName;


    private Short gender;


    private String phone;


    private String email;


    private String className;


    private Short status;

}