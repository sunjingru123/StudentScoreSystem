package com.student.studentscoresystem.service.impl;

import com.student.studentscoresystem.entity.ScoreAdminAdjustment;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.service.IScoreAdminAdjustmentService;
import com.student.studentscoresystem.service.IScoreRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ScoreAdminAdjustmentServiceImpl
        extends com.baomidou.mybatisplus.extension.service.impl.ServiceImpl<
        com.student.studentscoresystem.mapper.ScoreAdminAdjustmentMapper,
        ScoreAdminAdjustment
        >
        implements IScoreAdminAdjustmentService {

    private final IScoreRecordService scoreRecordService;


    public ScoreAdminAdjustmentServiceImpl(
            IScoreRecordService scoreRecordService
    ) {
        this.scoreRecordService =
                scoreRecordService;
    }


    /**
     * 管理员成绩调整
     *
     * 一个管理员调整操作：
     *
     * 1. 保存 score_admin_adjustment
     * 2. 生成 score_record
     *
     * 两步必须同时成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAdjustment(
            Long adminId,
            Long studentId,
            Short adjustType,
            BigDecimal score,
            String reason
    ) {

        /*
         * =====================================================
         * 1. 保存管理员调整记录
         * =====================================================
         */
        ScoreAdminAdjustment adjustment =
                new ScoreAdminAdjustment();

        adjustment.setStudentId(
                studentId
        );

        adjustment.setAdminId(
                adminId
        );

        adjustment.setAdjustType(
                adjustType
        );

        adjustment.setScore(
                score
        );

        adjustment.setReason(
                reason
        );

        adjustment.setCreateTime(
                LocalDateTime.now()
        );

        this.save(
                adjustment
        );


        /*
         * =====================================================
         * 2. 转换成 ScoreRecord
         * =====================================================
         *
         * adjustType：
         *
         * 1  → 加分
         * -1 → 减分
         */
        BigDecimal recordScore;

        if (adjustType == 1) {

            recordScore =
                    score;

        } else {

            recordScore =
                    score.negate();

        }


        /*
         * =====================================================
         * 3. 创建成绩记录
         * =====================================================
         */
        ScoreRecord record =
                new ScoreRecord();

        record.setStudentId(
                studentId
        );

        /*
         * 管理员手动调整不一定对应具体规则
         *
         * 因此这里先允许 ruleId = null
         */
        record.setRuleId(
                null
        );

        record.setScore(
                recordScore
        );

        /*
         * 默认有效
         */
        record.setStatus(
                (short) 1
        );

        /*
         * 默认不隐藏
         */
        record.setAdminHidden(
                (short) 0
        );

        /*
         * 来源
         */
        record.setSourceType(
                "ADMIN_ADJUSTMENT"
        );

        /*
         * sourceId 对应：
         *
         * score_admin_adjustment.id
         */
        record.setSourceId(
                adjustment.getId()
        );

        record.setCreateTime(
                LocalDateTime.now()
        );

        scoreRecordService.save(
                record
        );
    }
}