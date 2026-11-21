package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
     *
     * 该字段禁止通过 JSON 序列化返回前端，防止敏感信息泄露。
     */
    @JsonIgnore
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
     * 软删除标识（学生回收站）
     *
     * 0 = 正常
     * 1 = 已删除
     *
     * 删除只做标记，不物理删除，
     * 历史成绩、加减分、获奖、活动参与和审核记录全部保留。
     */
    private Short deleted;

    /**
     * 删除时间
     */
    private LocalDateTime deletedTime;

    /**
     * 执行删除的管理员 ID
     */
    private Long deletedBy;

    /**
     * 删除原因
     */
    private String deleteReason;

    /**
     * 困难学生标识
     *
     * 1 = 困难学生
     * 0 = 普通学生（默认）
     *
     * 由辅导员端「导入困难学生名单」维护，
     * 用于比赛（个人证书）筛选与统计。
     */
    private Short povertyStatus;

    /**
     * 当前有效登录会话 ID
     *
     * 用于「同一账号不允许同时登录」：
     * 登录时写入新的会话 ID，Token 中携带同样的 sid，
     * 两者不一致即视为旧会话，立即失效。
     *
     * 属于内部状态，禁止通过 JSON 序列化返回前端。
     */
    @JsonIgnore
    private String activeSessionId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}