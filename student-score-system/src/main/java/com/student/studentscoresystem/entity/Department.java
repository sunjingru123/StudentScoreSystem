package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("department")
public class Department {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long teacherId;

    /**
     * 组织类型：
     *
     * 1 = 部门
     * 2 = 大社团
     * 3 = 小社团
     */
    private Short departmentType;

    /**
     * 上级组织 ID
     *
     * 仅小社团使用，指向所属大社团；
     * 部门与大社团为 null。
     */
    private Long parentId;

    private Short status;

    private LocalDateTime createTime;
}