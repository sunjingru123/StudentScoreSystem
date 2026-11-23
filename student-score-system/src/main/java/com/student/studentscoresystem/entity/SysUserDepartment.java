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
     * 组织内真实职位名称
     *
     * 例如：干事 / 副部长 / 部长 /
     * 站长 / 副站长 / 理事长 / 副理事长 /
     * 主席 / 副主席 / 主任 / 副主任
     */
    private String position;

    /**
     * 归一后的系统权限层级
     *
     * 干事 / 副部长 / 部长
     *
     * 所有权限判断一律看这个字段，
     * 界面上展示仍使用 {@link #position}。
     */
    private String positionLevel;

    private Short status;

    private LocalDateTime joinTime;
}