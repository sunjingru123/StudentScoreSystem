package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================
 * 成绩统计 Controller
 * =========================================================
 *
 * 功能：
 *
 * 1. 学生查看自己的成绩统计
 * 2. 辅导员查看学生成绩统计
 * 3. 管理员查看学生成绩统计
 * 4. 管理员分页查看成绩明细
 * 5. 学生分页查看成绩明细
 *
 * =========================================================
 */
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
     * 学生 / 辅导员查看成绩统计
     * =========================================================
     *
     * GET
     *
     * /scoreStatistics/{studentId}
     *
     * 查询规则：
     *
     * status = 1
     * adminHidden = 0
     *
     * 这里只返回统计信息。
     *
     * 不再把全部成绩明细塞进这里。
     * =========================================================
     */
    @GetMapping("/{studentId}")
    public Result<ScoreStatisticsVO> detail(
            @PathVariable Long studentId
    ) {

        /*
         * 检查学生
         */
        SysUser user =
                sysUserMapper.selectById(studentId);

        if (user == null) {

            return Result.fail(
                    "学生不存在"
            );
        }


        /*
         * =====================================================
         * 查询学生可见成绩
         *
         * status = 1
         * adminHidden = 0
         * =====================================================
         */
        LambdaQueryWrapper<ScoreRecord> wrapper =
                new LambdaQueryWrapper<>();

        wrapper
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
                );


        List<ScoreRecord> records =
                scoreRecordService.list(
                        wrapper
                );


        /*
         * 构造统计
         */
        return buildStatistics(
                user,
                records
        );
    }


    /**
     * =========================================================
     * 管理员查看学生成绩统计
     * =========================================================
     *
     * GET
     *
     * /scoreStatistics/admin/{studentId}
     *
     * 管理员可以看到：
     *
     * adminHidden = 0
     * adminHidden = 1
     *
     * 但是：
     *
     * status = 0
     *
     * 的作废记录不参与统计。
     *
     * =========================================================
     */
    @GetMapping("/admin/{studentId}")
    public Result<ScoreStatisticsVO> adminDetail(
            @PathVariable Long studentId
    ) {

        /*
         * 检查学生
         */
        SysUser user =
                sysUserMapper.selectById(
                        studentId
                );

        if (user == null) {

            return Result.fail(
                    "学生不存在"
            );
        }


        /*
         * =====================================================
         * 管理员统计
         *
         * 只过滤：
         *
         * status = 1
         *
         * 不过滤 adminHidden。
         * =====================================================
         */
        LambdaQueryWrapper<ScoreRecord> wrapper =
                new LambdaQueryWrapper<>();

        wrapper
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
                );


        List<ScoreRecord> records =
                scoreRecordService.list(
                        wrapper
                );


        /*
         * 返回统计
         */
        return buildStatistics(
                user,
                records
        );
    }


    /**
     * =========================================================
     * 管理员分页查询成绩明细
     * =========================================================
     *
     * GET
     *
     * /scoreStatistics/admin/{studentId}/records
     *
     * 参数：
     *
     * pageNum=1
     * pageSize=10
     *
     * 示例：
     *
     * /scoreStatistics/admin/1/records
     *
     * /scoreStatistics/admin/1/records?pageNum=2&pageSize=10
     *
     * =========================================================
     */
    @GetMapping("/admin/{studentId}/records")
    public Result<Page<ScoreDetailVO>> adminRecords(
            @PathVariable Long studentId,

            @RequestParam(
                    defaultValue = "1"
            )
            long pageNum,

            @RequestParam(
                    defaultValue = "10"
            )
            long pageSize
    ) {

        /*
         * =====================================================
         * 1. 保护分页参数
         * =====================================================
         */

        if (pageNum < 1) {

            pageNum = 1;
        }


        if (pageSize < 1) {

            pageSize = 10;
        }


        /*
         * 最大一次查询 100 条
         */
        if (pageSize > 100) {

            pageSize = 100;
        }


        /*
         * =====================================================
         * 2. 检查学生
         * =====================================================
         */

        SysUser user =
                sysUserMapper.selectById(
                        studentId
                );

        if (user == null) {

            return Result.fail(
                    "学生不存在"
            );
        }


        /*
         * =====================================================
         * 3. 创建真正分页对象
         * =====================================================
         */

        Page<ScoreRecord> page =
                new Page<>(
                        pageNum,
                        pageSize
                );


        /*
         * =====================================================
         * 4. 查询条件
         *
         * 管理员：
         *
         * status = 1
         *
         * 不限制 adminHidden
         *
         * 所以：
         *
         * adminHidden = 0
         * adminHidden = 1
         *
         * 都能看到。
         * =====================================================
         */

        LambdaQueryWrapper<ScoreRecord> wrapper =
                new LambdaQueryWrapper<>();

        wrapper
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
                );


        /*
         * =====================================================
         * 5. 真正执行分页
         *
         * MyBatis-Plus 会生成：
         *
         * LIMIT ?, ?
         *
         * =====================================================
         */

        Page<ScoreRecord> recordPage =
                scoreRecordService.page(
                        page,
                        wrapper
                );


        /*
         * =====================================================
         * 6. 创建 VO 分页对象
         * =====================================================
         */

        Page<ScoreDetailVO> voPage =
                new Page<>(
                        recordPage.getCurrent(),
                        recordPage.getSize(),
                        recordPage.getTotal()
                );


        List<ScoreDetailVO> voList =
                new ArrayList<>();


        /*
         * =====================================================
         * 7. Entity -> VO
         * =====================================================
         */

        for (
                ScoreRecord record
                : recordPage.getRecords()
        ) {

            if (record == null) {

                continue;
            }


            ScoreDetailVO vo =
                    new ScoreDetailVO();


            /*
             * =================================================
             * 成绩记录 ID
             *
             * 非常重要。
             *
             * 前端：
             *
             * hideScore(row)
             *
             * 会使用：
             *
             * row.id
             *
             * =================================================
             */

            vo.setId(
                    record.getId()
            );


            /*
             * =================================================
             * 成绩规则
             * =================================================
             */

            if (
                    record.getRuleId() != null
            ) {

                ScoreRule rule =
                        scoreRuleMapper.selectById(
                                record.getRuleId()
                        );

                if (rule != null) {

                    vo.setRuleName(
                            rule.getName()
                    );
                }
            }


            /*
             * =================================================
             * 分数
             * =================================================
             */

            vo.setScore(
                    record.getScore()
            );


            /*
             * =================================================
             * 来源
             * =================================================
             */

            vo.setSourceType(
                    record.getSourceType()
            );


            /*
             * =================================================
             * 创建时间
             * =================================================
             */

            vo.setCreateTime(
                    record.getCreateTime()
            );


            /*
             * =================================================
             * 管理员隐藏状态
             *
             * 注意：
             *
             * 如果实体是 Short，
             * VO 也使用 Short。
             *
             * 不要强转 Integer。
             * =================================================
             */

            vo.setAdminHidden(
                    record.getAdminHidden()
            );


            /*
             * 加入列表
             */

            voList.add(
                    vo
            );
        }


        /*
         * =====================================================
         * 8. 设置分页 records
         * =====================================================
         */

        voPage.setRecords(
                voList
        );


        /*
         * =====================================================
         * 9. 返回
         * =====================================================
         */

        return Result.success(
                voPage
        );
    }


    /**
     * =========================================================
     * 学生分页查询成绩明细
     * =========================================================
     *
     * GET
     *
     * /scoreStatistics/{studentId}/records
     *
     * 学生只能看到：
     *
     * status = 1
     * adminHidden = 0
     *
     * =========================================================
     */
    @GetMapping("/{studentId}/records")
    public Result<Page<ScoreDetailVO>> records(
            @PathVariable Long studentId,

            @RequestParam(
                    defaultValue = "1"
            )
            long pageNum,

            @RequestParam(
                    defaultValue = "10"
            )
            long pageSize
    ) {

        /*
         * =====================================================
         * 1. 保护分页参数
         * =====================================================
         */

        if (pageNum < 1) {

            pageNum = 1;
        }


        if (pageSize < 1) {

            pageSize = 10;
        }


        if (pageSize > 100) {

            pageSize = 100;
        }


        /*
         * =====================================================
         * 2. 检查学生
         * =====================================================
         */

        SysUser user =
                sysUserMapper.selectById(
                        studentId
                );

        if (user == null) {

            return Result.fail(
                    "学生不存在"
            );
        }


        /*
         * =====================================================
         * 3. 创建分页
         * =====================================================
         */

        Page<ScoreRecord> page =
                new Page<>(
                        pageNum,
                        pageSize
                );


        /*
         * =====================================================
         * 4. 查询学生可见成绩
         *
         * status = 1
         * adminHidden = 0
         * =====================================================
         */

        LambdaQueryWrapper<ScoreRecord> wrapper =
                new LambdaQueryWrapper<>();

        wrapper
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
                );


        /*
         * =====================================================
         * 5. 真正分页查询
         * =====================================================
         */

        Page<ScoreRecord> recordPage =
                scoreRecordService.page(
                        page,
                        wrapper
                );


        /*
         * =====================================================
         * 6. 创建 VO 分页
         * =====================================================
         */

        Page<ScoreDetailVO> voPage =
                new Page<>(
                        recordPage.getCurrent(),
                        recordPage.getSize(),
                        recordPage.getTotal()
                );


        List<ScoreDetailVO> voList =
                new ArrayList<>();


        /*
         * =====================================================
         * 7. Entity -> VO
         * =====================================================
         */

        for (
                ScoreRecord record
                : recordPage.getRecords()
        ) {

            if (record == null) {

                continue;
            }


            ScoreDetailVO vo =
                    new ScoreDetailVO();


            /*
             * 成绩 ID
             */
            vo.setId(
                    record.getId()
            );


            /*
             * 成绩规则
             */

            if (
                    record.getRuleId() != null
            ) {

                ScoreRule rule =
                        scoreRuleMapper.selectById(
                                record.getRuleId()
                        );

                if (rule != null) {

                    vo.setRuleName(
                            rule.getName()
                    );
                }
            }


            /*
             * 分数
             */

            vo.setScore(
                    record.getScore()
            );


            /*
             * 来源
             */

            vo.setSourceType(
                    record.getSourceType()
            );


            /*
             * 时间
             */

            vo.setCreateTime(
                    record.getCreateTime()
            );


            /*
             * 隐藏状态
             *
             * 学生接口理论上永远是 0，
             * 这里仍然返回字段，
             * 方便前端统一处理。
             */

            vo.setAdminHidden(
                    record.getAdminHidden()
            );


            voList.add(
                    vo
            );
        }


        /*
         * 设置 records
         */

        voPage.setRecords(
                voList
        );


        return Result.success(
                voPage
        );
    }


    /**
     * =========================================================
     * 统一计算成绩统计
     * =========================================================
     *
     * 注意：
     *
     * 这里不负责分页。
     *
     * 也不返回全部成绩明细。
     *
     * 成绩明细由：
     *
     * /admin/{studentId}/records
     *
     * 单独分页查询。
     *
     * =========================================================
     */
    private Result<ScoreStatisticsVO> buildStatistics(
            SysUser user,
            List<ScoreRecord> records
    ) {

        ScoreStatisticsVO vo =
                new ScoreStatisticsVO();


        /*
         * =====================================================
         * 学生姓名
         * =====================================================
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
         * 3. 减分
         * =====================================================
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
         * 4. 实际最高上限
         * =====================================================
         */

        BigDecimal actualLimit =
                baseLimit.subtract(
                        deductScore
                );


        /*
         * 上限不能小于 0
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
         * 6. 设置统计
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
         * 8. 不再返回全部明细
         *
         * 防止：
         *
         * /scoreStatistics/admin/{studentId}
         *
         * 一次查询几百条甚至几千条成绩。
         *
         * 真正明细通过分页接口查询。
         * =====================================================
         */

        vo.setDetail(
                new ArrayList<>()
        );


        return Result.success(
                vo
        );
    }
}