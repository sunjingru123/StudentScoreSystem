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

    /**
     * 用户 ID
     */
    @TableId(
            value = "id",
            type = IdType.AUTO
    )
    private Long id;

    /**
     * 学号
     *
     * 教师账号也使用该字段保存内部编号。
     */
    private String studentNo;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * BCrypt 加密后的密码
     */
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 性别
     */
    private Short gender;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 班级
     */
    private String className;

    /**
     * 用户角色
     *
     * 数据库中不保存。
     *
     * 实际角色通过 sys_user_position 判断。
     */
    @TableField(exist = false)
    private String userRole;

    /**
     * 是否首次登录
     *
     * 1 = 首次登录，必须修改密码
     * 0 = 正常登录
     */
    private Short firstLogin;

    /**
     * 账号状态
     *
     * 1 = 正常
     * 0 = 停用
     */
    private Short status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}