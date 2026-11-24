package com.student.studentscoresystem.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 活动批量加分申报：解析结果（导入前预览）。
 */
@Data
public class ActivityBatchPreviewVO {

    /* =====================================================
     * 活动基本信息
     * ===================================================== */

    private String activityName;

    private String departmentName;

    private String activityDate;

    private String location;

    private String organizers;

    private String evidence;

    /* =====================================================
     * 学生明细
     * ===================================================== */

    private List<ActivityBatchRowVO> rows =
            new ArrayList<>();

    /**
     * 可导入行数
     */
    private int validCount;

    /**
     * 存在问题的行数
     */
    private int invalidCount;

    /**
     * 整体错误（例如活动信息不完整、无权限）。
     * 非空时不允许导入。
     */
    private String message;
}
