package com.student.studentscoresystem.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


import java.time.LocalDateTime;


@Data
@TableName("course")
public class Course {


    /**
     * 课程ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * 课程名称
     */
    private String courseName;


    /**
     * 授课教师ID
     */
    private Long teacherId;


    /**
     * 学分
     */
    private Integer credit;


    /**
     * 创建时间
     */
    private LocalDateTime createTime;


}