package com.student.studentscoresystem.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@TableName("score_audit")
public class ScoreAudit {


    @TableId(
            value = "id",
            type = IdType.AUTO
    )
    private Long id;


    /**
     * 申请id
     */
    private Long applyId;


    /**
     * 审核人
     */
    private Long auditorId;


    /**
     * 审核状态
     * 0待审核
     * 1通过
     * 2拒绝
     */
    private Short auditStatus;


    /**
     * 审核意见
     */
    private String auditComment;


    /**
     * 审核时间
     */
    private LocalDateTime auditTime;


}