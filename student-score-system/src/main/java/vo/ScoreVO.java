package com.student.studentscoresystem.vo;


import lombok.Data;

import java.time.LocalDateTime;


@Data
public class ScoreVO {


    private Long id;


    private String studentName;


    private String courseName;


    private Integer score;


    private String semester;


    private LocalDateTime createTime;


}