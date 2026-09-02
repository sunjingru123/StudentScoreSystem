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
     * 管理员隐藏的成绩：
     *
     * 1. 不显示
     * 2. 不参与加分
     * 3. 不参与减分
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


        /*
         * 学生和辅导员只能看到：
         *
         * admin_hidden = 0
         */
        List<ScoreRecord> records =
                scoreRecordService.list(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .eq(
                                        ScoreRecord::getStudentId,
                                        studentId
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
     * 管理员需要看到全部成绩：
     *
     * 1. 正常成绩
     * 2. 已隐藏成绩
     *
     * 但是：
     *
     * 已隐藏成绩不能参与综合评分。
     *
     * 所以这里分成：
     *
     * allRecords
     *     ↓
     *     管理员看到的全部明细
     *
     * visibleRecords
     *     ↓
     *     参与综合评分
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


        /*
         * =====================================================
         * 1. 查询全部成绩
         * =====================================================
         *
         * 管理员必须能够看到隐藏成绩。
         */
        List<ScoreRecord> allRecords =
                scoreRecordService.list(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .eq(
                                        ScoreRecord::getStudentId,
                                        studentId
                                )
                                .orderByDesc(
                                        ScoreRecord::getCreateTime
                                )
                );


        /*
         * =====================================================
         * 2. 只筛选正常成绩参与统计
         * =====================================================
         *
         * adminHidden：
         *
         * 0 = 正常
         * 1 = 隐藏
         *
         * 兼容数据库中可能存在 NULL 的情况：
         *
         * NULL 也视为正常。
         */
        List<ScoreRecord> visibleRecords =
                allRecords.stream()
                        .filter(record ->
                                record.getAdminHidden() == null
                                        ||
                                        record.getAdminHidden() == 0
                        )
                        .toList();


        /*
         * =====================================================
         * 3. 用正常成绩计算综合评分
         * =====================================================
         */
        Result<ScoreStatisticsVO> result =
                buildStatistics(
                        user,
                        visibleRecords
                );


        if (result.getData() == null) {
            return result;
        }


        ScoreStatisticsVO vo =
                result.getData();


        /*
         * =====================================================
         * 4. 管理员明细使用全部成绩
         * =====================================================
         *
         * 注意：
         *
         * 这里不能使用 visibleRecords。
         *
         * 否则隐藏成绩会直接消失。
         */
        List<ScoreDetailVO> detail =
                allRecords.stream()
                        .map(record -> {

                            ScoreDetailVO d =
                                    new ScoreDetailVO();


                            /*
                             * 成绩记录 ID
                             */
                            d.setId(
                                    record.getId()
                            );


                            /*
                             * 来源业务 ID
                             */
                            d.setSourceId(
                                    record.getSourceId()
                            );


                            /*
                             * 查询评分项目名称
                             */
                            ScoreRule rule =
                                    scoreRuleMapper.selectById(
                                            record.getRuleId()
                                    );

                            if (rule != null) {

                                d.setRuleName(
                                        rule.getName()
                                );

                            }


                            /*
                             * 分数
                             */
                            d.setScore(
                                    record.getScore()
                            );


                            /*
                             * 来源
                             */
                            d.setSourceType(
                                    record.getSourceType()
                            );


                            /*
                             * 创建时间
                             */
                            d.setCreateTime(
                                    record.getCreateTime()
                            );


                            /*
                             * =================================================
                             * 最关键：
                             *
                             * 把 adminHidden 返回给前端
                             *
                             * 0 = 正常
                             * 1 = 已隐藏
                             * =================================================
                             */
                            d.setAdminHidden(
                                    record.getAdminHidden()
                            );


                            return d;

                        })
                        .toList();


        /*
         * 将完整明细放回 VO
         */
        vo.setDetail(
                detail
        );


        return Result.success(
                vo
        );
    }


    /**
     * =========================================================
     * 统一计算综合评分
     * =========================================================
     *
     * 计算规则：
     *
     * 基础最高上限 = 40
     *
     * 减分：
     *
     * actualLimit = 40 - 减分总和
     *
     * 加分：
     *
     * bonusScore = 所有正常加分之和
     *
     * 最终成绩：
     *
     * totalScore =
     * min(
     *     bonusScore,
     *     actualLimit
     * )
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
         * 2. 计算加分
         * =====================================================
         */
        BigDecimal bonusScore =
                records.stream()
                        .map(
                                ScoreRecord::getScore
                        )
                        .filter(
                                score ->
                                        score != null
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
         * 3. 计算减分
         * =====================================================
         *
         * 数据库：
         *
         * -5
         * -3
         *
         * 统计：
         *
         * deductScore = 8
         */
        BigDecimal deductScore =
                records.stream()
                        .map(
                                ScoreRecord::getScore
                        )
                        .filter(
                                score ->
                                        score != null
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
         * 4. 当前最高上限
         * =====================================================
         */
        BigDecimal actualLimit =
                baseLimit.subtract(
                        deductScore
                );


        /*
         * 上限最低不能低于 0
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
         * 6. 设置统计结果
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
         * 7. 兼容旧前端字段
         * =====================================================
         */
        double avgScore =
                records.stream()
                        .map(
                                ScoreRecord::getScore
                        )
                        .filter(
                                score ->
                                        score != null
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
                                score ->
                                        score != null
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
                                score ->
                                        score != null
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
         * 8. 普通情况下的成绩明细
         * =====================================================
         *
         * 学生 / 辅导员调用这里时：
         *
         * records 本身就是 adminHidden = 0
         *
         * 所以不会看到隐藏成绩。
         */
        List<ScoreDetailVO> detail =
                records.stream()
                        .map(record -> {

                            ScoreDetailVO d =
                                    new ScoreDetailVO();


                            /*
                             * 成绩记录 ID
                             */
                            d.setId(
                                    record.getId()
                            );


                            /*
                             * 来源业务 ID
                             */
                            d.setSourceId(
                                    record.getSourceId()
                            );


                            /*
                             * 评分项目名称
                             */
                            ScoreRule rule =
                                    scoreRuleMapper.selectById(
                                            record.getRuleId()
                                    );

                            if (rule != null) {

                                d.setRuleName(
                                        rule.getName()
                                );

                            }


                            /*
                             * 分数
                             */
                            d.setScore(
                                    record.getScore()
                            );


                            /*
                             * 来源类型
                             */
                            d.setSourceType(
                                    record.getSourceType()
                            );


                            /*
                             * 创建时间
                             */
                            d.setCreateTime(
                                    record.getCreateTime()
                            );


                            /*
                             * 隐藏状态
                             */
                            d.setAdminHidden(
                                    record.getAdminHidden()
                            );


                            return d;

                        })
                        .toList();


        vo.setDetail(
                detail
        );


        return Result.success(
                vo
        );
    }
}