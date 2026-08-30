package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.ScoreAddDTO;
import com.student.studentscoresystem.dto.ScoreUpdateDTO;
import com.student.studentscoresystem.entity.Course;
import com.student.studentscoresystem.entity.Score;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.CourseMapper;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.IScoreService;
import com.student.studentscoresystem.utils.JwtUtil;
import com.student.studentscoresystem.vo.ScoreDetailVO;
import com.student.studentscoresystem.vo.ScoreRankVO;
import com.student.studentscoresystem.vo.ScoreStatisticsVO;
import com.student.studentscoresystem.vo.ScoreVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/score")
public class ScoreController {

    private final IScoreService scoreService;
    private final SysUserMapper sysUserMapper;
    private final CourseMapper courseMapper;
    private final ScoreRecordMapper scoreRecordMapper;

    public ScoreController(
            IScoreService scoreService,
            SysUserMapper sysUserMapper,
            CourseMapper courseMapper,
            ScoreRecordMapper scoreRecordMapper
    ) {
        this.scoreService = scoreService;
        this.sysUserMapper = sysUserMapper;
        this.courseMapper = courseMapper;
        this.scoreRecordMapper = scoreRecordMapper;
    }


    /**
     * =========================================================
     * 查询课程成绩
     * =========================================================
     */
    @GetMapping("/list")
    public Result<List<ScoreVO>> list() {

        List<Score> scores =
                scoreService.list();

        return Result.success(
                convertVO(scores)
        );
    }


    /**
     * =========================================================
     * 添加课程成绩
     * =========================================================
     */
    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody ScoreAddDTO dto
    ) {

        Score score =
                new Score();

        score.setStudentId(
                dto.getStudentId()
        );

        score.setCourseId(
                dto.getCourseId()
        );

        score.setScore(
                dto.getScore()
        );

        score.setSemester(
                dto.getSemester()
        );

        scoreService.save(
                score
        );

        return Result.success(null);
    }


    /**
     * =========================================================
     * 修改课程成绩
     * =========================================================
     */
    @PutMapping("/update")
    public Result<Void> update(
            @RequestBody ScoreUpdateDTO dto
    ) {

        Score score =
                scoreService.getById(
                        dto.getId()
                );

        if (score == null) {

            return Result.fail(
                    "成绩不存在"
            );
        }

        score.setScore(
                dto.getScore()
        );

        score.setSemester(
                dto.getSemester()
        );

        scoreService.updateById(
                score
        );

        return Result.success(null);
    }


    /**
     * =========================================================
     * 删除课程成绩
     * =========================================================
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable Long id
    ) {

        boolean result =
                scoreService.removeById(
                        id
                );

        if (!result) {

            return Result.fail(
                    "成绩不存在"
            );
        }

        return Result.success(null);
    }


    /**
     * =========================================================
     * 学生本人查看自己的加减分流水
     *
     * GET /score/my
     *
     * 只能看到：
     *
     * status = 1
     * adminHidden = 0
     *
     * 隐藏记录不会返回。
     * =========================================================
     */
    @GetMapping("/my")
    public Result<List<ScoreRecord>> my(
            HttpServletRequest request
    ) {

        Long userId =
                getCurrentUserId(request);

        if (userId == null) {

            return Result.fail(
                    "用户信息无效"
            );
        }

        List<ScoreRecord> records =
                scoreRecordMapper.selectList(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .eq(
                                        ScoreRecord::getStudentId,
                                        userId
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

        return Result.success(
                records
        );
    }


    /**
     * =========================================================
     * 查看指定学生的公开流水
     *
     * 给：
     *
     * 1. 学生综合测评页面
     * 2. 老师查看学生成绩
     * 3. 部门负责人查看学生成绩
     *
     * 使用。
     *
     * GET /score/student/{studentId}/records
     *
     * 重要：
     *
     * 这里必须过滤 adminHidden = 0。
     *
     * 管理员隐藏的记录：
     *
     * 不返回。
     *
     * 普通查看者根本拿不到隐藏记录。
     * =========================================================
     */
    @GetMapping("/student/{studentId}/records")
    public Result<List<ScoreDetailVO>> studentRecords(
            @PathVariable Long studentId
    ) {

        /*
         * =====================================================
         * 1. 检查学生是否存在
         * =====================================================
         */

        SysUser student =
                sysUserMapper.selectById(
                        studentId
                );

        if (student == null) {

            return Result.error(
                    "学生不存在"
            );
        }


        /*
         * =====================================================
         * 2. 查询公开流水
         *
         * 这里只查询：
         *
         * status = 1
         *
         * adminHidden = 0
         *
         * 所以隐藏记录不会进入结果。
         * =====================================================
         */

        List<ScoreRecord> records =
                scoreRecordMapper.selectList(
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


        /*
         * =====================================================
         * 3. 转换成前端需要的 VO
         * =====================================================
         */

        List<ScoreDetailVO> result =
                new ArrayList<>();


        for (
                ScoreRecord record
                : records
        ) {

            if (record == null) {
                continue;
            }


            ScoreDetailVO vo =
                    new ScoreDetailVO();


            /*
             * 流水 ID
             */

            vo.setId(
                    record.getId()
            );





            /*
             * 分数
             */

            vo.setScore(
                    record.getScore()
            );


            /*
             * 来源类型
             */

            vo.setSourceType(
                    record.getSourceType()
            );


            /*
             * 来源 ID
             */

            vo.setSourceId(
                    record.getSourceId()
            );


            /*
             * 创建时间
             */

            vo.setCreateTime(
                    record.getCreateTime()
            );


            /*
             * 由于这里已经过滤：
             *
             * adminHidden = 0
             *
             * 所以返回给普通查看页面的值固定为 0。
             */

            vo.setAdminHidden(
                    (short) 0
            );


            /*
             * ScoreRecord 目前没有直接保存 ruleName，
             * 所以暂时使用 sourceType。
             */

            vo.setRuleName(
                    "计分项: "
                            + record.getSourceType()
            );


            result.add(
                    vo
            );
        }


        return Result.success(
                result
        );
    }


    /**
     * =========================================================
     * 学生本人统计
     *
     * 只统计公开记录。
     * =========================================================
     */
    @GetMapping("/statistics")
    public Result<ScoreStatisticsVO> statistics(
            HttpServletRequest request
    ) {

        Long userId =
                getCurrentUserId(request);

        if (userId == null) {

            return Result.fail(
                    "用户信息无效"
            );
        }


        SysUser user =
                sysUserMapper.selectById(
                        userId
                );


        List<ScoreRecord> records =
                scoreRecordMapper.selectList(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .eq(
                                        ScoreRecord::getStudentId,
                                        userId
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


        ScoreStatisticsVO vo =
                new ScoreStatisticsVO();


        vo.setStudentName(
                user != null
                        ? user.getRealName()
                        : "未知"
        );


        vo.setBaseLimit(
                new BigDecimal("40")
        );


        if (records.isEmpty()) {

            vo.setAvgScore(0D);

            vo.setMaxScore(0);

            vo.setMinScore(0);

            vo.setBonusScore(
                    BigDecimal.ZERO
            );

            vo.setDeductScore(
                    BigDecimal.ZERO
            );

            vo.setActualLimit(
                    new BigDecimal("40")
            );

            vo.setTotalScore(
                    BigDecimal.ZERO
            );

            vo.setDetail(
                    new ArrayList<>()
            );

            return Result.success(
                    vo
            );
        }


        List<BigDecimal> scoreList =
                records.stream()
                        .map(
                                ScoreRecord::getScore
                        )
                        .filter(
                                s -> s != null
                        )
                        .toList();


        BigDecimal bonusScore =
                scoreList.stream()
                        .filter(
                                s -> s.compareTo(
                                        BigDecimal.ZERO
                                ) > 0
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );


        BigDecimal deductScore =
                scoreList.stream()
                        .filter(
                                s -> s.compareTo(
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


        BigDecimal actualLimit =
                vo.getBaseLimit()
                        .subtract(
                                deductScore
                        );


        vo.setActualLimit(
                actualLimit
        );


        vo.setTotalScore(
                bonusScore.min(
                        actualLimit
                )
        );


        vo.setBonusScore(
                bonusScore
        );


        vo.setDeductScore(
                deductScore
        );


        double avg =
                scoreList.stream()
                        .mapToDouble(
                                BigDecimal::doubleValue
                        )
                        .average()
                        .orElse(0.0);


        vo.setAvgScore(
                BigDecimal.valueOf(avg)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        )
                        .doubleValue()
        );


        vo.setMaxScore(
                scoreList.stream()
                        .max(
                                Comparator.naturalOrder()
                        )
                        .orElse(
                                BigDecimal.ZERO
                        )
                        .intValue()
        );


        vo.setMinScore(
                scoreList.stream()
                        .min(
                                Comparator.naturalOrder()
                        )
                        .orElse(
                                BigDecimal.ZERO
                        )
                        .intValue()
        );


        /*
         * =====================================================
         * 构造详情
         * =====================================================
         */

        List<ScoreDetailVO> details =
                records.stream()
                        .map(r -> {

                            ScoreDetailVO d =
                                    new ScoreDetailVO();

                            d.setScore(
                                    r.getScore()
                            );

                            d.setSourceType(
                                    r.getSourceType()
                            );

                            d.setSourceId(
                                    r.getSourceId()
                            );

                            d.setCreateTime(
                                    r.getCreateTime()
                            );

                            d.setAdminHidden(
                                    r.getAdminHidden()
                            );

                            d.setRuleName(
                                    "计分项: "
                                            + r.getSourceType()
                            );

                            return d;
                        })
                        .toList();


        vo.setDetail(
                details
        );


        return Result.success(
                vo
        );
    }


    /**
     * =========================================================
     * 综合测评排名
     *
     * 只统计公开记录。
     * =========================================================
     */
    @GetMapping("/rank")
    public Result<List<ScoreRankVO>> rank() {

        List<ScoreRecord> records =
                scoreRecordMapper.selectList(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .eq(
                                        ScoreRecord::getStatus,
                                        (short) 1
                                )
                                .eq(
                                        ScoreRecord::getAdminHidden,
                                        (short) 0
                                )
                );


        Map<Long, List<ScoreRecord>> map =
                new HashMap<>();


        for (
                ScoreRecord record
                : records
        ) {

            if (
                    record.getStudentId()
                            == null
            ) {
                continue;
            }


            map.computeIfAbsent(
                    record.getStudentId(),
                    k -> new ArrayList<>()
            ).add(
                    record
            );
        }


        List<ScoreRankVO> list =
                new ArrayList<>();


        for (
                Map.Entry<Long, List<ScoreRecord>> entry
                : map.entrySet()
        ) {

            Long studentId =
                    entry.getKey();


            List<ScoreRecord> studentRecords =
                    entry.getValue();


            BigDecimal bonus =
                    studentRecords.stream()
                            .map(
                                    ScoreRecord::getScore
                            )
                            .filter(
                                    s -> s != null
                                            && s.compareTo(
                                            BigDecimal.ZERO
                                    ) > 0
                            )
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );


            BigDecimal deduct =
                    studentRecords.stream()
                            .map(
                                    ScoreRecord::getScore
                            )
                            .filter(
                                    s -> s != null
                                            && s.compareTo(
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


            BigDecimal finalScore =
                    bonus.min(
                            new BigDecimal("40")
                                    .subtract(
                                            deduct
                                    )
                    );


            SysUser user =
                    sysUserMapper.selectById(
                            studentId
                    );


            if (user == null) {
                continue;
            }


            ScoreRankVO vo =
                    new ScoreRankVO();


            vo.setStudentName(
                    user.getRealName()
            );


            vo.setAvgScore(
                    finalScore.doubleValue()
            );


            list.add(
                    vo
            );
        }


        list.sort(
                (a, b) ->
                        Double.compare(
                                b.getAvgScore(),
                                a.getAvgScore()
                        )
        );


        for (
                int i = 0;
                i < list.size();
                i++
        ) {

            list.get(i)
                    .setRank(
                            i + 1
                    );
        }


        return Result.success(
                list
        );
    }


    /**
     * =========================================================
     * 获取当前登录用户 ID
     * =========================================================
     */
    private Long getCurrentUserId(
            HttpServletRequest request
    ) {

        String auth =
                request.getHeader(
                        "Authorization"
                );


        if (
                auth == null
                        || !auth.startsWith(
                        "Bearer "
                )
        ) {

            return null;
        }


        String token =
                auth.substring(7);


        try {

            Claims claims =
                    JwtUtil.parseToken(
                            token
                    );


            return claims.get(
                    "userId",
                    Long.class
            );

        } catch (Exception e) {

            return null;
        }
    }


    /**
     * =========================================================
     * Score -> ScoreVO
     * =========================================================
     */
    private List<ScoreVO> convertVO(
            List<Score> scores
    ) {

        return scores.stream()
                .map(score -> {

                    ScoreVO vo =
                            new ScoreVO();


                    vo.setId(
                            score.getId()
                    );


                    SysUser user =
                            sysUserMapper.selectById(
                                    score.getStudentId()
                            );


                    Course course =
                            courseMapper.selectById(
                                    score.getCourseId()
                            );


                    if (user != null) {

                        vo.setStudentName(
                                user.getRealName()
                        );
                    }


                    if (course != null) {

                        vo.setCourseName(
                                course.getCourseName()
                        );
                    }


                    vo.setScore(
                            score.getScore()
                    );


                    vo.setSemester(
                            score.getSemester()
                    );


                    vo.setCreateTime(
                            score.getCreateTime()
                    );


                    return vo;

                })
                .toList();
    }
}