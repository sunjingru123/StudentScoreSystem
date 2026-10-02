package com.student.studentscoresystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.ScoreApply;
import com.student.studentscoresystem.entity.ScoreFlow;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.SysSemester;
import com.student.studentscoresystem.mapper.ScoreApplyMapper;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * =========================================================
 * 个人证书终审通过后的正式成绩生成
 *
 * 负责把「个人证书」终审通过落地为正式成绩记录
 * （score_record），并补齐成绩流水（score_flow）。
 *
 * 全部写操作放在同一个事务里，任何一步异常都会整体回滚，
 * 避免出现「成绩已生成但申请状态没更新」这类半截数据。
 * =========================================================
 */
@Service
public class CertificateScoreFinalizeService {

    private final ScoreRecordMapper scoreRecordMapper;

    private final ScoreApplyMapper scoreApplyMapper;

    private final IScoreFlowService scoreFlowService;

    private final ISysSemesterService sysSemesterService;

    public CertificateScoreFinalizeService(
            ScoreRecordMapper scoreRecordMapper,
            ScoreApplyMapper scoreApplyMapper,
            IScoreFlowService scoreFlowService,
            ISysSemesterService sysSemesterService
    ) {

        this.scoreRecordMapper =
                scoreRecordMapper;

        this.scoreApplyMapper =
                scoreApplyMapper;

        this.scoreFlowService =
                scoreFlowService;

        this.sysSemesterService =
                sysSemesterService;
    }

    /**
     * =========================================================
     * 终审通过
     *
     * @return null 表示成功，否则为业务失败提示
     * =========================================================
     */
    @Transactional(rollbackFor = Exception.class)
    public String finalizeApproval(
            ScoreApply apply,
            Long reviewerId
    ) {

        if (apply == null
                || apply.getId() == null) {

            return "申请不存在";
        }

        /*
         * 防御性判空：没有合法认定分值不允许生成成绩。
         */
        if (apply.getApplyScore() == null
                || apply.getApplyScore()
                .compareTo(
                        BigDecimal.ZERO
                ) <= 0) {

            return "该申请没有合法认定分值，无法终审";
        }

        /*
         * =====================================================
         * 查询当前学期
         *
         * 统一从 sys_semester 动态获取，严禁写死。
         * =====================================================
         */

        SysSemester semester =
                sysSemesterService.getCurrentSemester();

        if (semester == null) {

            return "当前没有可用学期，无法生成正式成绩记录";
        }

        /*
         * =====================================================
         * 防止重复生成 ScoreRecord
         * =====================================================
         */

        Long existCount =
                scoreRecordMapper.selectCount(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .eq(
                                        ScoreRecord::getSourceType,
                                        ScoreProjectNameResolver.TYPE_CERTIFICATE
                                )
                                .eq(
                                        ScoreRecord::getSourceId,
                                        apply.getId()
                                )
                );

        if (existCount != null
                && existCount > 0) {

            return "该证书已经生成正式成绩记录，不能重复审批";
        }

        /*
         * =====================================================
         * 创建正式成绩记录
         * =====================================================
         */

        ScoreRecord record =
                new ScoreRecord();

        record.setStudentId(
                apply.getStudentId()
        );

        /*
         * 个人证书暂时没有 ruleId
         */
        record.setRuleId(
                apply.getRuleId()
        );

        record.setScore(
                apply.getApplyScore()
        );

        record.setSemesterId(
                semester.getId()
        );

        record.setSourceType(
                ScoreProjectNameResolver.TYPE_CERTIFICATE
        );

        record.setSourceId(
                apply.getId()
        );

        record.setStatus(
                (short) 1
        );

        record.setAdminHidden(
                (short) 0
        );

        record.setCreateTime(
                LocalDateTime.now()
        );

        int insertResult =
                scoreRecordMapper.insert(
                        record
                );

        if (insertResult <= 0) {

            return "正式成绩生成失败";
        }

        /*
         * =====================================================
         * 写入成绩流水
         *
         * 与 ScoreAuditServiceImpl 的算法保持一致：
         * 先统计该学生当前可见总分，再减去本次变动得到修改前分数。
         * =====================================================
         */

        BigDecimal changeScore =
                apply.getApplyScore();

        BigDecimal beforeScore =
                scoreRecordMapper.selectList(
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
                beforeScore.add(
                        changeScore
                )
        );

        flow.setChangeType(
                ScoreProjectNameResolver.TYPE_CERTIFICATE
        );

        flow.setDescription(
                "个人证书终审通过"
        );

        flow.setCreateTime(
                LocalDateTime.now()
        );

        scoreFlowService.save(
                flow
        );

        /*
         * =====================================================
         * 更新申请最终状态
         * =====================================================
         */

        apply.setFinalStatus(
                (short) 1
        );

        apply.setFinalReviewerId(
                reviewerId
        );

        apply.setFinalReviewTime(
                LocalDateTime.now()
        );

        /*
         * 1 = 整个证书审核完成
         */
        apply.setStatus(
                (short) 1
        );

        apply.setUpdateTime(
                LocalDateTime.now()
        );

        scoreApplyMapper.updateById(
                apply
        );

        return null;
    }
}
