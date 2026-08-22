package com.student.studentscoresystem.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.*;
import com.student.studentscoresystem.mapper.NoticeMessageMapper;
import com.student.studentscoresystem.service.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequestMapping("/scoreAudit")
public class ScoreAuditController {


    private final IScoreAuditService scoreAuditService;

    private final IScoreApplyService scoreApplyService;

    private final IScoreRecordService scoreRecordService;

    private final IScoreFlowService scoreFlowService;

    private final NoticeMessageMapper noticeMessageMapper;


    public ScoreAuditController(
            IScoreAuditService scoreAuditService,
            IScoreApplyService scoreApplyService,
            IScoreRecordService scoreRecordService,
            IScoreFlowService scoreFlowService,
            NoticeMessageMapper noticeMessageMapper
    ) {
        this.scoreAuditService = scoreAuditService;
        this.scoreApplyService = scoreApplyService;
        this.scoreRecordService = scoreRecordService;
        this.scoreFlowService = scoreFlowService;
        this.noticeMessageMapper = noticeMessageMapper;
    }


    /**
     * 查询待审核列表
     */
    @GetMapping("/pending")
    public Result<List<ScoreApply>> pending() {

        List<ScoreApply> list =
                scoreApplyService.list(
                        new LambdaQueryWrapper<ScoreApply>()
                                .eq(ScoreApply::getStatus, 0)
                );

        return Result.success(list);
    }


    /**
     * 审核
     *
     * status:
     * 1 通过
     * 2 驳回
     */
    @PutMapping("/audit")
    public Result<Void> audit(
            @RequestBody ScoreAudit dto
    ) {

        // 查询申请
        ScoreApply apply = scoreApplyService.getById(dto.getApplyId());

        if (apply == null) {
            return Result.fail("申请不存在");
        }

        /**
         * 1. 保存审核记录
         */
        ScoreAudit audit = new ScoreAudit();
        audit.setApplyId(apply.getId());
        audit.setAuditorId(1L);
        audit.setAuditStatus(dto.getAuditStatus());
        audit.setAuditComment(dto.getAuditComment());
        audit.setAuditTime(LocalDateTime.now());
        scoreAuditService.save(audit);


        /**
         * 2. 修改申请状态
         */
        apply.setStatus(dto.getAuditStatus());
        scoreApplyService.updateById(apply);


        /**
         * 3. 审核通过生成 score_record、score_flow
         */
        if (dto.getAuditStatus() == 1) {

            ScoreRecord record = new ScoreRecord();
            record.setStudentId(apply.getStudentId());
            record.setRuleId(apply.getRuleId());
            record.setScore(apply.getApplyScore());
            record.setSemesterId(1L);
            record.setSourceType("apply");
            record.setSourceId(apply.getId());
            scoreRecordService.save(record);


            // 查询学生当前已有综合测评分
            BigDecimal beforeScore =
                    scoreRecordService.list(
                                    new LambdaQueryWrapper<ScoreRecord>()
                                            .eq(ScoreRecord::getStudentId, apply.getStudentId())
                            )
                            .stream()
                            .map(ScoreRecord::getScore)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal changeScore = apply.getApplyScore();
            BigDecimal afterScore = beforeScore.add(changeScore);

            ScoreFlow flow = new ScoreFlow();
            flow.setStudentId(apply.getStudentId());
            flow.setBeforeScore(beforeScore);
            flow.setChangeScore(changeScore);
            flow.setAfterScore(afterScore);
            flow.setChangeType("apply");
            flow.setDescription("自主申报审核通过");
            scoreFlowService.save(flow);

        } // ✅闭合if(dto.getAuditStatus()==1)


        /**
         * 5. 发送通知：不管通过还是驳回，都要发通知，放到if外面！
         */
        NoticeMessage message = new NoticeMessage();
        message.setTitle("综合测评审核通知");

        if (dto.getAuditStatus() == 1) {
            message.setContent("你的综合测评申请已通过，获得" + apply.getApplyScore() + "分");
        } else {
            message.setContent("你的综合测评申请未通过：" + dto.getAuditComment());
        }

        message.setSenderId(1L);
        message.setReceiverId(apply.getStudentId());
        message.setReadStatus(0);
        noticeMessageMapper.insert(message);


        return Result.success(null);
    }

}