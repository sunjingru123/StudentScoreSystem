package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 部门加减分申报模板
 *
 * 每个部门拥有自己的加减分模板。
 *
 * 例如：
 * 学习部：
 *   - 迟到
 *   - 无故缺席学习活动
 *   - 获得学习类荣誉
 *
 * 生活部：
 *   - 宿舍卫生不合格
 *   - 宿舍检查优秀
 *
 * 不同部门之间的模板完全隔离。
 */
@Data
@TableName("department_score_template")
public class DepartmentScoreTemplate {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属部门ID
     */
    private Long departmentId;

    /**
     * 模板名称
     *
     * 例如：
     * 迟到
     * 无故缺席
     * 优秀志愿服务
     */
    private String name;

    /**
     * 模板说明
     *
     * 选择模板后，前端自动带出。
     */
    private String description;

    /**
     * 加减分类型
     *
     * 1  加分
     * -1 减分
     */
    private Short scoreType;

    /**
     * 默认分值
     *
     * 如果模板固定分值，可以直接使用。
     * 如果允许申报人调整，也可以作为默认值。
     */
    private BigDecimal score;

    /**
     * 是否启用
     *
     * 1 启用
     * 0 停用
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