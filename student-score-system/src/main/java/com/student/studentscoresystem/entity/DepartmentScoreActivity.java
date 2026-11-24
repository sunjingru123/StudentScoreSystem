package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 活动批量加分申报：活动基本信息。
 *
 * 一场活动对应一条记录，
 * 每名学生的加分明细各自生成一条 {@link DepartmentScoreApply}，
 * 通过 activityId 关联，独立审核、独立成统。
 */
@Data
@TableName("department_score_activity")
public class DepartmentScoreActivity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 活动名称（同时作为明细的加减分项目名称）
     */
    private String name;

    /**
     * 举办部门 / 组织 ID
     */
    private Long departmentId;

    /**
     * 申报人（组织人员）
     */
    private Long organizerId;

    /**
     * 举办时间
     */
    private LocalDate activityDate;

    /**
     * 活动地点
     */
    private String location;

    /**
     * 组织人员（文本，取自模板）
     */
    private String organizers;

    /**
     * 活动凭证（可选，学生未单独填写时使用）
     */
    private String evidenceUrl;

    private String description;

    private LocalDateTime createTime;
}
