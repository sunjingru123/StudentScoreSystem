package com.student.studentscoresystem.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;


@Data
@TableName("score")
public class Score {


    @TableId(type = IdType.AUTO)
    private Long id;


    private Long studentId;


    private Long courseId;


    private Integer score;


    private String semester;


    private LocalDateTime createTime;

}