package com.student.studentscoresystem.dto;

import lombok.Data;

/**
 * 删除学生（软删除）请求参数
 */
@Data
public class StudentDeleteDTO {

    /**
     * 删除原因，必填
     */
    private String reason;

}
