package com.student.studentscoresystem.dto;

import lombok.Data;

/**
 * 部门管理页新增成员入参
 */
@Data
public class DepartmentMemberAddDTO {

    private Long userId;

    /**
     * 干事 / 副部长 / 部长
     */
    private String position;

}
