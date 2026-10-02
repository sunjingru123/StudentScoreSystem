package com.student.studentscoresystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.ScoreCalculator;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.SysSemester;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.service.IScoreStatisticsService;
import com.student.studentscoresystem.service.ISysSemesterService;
import com.student.studentscoresystem.vo.ScoreStatisticsVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * =========================================================
 * 学生成绩统计
 *
 * 统一使用 ScoreCalculator 的全系统口径：
 *
 *   totalScore = min(bonusScore, max(0, 40 - deductScore))
 *
 * 并且在统计前按学期（semesterId）过滤。
 * =========================================================
 */
@Service
public class ScoreStatisticsServiceImpl implements IScoreStatisticsService {

    private final ScoreRecordMapper scoreRecordMapper;

    private final ISysSemesterService sysSemesterService;

    public ScoreStatisticsServiceImpl(
            ScoreRecordMapper scoreRecordMapper,
            ISysSemesterService sysSemesterService
    ) {

        this.scoreRecordMapper =
                scoreRecordMapper;

        this.sysSemesterService =
                sysSemesterService;
    }

    /**
     * =========================================================
     * 按当前生效学期统计
     * =========================================================
     */
    @Override
    public ScoreStatisticsVO calculateStats(Long studentId) {

        SysSemester currentSemester =
                sysSemesterService.getCurrentSemester();

        return calculateStats(
                studentId,
                currentSemester == null
                        ? null
                        : currentSemester.getId()
        );
    }

    /**
     * =========================================================
     * 按指定学期统计
     *
     * semesterId 为 null 时统计全部记录。
     * =========================================================
     */
    @Override
    public ScoreStatisticsVO calculateStats(
            Long studentId,
            Long semesterId
    ) {

        ScoreStatisticsVO vo =
                new ScoreStatisticsVO();

        /*
         * 查询未隐藏的有效成绩记录，并按学期过滤。
         */
        List<ScoreRecord> records =
                scoreRecordMapper.selectList(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .eq(
                                        ScoreRecord::getStudentId,
                                        studentId
                                )
                                .eq(
                                        ScoreRecord::getAdminHidden,
                                        (short) 0
                                )
                                .eq(
                                        semesterId != null,
                                        ScoreRecord::getSemesterId,
                                        semesterId
                                )
                );

        List<BigDecimal> scores =
                records == null
                        ? Collections.emptyList()
                        : records.stream()
                        .map(ScoreRecord::getScore)
                        .toList();

        /*
         * 空值安全：ScoreCalculator 内部对 null 视为 0。
         */
        ScoreCalculator.Summary summary =
                ScoreCalculator.summarize(scores);

        vo.setBonusScore(
                summary.getBonusScore()
        );

        vo.setDeductScore(
                summary.getDeductScore()
        );

        vo.setTotalScore(
                summary.getTotalScore()
        );

        vo.setBaseLimit(
                summary.getBaseLimit()
        );

        vo.setActualLimit(
                summary.getActualLimit()
        );

        return vo;
    }
}
