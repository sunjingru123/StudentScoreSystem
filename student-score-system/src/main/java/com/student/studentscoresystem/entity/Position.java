package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_position")
public class Position {

    @TableId
    private Long id;

    private String name;

    private String description;

    private Integer status;

    private LocalDateTime createTime;
}