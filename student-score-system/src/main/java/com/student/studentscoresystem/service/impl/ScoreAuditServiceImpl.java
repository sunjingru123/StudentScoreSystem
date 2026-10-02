package com.student.studentscoresystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.NoticeMessage;
import com.student.studentscoresystem.entity.ScoreApply;
import com.student.studentscoresystem.entity.ScoreAudit;
import com.student.studentscoresystem.entity.ScoreFlow;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.mapper.NoticeMessageMapper;
import com.student.studentscoresystem.mapper.ScoreAuditMapper;
import com.student.studentscoresystem.service.IScoreApplyService;
import com.student.studentscoresystem.service.IScoreAuditService;
import com.student.studentscoresystem.service.IScoreFlowService;
import com.student.studentscoresystem.service.IScoreRecordService;
import com.student.studentscoresystem.service.ScoreProjectNameResolver;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 综合测评审核表 服务实现类
 * </p>
 *
 * @author 茹茹宝贝
 * @since 2026-08-05
 */
@Service
public class ScoreAuditServiceImpl extends ServiceImpl<ScoreAuditMapper, ScoreAudit> implements IScoreAuditService {

    private final IScoreApplyService scoreApplyService;

    private final IScoreRecordService scoreRecordService;

    private final IScoreFlowService scoreFlowService;

    private final NoticeMessageMapper noticeMessageMapper;

    public ScoreAuditServiceImpl(
            IScoreApplyService scoreApplyService,
            IScoreRecordService scoreRecordService,
            IScoreFlowService scoreFlowService,
            NoticeMessageMapper noticeMessageMapper
    ) {

        this.scoreApplyService =
                scoreApplyService;

        this.scoreRecordService =
                scoreRecordService;

        this.scoreFlowService =
                scoreFlowService;

        this.noticeMessageMapper =
                noticeMessageMapper;
    }

    /**
     * =========================================================
     * 审核综合测评申请
     *
     * 整段多表写操作放在同一个事务里，
     * 任何一步异常都会回滚 score_audit / score_apply /
     * score_record / score_flow / notice_message。
     * =========================================================
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String auditApply(
            ScoreAudit dto,
            Long auditorId
    ) {

        /*
         * =====================================================
         * 1. 参数校验
         * =====================================================
         */

        if (dto == null) {

            return "审核参数不能为空";
        }

        if (dto.getApplyId() == null) {

            return "申请ID不能为空";
        }

        if (dto.getAuditStatus() == null
                || (dto.getAuditStatus() != 1
                && dto.getAuditStatus() != 2)) {

            return "审核状态错误";
        }

        /*
         * =====================================================
         * 2. 查询申请
         * =====================================================
         */

        ScoreApply apply =
                scoreApplyService.getById(
                        dto.getApplyId()
                );

        if (apply == null) {

            return "申请不存在";
        }

        /*
         * =====================================================
         * 3. 防止重复审核
         * =====================================================
         */

        if (!Short.valueOf((short) 0)
                .equals(apply.getStatus())) {

            return "该申请已经审核，不可重复操作";
        }

        /*
         * =====================================================
         * 4. 禁止自己审核自己的申请
         * =====================================================
         */

        if (auditorId != null
                && auditorId.equals(
                        apply.getStudentId()
                )) {

            return "不能审核自己提交的申请";
        }

        /*
         * =====================================================
         * 5. 分值防御性校验
         *
         * 审核通过必须要有合法分值，
         * 否则会在计算流水时出现空指针。
         * 这里在写任何数据之前先阻断。
         * =====================================================
         */

        if (dto.getAuditStatus() == 1) {

            if (apply.getApplyScore() == null) {

                return "该申请没有认定分值，无法审核通过";
            }

            if (apply.getApplyScore()
                    .compareTo(
                            BigDecimal.ZERO
                    ) <= 0) {

                return "申请分值必须大于0，无法审核通过";
            }
        }

        /*
         * =====================================================
         * 6. 保存审核记录
         * =====================================================
         */

        ScoreAudit audit =
                new ScoreAudit();

        audit.setApplyId(
                apply.getId()
        );

        audit.setAuditorId(
                auditorId
        );

        audit.setAuditStatus(
                dto.getAuditStatus()
        );

        audit.setAuditComment(
                dto.getAuditComment()
        );

        audit.setAuditTime(
                LocalDateTime.now()
        );

        save(audit);

        /*
         * =====================================================
         * 7. 修改申请状态
         * =====================================================
         */

        apply.setStatus(
                dto.getAuditStatus()
        );

        apply.setUpdateTime(
                LocalDateTime.now()
        );

        scoreApplyService.updateById(
                apply
        );

        /*
         * =====================================================
         * 8. 驳回
         * =====================================================
         */

        if (dto.getAuditStatus() == 2) {

            sendNotice(
                    apply,
                    false,
                    dto.getAuditComment(),
                    auditorId
            );

            return null;
        }

        /*
         * =====================================================
         * 9. 审核通过
         * =====================================================
         */

        Long existRecord =
                scoreRecordService.count(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .eq(
                                        ScoreRecord::getSourceType,
                                        ScoreProjectNameResolver.TYPE_APPLY
                                )
                                .eq(
                                        ScoreRecord::getSourceId,
                                        apply.getId()
                                )
                );

        /*
         * 已经生成过成绩记录，直接返回，防止重复加分。
         */
        if (existRecord != null
                && existRecord > 0) {

            sendNotice(
                    apply,
                    true,
                    null,
                    auditorId
            );

            return null;
        }

        /*
         * =====================================================
         * 10. 创建 ScoreRecord
         * =====================================================
         */

        ScoreRecord record =
                new ScoreRecord();

        record.setStudentId(
                apply.getStudentId()
        );

        record.setRuleId(
                apply.getRuleId()
        );

        record.setScore(
                apply.getApplyScore()
        );

        record.setSemesterId(
                1L
        );

        record.setStatus(
                (short) 1
        );

        record.setAdminHidden(
                (short) 0
        );

        record.setSourceType(
                ScoreProjectNameResolver.TYPE_APPLY
        );

        record.setSourceId(
                apply.getId()
        );

        record.setCreateTime(
                LocalDateTime.now()
        );

        scoreRecordService.save(record);

        /*
         * =====================================================
         * 11. 计算 ScoreFlow
         * =====================================================
         */

        BigDecimal beforeScore =
                scoreRecordService.list(
                                new LambdaQueryWrapper<ScoreRecord>()
                                        .eq(
                                                ScoreRecord::getStudentId,
                                                apply.getStudentId()
                                        )
                                        .eq(
                                                ScoreRecord::getAdminHidden,
                                                (short) 0
                                        )
                        )
                        .stream()
                        .map(
                                ScoreRecord::getScore
                        )
                        .filter(
                                score -> score != null
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        )
                        .subtract(
                                apply.getApplyScore()
                        );

        BigDecimal changeScore =
                apply.getApplyScore();

        BigDecimal afterScore =
                beforeScore.add(
                        changeScore
                );

        ScoreFlow flow =
                new ScoreFlow();

        flow.setStudentId(
                apply.getStudentId()
        );

        flow.setBeforeScore(
                beforeScore
        );

        flow.setChangeScore(
                changeScore
        );

        flow.setAfterScore(
                afterScore
        );

        flow.setChangeType(
                ScoreProjectNameResolver.TYPE_APPLY
        );

        flow.setDescription(
                "自主申报审核通过"
        );

        flow.setCreateTime(
                LocalDateTime.now()
        );

        scoreFlowService.save(
                flow
        );

        /*
         * =====================================================
         * 12. 发送通知
         * =====================================================
         */

        sendNotice(
                apply,
                true,
                null,
                auditorId
        );

        return null;
    }

    /**
     * =========================================================
     * 发送审核通知
     * =========================================================
     */
    private void sendNotice(
            ScoreApply apply,
            boolean pass,
            String remark,
            Long senderId
    ) {

        NoticeMessage message =
                new NoticeMessage();

        message.setTitle(
                "综合测评审核通知"
        );

        if (pass) {

            message.setContent(
                    "你的综合测评申请已通过，获得"
                            + apply.getApplyScore()
                            + "分"
            );

        } else {

            String content =
                    "你的综合测评申请未通过";

            if (remark != null
                    && !remark.trim().isEmpty()) {

                content +=
                        "：" + remark.trim();
            }

            message.setContent(
                    content
            );
        }

        message.setSenderId(
                senderId
        );

        message.setReceiverId(
                apply.getStudentId()
        );

        message.setReadStatus(0);

        noticeMessageMapper.insert(
                message
        );
    }

}
