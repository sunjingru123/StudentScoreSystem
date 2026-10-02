package com.student.studentscoresystem.service;

import com.student.studentscoresystem.entity.ScoreAudit;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 综合测评审核表 服务类
 * </p>
 *
 * @author 茹茹宝贝
 * @since 2026-08-05
 */
public interface IScoreAuditService extends IService<ScoreAudit> {

    /**
     * =========================================================
     * 审核综合测评申请
     *
     * 该方法在同一事务内完成：
     *
     * 1. 保存审核记录 score_audit
     * 2. 更新申请状态 score_apply
     * 3. 审核通过时生成成绩记录 score_record
     * 4. 审核通过时写入成绩流水 score_flow
     * 5. 发送审核通知 notice_message
     *
     * 任意一步异常都会整体回滚。
     *
     * @return null 表示成功；否则为业务失败提示
     * =========================================================
     */
    String auditApply(
            ScoreAudit dto,
            Long auditorId
    );

}
