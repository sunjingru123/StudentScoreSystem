package com.student.studentscoresystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 系统公告表
 * </p>
 *
 * @author 茹茹宝贝
 * @since 2026-08-05
 */
@Getter
@Setter
@TableName("system_notice")
public class SystemNotice implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private Long publisherId;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
