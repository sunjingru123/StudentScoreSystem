package com.student.studentscoresystem.vo;


import lombok.Data;

import java.math.BigDecimal;

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
    private BigDecimal totalScore;   // 综合评分
    private BigDecimal bonusScore;   // 可见加分
    private BigDecimal deductScore;  // 可见减分
    private BigDecimal actualLimit;  // 当前上限
}