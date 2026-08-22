package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_department")
public class SysUserDepartment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long departmentId;

    /**
     * 干事 / 副部长 / 部长
     */
    private String position;

    private Short status;

    private LocalDateTime joinTime;
}