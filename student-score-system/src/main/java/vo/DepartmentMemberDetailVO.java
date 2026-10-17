package com.student.studentscoresystem.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门成员明细（部门管理页使用）
 */
@Data
public class DepartmentMemberDetailVO {

    /**
     * sys_user_department 主键，用于移出成员
     */
    private Long id;

    private Long userId;

    private String studentNo;

    private String realName;

    private String className;

    private String position;

    private Short status;

    private LocalDateTime joinTime;

}
