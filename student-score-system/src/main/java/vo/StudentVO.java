package com.student.studentscoresystem.vo;


import lombok.Data;

@Data
public class StudentVO {


    private Long id;


    private String studentNo;


    private String username;


    private String realName;


    private String phone;


    private String className;


    private Short status;
    private Double totalScore;
    private Double bonusScore;
    private Double deductScore;
    private Double actualLimit;

}