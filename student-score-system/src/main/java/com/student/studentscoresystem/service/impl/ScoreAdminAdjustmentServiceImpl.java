package com.student.studentscoresystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.student.studentscoresystem.entity.ScoreAdminAdjustment;
import com.student.studentscoresystem.entity.ScoreFlow;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.mapper.ScoreAdminAdjustmentMapper;
import com.student.studentscoresystem.service.IScoreAdminAdjustmentService;
import com.student.studentscoresystem.service.IScoreFlowService;
import com.student.studentscoresystem.service.IScoreRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScoreAdminAdjustmentServiceImpl
        extends ServiceImpl<ScoreAdminAdjustmentMapper, ScoreAdminAdjustment>
        implements IScoreAdminAdjustmentService {

    /**
     * 成绩记录 Service
     */
    private final IScoreRecordService scoreRecordService;

    /**
     * 成绩流水 Service
     */
    private final IScoreFlowService scoreFlowService;


    public ScoreAdminAdjustmentServiceImpl(
            IScoreRecordService scoreRecordService,
            IScoreFlowService scoreFlowService
    ) {

        this.scoreRecordService =
                scoreRecordService;

        this.scoreFlowService =
                scoreFlowService;
    }


    /**
     * =========================================================
     * 管理员成绩调整
     * =========================================================
     *
     * 一个管理员调整操作：
     *
     * 1. 保存 ScoreAdminAdjustment
     *
     * 2. 创建 ScoreRecord
     *
     * 3. 创建 ScoreFlow
     *
     * 三个操作必须全部成功，
     * 任意一步失败全部回滚。
     *
     * adjustType：
     *
     * 1  = 加分
     * -1 = 减分
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
         * 1. 参数保护
         * =====================================================
         */

        if (adminId == null) {
            throw new IllegalArgumentException(
                    "管理员ID不能为空"
            );
        }

        if (studentId == null) {
            throw new IllegalArgumentException(
                    "学生ID不能为空"
            );
        }

        if (adjustType == null) {
            throw new IllegalArgumentException(
                    "调整类型不能为空"
            );
        }

        if (
                adjustType != 1
                        && adjustType != -1
        ) {
            throw new IllegalArgumentException(
                    "调整类型错误"
            );
        }

        if (
                score == null
                        || score.compareTo(
                        BigDecimal.ZERO
                ) <= 0
        ) {
            throw new IllegalArgumentException(
                    "调整分数必须大于0"
            );
        }


        /*
         * =====================================================
         * 2. 查询当前有效成绩
         * =====================================================
         *
         * 注意：
         *
         * ScoreRecord 中：
         *
         * status = 1
         * 表示有效
         *
         * adminHidden = 0
         * 表示正常显示
         *
         * 这里计算的是调整之前的总成绩。
         */
        BigDecimal beforeScore =
                calculateCurrentScore(
                        studentId
                );


        /*
         * =====================================================
         * 3. 保存 ScoreAdminAdjustment
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


        /*
         * 保存后：
         *
         * adjustment.getId()
         *
         * 就是数据库生成的主键。
         */
        boolean adjustmentSaved =
                this.save(
                        adjustment
                );

        if (!adjustmentSaved) {

            throw new IllegalStateException(
                    "保存成绩调整记录失败"
            );
        }


        /*
         * =====================================================
         * 4. 计算真正进入 ScoreRecord 的分数
         * =====================================================
         *
         * 加分：
         *
         * 10 → +10
         *
         * 减分：
         *
         * 10 → -10
         */
        BigDecimal changeScore;

        if (adjustType == 1) {

            changeScore =
                    score;

        } else {

            changeScore =
                    score.negate();
        }


        /*
         * =====================================================
         * 5. 计算调整后的总成绩
         * =====================================================
         */
        BigDecimal afterScore =
                beforeScore.add(
                        changeScore
                );


        /*
         * =====================================================
         * 6. 创建 ScoreRecord
         * =====================================================
         */
        ScoreRecord record =
                new ScoreRecord();


        /*
         * 学生
         */
        record.setStudentId(
                studentId
        );


        /*
         * 管理员手动调整没有对应规则
         */
        record.setRuleId(
                null
        );


        /*
         * 实际分数：
         *
         * 加分 → 正数
         * 减分 → 负数
         */
        record.setScore(
                changeScore
        );


        /*
         * 有效
         */
        record.setStatus(
                (short) 1
        );


        /*
         * 正常显示
         */
        record.setAdminHidden(
                (short) 0
        );


        /*
         * 管理员调整来源
         */
        record.setSourceType(
                "ADMIN_ADJUSTMENT"
        );


        /*
         * 对应 score_admin_adjustment.id
         */
        record.setSourceId(
                adjustment.getId()
        );


        /*
         * 管理员调整不绑定学期。
         *
         * 如果以后要求必须绑定学期，
         * 再从当前用户或请求参数中补充。
         */
        record.setSemesterId(
                null
        );


        record.setCreateTime(
                LocalDateTime.now()
        );


        /*
         * 保存 ScoreRecord
         */
        boolean recordSaved =
                scoreRecordService.save(
                        record
                );

        if (!recordSaved) {

            throw new IllegalStateException(
                    "保存成绩记录失败"
            );
        }


        /*
         * =====================================================
         * 7. 创建 ScoreFlow
         * =====================================================
         *
         * ScoreFlow 用于保存：
         *
         * 调整前：
         * beforeScore
         *
         * 本次变化：
         * changeScore
         *
         * 调整后：
         * afterScore
         */
        ScoreFlow flow =
                new ScoreFlow();


        /*
         * 学生
         */
        flow.setStudentId(
                studentId
        );


        /*
         * 调整前
         */
        flow.setBeforeScore(
                beforeScore
        );


        /*
         * 本次变化
         *
         * 加分 → 正数
         * 减分 → 负数
         */
        flow.setChangeScore(
                changeScore
        );


        /*
         * 调整后
         */
        flow.setAfterScore(
                afterScore
        );


        /*
         * 变化类型
         */
        flow.setChangeType(
                "ADMIN_ADJUSTMENT"
        );


        /*
         * 描述
         */
        String description =
                reason;

        if (
                description == null
                        || description.trim().isEmpty()
        ) {

            description =
                    adjustType == 1
                            ? "管理员成绩加分"
                            : "管理员成绩减分";
        }

        flow.setDescription(
                description.trim()
        );


        /*
         * 创建时间
         */
        flow.setCreateTime(
                LocalDateTime.now()
        );


        /*
         * 保存流水
         */
        boolean flowSaved =
                scoreFlowService.save(
                        flow
                );

        if (!flowSaved) {

            throw new IllegalStateException(
                    "保存成绩流水失败"
            );
        }
    }


    /**
     * =========================================================
     * 计算学生当前有效成绩
     * =========================================================
     *
     * 计算规则：
     *
     * studentId = 当前学生
     *
     * status = 1
     *
     * adminHidden = 0
     *
     * 将所有 ScoreRecord.score 相加。
     */
    private BigDecimal calculateCurrentScore(
            Long studentId
    ) {

        List<ScoreRecord> records =
                scoreRecordService.list(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .eq(
                                        ScoreRecord::getStudentId,
                                        studentId
                                )
                                .eq(
                                        ScoreRecord::getStatus,
                                        (short) 1
                                )
                                .eq(
                                        ScoreRecord::getAdminHidden,
                                        (short) 0
                                )
                );


        /*
         * 没有成绩记录
         *
         * 当前总成绩就是 0。
         */
        if (
                records == null
                        || records.isEmpty()
        ) {

            return BigDecimal.ZERO;
        }


        /*
         * 所有有效成绩求和
         */
        return records.stream()
                .map(
                        ScoreRecord::getScore
                )
                .filter(
                        score ->
                                score != null
                )
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }
}