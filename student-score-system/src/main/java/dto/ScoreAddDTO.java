package com.student.studentscoresystem.dto;


import lombok.Data;


@Data
public class ScoreAddDTO {


    /**
     * 学生id
     */
    private Long studentId;


    /**
     * 课程id
     */
    private Long courseId;


    /**
     * 成绩
     */
    private Integer score;


    /**
     * 学期
     */
    private String semester;


}