package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.common.ScoreConstants;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.ScoreRuleMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.IScoreRecordService;
import com.student.studentscoresystem.vo.ScoreDetailVO;
import com.student.studentscoresystem.vo.ScoreStatisticsVO;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/scoreStatistics")
public class ScoreStatisticsController {

    private final IScoreRecordService scoreRecordService;

    private final ScoreRuleMapper scoreRuleMapper;

    private final SysUserMapper sysUserMapper;


    public ScoreStatisticsController(
            IScoreRecordService scoreRecordService,
            ScoreRuleMapper scoreRuleMapper,
            SysUserMapper sysUserMapper
    ) {
        this.scoreRecordService = scoreRecordService;
        this.scoreRuleMapper = scoreRuleMapper;
        this.sysUserMapper = sysUserMapper;
    }


    /**
     * =========================================================
     * 学生 / 辅导员查看成绩
     * =========================================================
     *
     * 只显示：
     *
     * status = 1
     * adminHidden = 0
     */
    @GetMapping("/{studentId}")
    public Result<ScoreStatisticsVO> detail(
            @PathVariable Long studentId
    ) {

        SysUser user =
                sysUserMapper.selectById(studentId);

        if (user == null) {
            return Result.fail("学生不存在");
        }


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
                                .orderByDesc(
                                        ScoreRecord::getCreateTime
                                )
                );


        return buildStatistics(
                user,
                records
        );
    }


    /**
     * =========================================================
     * 管理员查看学生成绩
     * =========================================================
     *
     * 管理员可以看到隐藏记录。
     *
     * 但是：
     *
     * status = 0 的作废记录
     * 仍然不参与统计。
     */
    @GetMapping("/admin/{studentId}")
    public Result<ScoreStatisticsVO> adminDetail(
            @PathVariable Long studentId
    ) {

        SysUser user =
                sysUserMapper.selectById(studentId);

        if (user == null) {
            return Result.fail("学生不存在");
        }


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
                                .orderByDesc(
                                        ScoreRecord::getCreateTime
                                )
                );


        return buildStatistics(
                user,
                records
        );
    }


    /**
     * =========================================================
     * 统一计算成绩
     * =========================================================
     *
     * 基础上限：
     *
     * 40分
     *
     * 正分：
     *
     * 累计加分
     *
     * 负分：
     *
     * 累计减分
     *
     * 最终：
     *
     * min(加分总和, 40 - 减分总和)
     */
    private Result<ScoreStatisticsVO> buildStatistics(
            SysUser user,
            List<ScoreRecord> records
    ) {

        ScoreStatisticsVO vo =
                new ScoreStatisticsVO();


        /*
         * 学生姓名
         */
        vo.setStudentName(
                user.getRealName()
        );


        /*
         * =====================================================
         * 1. 基础最高上限
         * =====================================================
         */
        BigDecimal baseLimit =
                ScoreConstants.MAX_SCORE;


        /*
         * =====================================================
         * 2. 加分
         * =====================================================
         */
        BigDecimal bonusScore =
                records.stream()
                        .map(
                                ScoreRecord::getScore
                        )
                        .filter(
                                score -> score != null
                        )
                        .filter(
                                score ->
                                        score.compareTo(
                                                BigDecimal.ZERO
                                        ) > 0
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );


        /*
         * =====================================================
         * 3. 减分
         * =====================================================
         */
        BigDecimal deductScore =
                records.stream()
                        .map(
                                ScoreRecord::getScore
                        )
                        .filter(
                                score -> score != null
                        )
                        .filter(
                                score ->
                                        score.compareTo(
                                                BigDecimal.ZERO
                                        ) < 0
                        )
                        .map(
                                BigDecimal::abs
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );


        /*
         * =====================================================
         * 4. 实际最高上限
         * =====================================================
         */
        BigDecimal actualLimit =
                baseLimit.subtract(
                        deductScore
                );


        /*
         * 上限最低为 0
         */
        if (
                actualLimit.compareTo(
                        BigDecimal.ZERO
                ) < 0
        ) {

            actualLimit =
                    BigDecimal.ZERO;
        }


        /*
         * =====================================================
         * 5. 最终成绩
         * =====================================================
         */
        BigDecimal totalScore =
                bonusScore.min(
                        actualLimit
                );


        /*
         * =====================================================
         * 6. 设置统计数据
         * =====================================================
         */
        vo.setBaseLimit(
                baseLimit
        );

        vo.setBonusScore(
                bonusScore
        );

        vo.setDeductScore(
                deductScore
        );

        vo.setActualLimit(
                actualLimit
        );

        vo.setTotalScore(
                totalScore
        );


        /*
         * =====================================================
         * 7. 兼容旧字段
         * =====================================================
         */
        double avgScore =
                records.stream()
                        .map(
                                ScoreRecord::getScore
                        )
                        .filter(
                                score -> score != null
                        )
                        .mapToDouble(
                                BigDecimal::doubleValue
                        )
                        .average()
                        .orElse(0);


        vo.setAvgScore(
                avgScore
        );


        int maxScore =
                records.stream()
                        .map(
                                ScoreRecord::getScore
                        )
                        .filter(
                                score -> score != null
                        )
                        .mapToInt(
                                BigDecimal::intValue
                        )
                        .max()
                        .orElse(0);


        vo.setMaxScore(
                maxScore
        );


        int minScore =
                records.stream()
                        .map(
                                ScoreRecord::getScore
                        )
                        .filter(
                                score -> score != null
                        )
                        .mapToInt(
                                BigDecimal::intValue
                        )
                        .min()
                        .orElse(0);


        vo.setMinScore(
                minScore
        );


        /*
         * =====================================================
         * 8. 成绩明细
         * =====================================================
         */
        List<ScoreDetailVO> detail =
                records.stream()
                        .map(
                                record -> {

                                    ScoreDetailVO d =
                                            new ScoreDetailVO();


                                    ScoreRule rule =
                                            scoreRuleMapper.selectById(
                                                    record.getRuleId()
                                            );


                                    if (rule != null) {

                                        d.setRuleName(
                                                rule.getName()
                                        );
                                    }


                                    d.setScore(
                                            record.getScore()
                                    );


                                    d.setSourceType(
                                            record.getSourceType()
                                    );


                                    d.setCreateTime(
                                            record.getCreateTime()
                                    );


                                    d.setAdminHidden(
                                            record.getAdminHidden()
                                    );


                                    return d;
                                }
                        )
                        .toList();


        vo.setDetail(
                detail
        );


        return Result.success(
                vo
        );
    }
}