package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 系统用户表
 * </p>
 *
 * @author 茹茹宝贝
 * @since 2026-08-05
 */
@Getter
@Setter
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String studentNo;

    private String username;

    private String password;

    private String realName;

    private Short gender;

    private String phone;

    private String email;

    private String className;

    /** 用户角色：学生 / 档案部 / 管理员 */
    @TableField(exist = false)
    private String userRole;

    private Short status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}