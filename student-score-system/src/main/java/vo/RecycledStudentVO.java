package com.student.studentscoresystem.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回收站中的学生信息
 *
 * 只用于管理员端「学生管理 → 回收站」，
 * 在 {@link StudentVO} 的基础上补充删除审计信息。
 */
@Data
public class RecycledStudentVO {

    /**
     * 用户（学生）ID
     */
    private Long id;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 登录用户名（通常等于学号）
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 班级
     */
    private String className;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 账号状态：1 = 正常，0 = 停用
     */
    private Short status;

    /**
     * 删除时间
     */
    private LocalDateTime deletedTime;

    /**
     * 执行删除的管理员 ID
     */
    private Long deletedBy;

    /**
     * 执行删除的管理员姓名
     */
    private String deletedByName;

    /**
     * 删除原因
     */
    private String deleteReason;

}
