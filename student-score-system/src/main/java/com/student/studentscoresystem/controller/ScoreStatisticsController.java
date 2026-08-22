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
     * 管理员隐藏的记录：
     *
     * 1. 不显示
     * 2. 不参与加分
     * 3. 不参与减分
     *
     * 地址：
     *
     * GET /scoreStatistics/{studentId}
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
         * 学生和辅导员只能看到没有被管理员隐藏的记录
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
     * 管理员可以看到全部成绩记录。
     *
     * 包括：
     *
     * 1. 正常记录
     * 2. 已经被管理员隐藏的记录
     *
     * 地址：
     *
     * GET /scoreStatistics/admin/{studentId}
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
         * 管理员不进行 adminHidden 过滤
         *
         * 所以管理员可以看到全部记录
         */
        List<ScoreRecord> records =
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

        return buildStatistics(
                user,
                records
        );
    }

    /**
     * =========================================================
     * 统一计算 40 分上限模型
     * =========================================================
     *
     * 计算规则：
     *
     * 初始最高上限 = 40
     *
     * 减分：
     *
     * 当前最高上限 = 40 - 减分总和
     *
     * 加分：
     *
     * 从 0 开始累计
     *
     * 最终成绩：
     *
     * min(加分总和, 当前最高上限)
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
         *
         * 例如：
         *
         * +5
         * +10
         * +8
         *
         * bonusScore = 23
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
         * 3. 计算减分
         * =====================================================
         *
         * 数据库中：
         *
         * -5
         * -3
         *
         * 统计时：
         *
         * deductScore = 8
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
         * 4. 计算实际最高上限
         * =====================================================
         *
         * 40 - 减分
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
         * 5. 计算最终成绩
         * =====================================================
         *
         * 加分不能超过当前最高上限
         *
         * 例如：
         *
         * 加分 = 35
         * 减分 = 5
         *
         * 上限 = 35
         *
         * 最终 = 35
         *
         *
         * 加分 = 50
         * 减分 = 5
         *
         * 上限 = 35
         *
         * 最终 = 35
         */
        BigDecimal totalScore =
                bonusScore.min(
                        actualLimit
                );

        /*
         * =====================================================
         * 设置统计结果
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
         * 兼容旧前端字段
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
         * 成绩明细
         * =====================================================
         */
        List<ScoreDetailVO> detail =
                records.stream()
                        .map(
                                record -> {

                                    ScoreDetailVO d =
                                            new ScoreDetailVO();

                                    /*
                                     * 查询加分规则名称
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
                                     * 管理员隐藏状态
                                     */
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