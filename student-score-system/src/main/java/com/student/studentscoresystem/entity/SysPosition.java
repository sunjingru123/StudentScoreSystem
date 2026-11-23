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
 * 岗位信息表
 * </p>
 *
 * @author 茹茹宝贝
 * @since 2026-08-05
 */
@Getter
@Setter
@TableName("sys_position")
public class SysPosition implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    /**
     * 岗位对应的系统权限层级
     *
     * 例如：站长 / 理事长 / 主席 / 主任 = 部长，
     *       副站长 / 副理事长 / 副主席 / 副主任 = 副部长。
     *
     * RoleInterceptor 同时匹配 name 与 level，
     * 因此同一层级的不同职位名称拥有完全一致的系统权限。
     */
    private String level;

    private String description;

    private Short status;

    private LocalDateTime createTime;
}
