package com.student.studentscoresystem.dto;


import lombok.Data;

import java.util.List;


/**
 * 批量终审请求
 *
 * 终审通过时不再需要填写分值，
 * 使用初审阶段已经认定的 applyScore。
 */
@Data
public class BatchFinalAuditDTO {


    /**
     * 待终审的申请id集合
     */
    private List<Long> ids;


}
