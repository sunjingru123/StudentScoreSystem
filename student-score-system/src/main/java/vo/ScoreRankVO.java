package com.student.studentscoresystem.vo;


import lombok.Data;


@Data
public class ScoreRankVO {


    /**
     * 学生姓名
     */
    private String studentName;


    /**
     * 平均成绩
     */
    private Double avgScore;


    /**
     * 排名
     */
    private Integer rank;


}