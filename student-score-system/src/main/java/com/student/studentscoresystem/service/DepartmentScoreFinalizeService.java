package com.student.studentscoresystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.entity.SysSemester;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.ScoreRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * =========================================================
 * 部门加减分申报终审通过后的正式成绩生成
 *
 * 负责把辅导员终审通过的部门加减分申报落地为正式成绩记录
 * （score_record），并同步更新申报单终审状态。
 *
 * 全部写操作放在同一个事务里，任何一步异常都会整体回滚，
 * 避免出现「成绩已生成但申报状态没更新」这类半截数据。
 * =========================================================
 */
@Service
public class DepartmentScoreFinalizeService {

    private final ScoreRecordMapper scoreRecordMapper;

    private final ScoreRuleMapper scoreRuleMapper;

    private final IDepartmentScoreApplyService applyService;

    private final ISysSemesterService sysSemesterService;

    public DepartmentScoreFinalizeService(
            ScoreRecordMapper scoreRecordMapper,
            ScoreRuleMapper scoreRuleMapper,
            IDepartmentScoreApplyService applyService,
            ISysSemesterService sysSemesterService
    ) {

        this.scoreRecordMapper =
                scoreRecordMapper;

        this.scoreRuleMapper =
                scoreRuleMapper;

        this.applyService =
                applyService;

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
            DepartmentScoreApply apply,
            Long reviewerId,
            String reviewRemark
    ) {

        if (apply == null
                || apply.getId() == null) {

            return "申报记录不存在";
        }

        /*
         * =====================================================
         * 防止重复生成正式成绩
         * =====================================================
         */

        Long recordCount =
                scoreRecordMapper.selectCount(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .eq(
                                        ScoreRecord::getSourceType,
                                        ScoreProjectNameResolver.TYPE_DEPARTMENT
                                )
                                .eq(
                                        ScoreRecord::getSourceId,
                                        apply.getId()
                                )
                );

        if (recordCount != null
                && recordCount > 0) {

            return "该申报已经生成正式成绩记录，请勿重复生成";
        }

        /*
         * =====================================================
         * 申报分值
         * =====================================================
         */

        BigDecimal realScore =
                apply.getScore();

        if (realScore == null) {

            return "该申报分值为空，无法生成成绩记录";
        }

        /*
         * 减分转负数
         */
        if (Short.valueOf((short) -1)
                .equals(
                        apply.getScoreType()
                )) {

            realScore =
                    realScore.negate();
        }

        /*
         * 当前学期
         */
        SysSemester currentSemester =
                getCurrentSemester();

        if (currentSemester == null) {

            return "当前没有正在进行的学期，无法生成正式成绩记录";
        }

        /*
         * =====================================================
         * 查找正式成绩规则
         *
         * 有正式规则（固定加减分项目）时要求申报分值与规则一致；
         * 没有正式规则说明是部门自己的非固定活动，
         * 分值以申报单上填写的为准。
         * =====================================================
         */

        ScoreRule scoreRule =
                scoreRuleMapper.selectOne(
                        new LambdaQueryWrapper<ScoreRule>()
                                .eq(
                                        ScoreRule::getDepartmentId,
                                        apply.getDepartmentId()
                                )
                                .eq(
                                        ScoreRule::getName,
                                        apply.getTitle()
                                )
                                .eq(
                                        ScoreRule::getStatus,
                                        (short) 1
                                )
                                /*
                                 * 同一部门规则名称已由唯一索引保证唯一，
                                 * 这里再固定主键排序，避免匹配漂移。
                                 */
                                .orderByAsc(
                                        ScoreRule::getId
                                )
                                .last(
                                        "LIMIT 1"
                                )
                );

        if (scoreRule != null) {

            if (scoreRule.getScore() == null
                    || scoreRule.getScore()
                    .compareTo(
                            BigDecimal.ZERO
                    ) <= 0) {

                return "正式加减分规则分值配置错误：" +
                        scoreRule.getName();
            }

            if (apply.getScore() == null
                    || scoreRule.getScore()
                    .compareTo(
                            apply.getScore()
                    ) != 0) {

                return "部门申报分值与正式加减分规则分值不一致：" +
                        "申报分值=" +
                        apply.getScore() +
                        "，正式规则分值=" +
                        scoreRule.getScore();
            }
        }

        LocalDateTime now =
                LocalDateTime.now();

        /*
         * =====================================================
         * 创建正式成绩
         * =====================================================
         */

        ScoreRecord record =
                new ScoreRecord();

        record.setStudentId(
                apply.getStudentId()
        );

        /*
         * 非固定活动没有正式规则，score_record.rule_id 允许为空。
         */
        record.setRuleId(
                scoreRule == null
                        ? null
                        : scoreRule.getId()
        );

        record.setScore(
                realScore
        );

        record.setSemesterId(
                currentSemester.getId()
        );

        record.setSourceType(
                ScoreProjectNameResolver.TYPE_DEPARTMENT
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
                now
        );

        int result =
                scoreRecordMapper.insert(
                        record
                );

        if (result <= 0) {

            return "终审通过，但成绩记录生成失败";
        }

        /*
         * =====================================================
         * 更新申报终审状态
         * =====================================================
         */

        apply.setFinalStatus(
                (short) 1
        );

        apply.setFinalReviewerId(
                reviewerId
        );

        apply.setFinalReviewRemark(
                reviewRemark
        );

        apply.setFinalReviewTime(
                now
        );

        apply.setUpdateTime(
                now
        );

        if (!applyService.updateById(
                apply
        )) {

            return "终审状态更新失败";
        }

        return null;
    }

    /**
     * =========================================================
     * 查询当前正在进行的学期
     *
     * 统一从 sys_semester 动态获取，严禁写死。
     * =========================================================
     */
    private SysSemester getCurrentSemester() {

        return sysSemesterService.getCurrentSemester();
    }
}
