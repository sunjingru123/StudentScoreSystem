package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 活动信息表
 * </p>
 *
 * @author 茹茹宝贝
 * @since 2026-08-05
 */
@Getter
@Setter
public class Activity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private String name;

    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long organizerId;

    private Short status;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
