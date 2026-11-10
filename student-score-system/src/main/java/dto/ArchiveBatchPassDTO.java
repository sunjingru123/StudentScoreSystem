package com.student.studentscoresystem.dto;

import lombok.Data;

import java.util.List;

/**
 * 档案部批量审批部门加减分申报的请求对象。
 *
 * ids 为空表示「一键通过全部待审核」，
 * 与原有行为保持一致；
 * 传入 ids 时只审批选中的申报。
 */
@Data
public class ArchiveBatchPassDTO {

    /**
     * 需要审批通过的申报 ID 列表
     */
    private List<Long> ids;
}
