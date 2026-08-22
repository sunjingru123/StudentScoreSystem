package com.student.studentscoresystem.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.ScoreAddDTO;
import com.student.studentscoresystem.dto.ScoreUpdateDTO;
import com.student.studentscoresystem.entity.*;
import com.student.studentscoresystem.mapper.CourseMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.IScoreService;
import com.student.studentscoresystem.utils.JwtUtil;
import com.student.studentscoresystem.vo.ScoreVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/score")
public class ScoreController {


    private final IScoreService scoreService;

    private final SysUserMapper sysUserMapper;

    private final CourseMapper courseMapper;

    private final SysUserPositionMapper sysUserPositionMapper;

    private final SysPositionMapper sysPositionMapper;



    public ScoreController(
            IScoreService scoreService,
            SysUserMapper sysUserMapper,
            CourseMapper courseMapper,
            SysUserPositionMapper sysUserPositionMapper,
            SysPositionMapper sysPositionMapper
    ){

        this.scoreService = scoreService;
        this.sysUserMapper = sysUserMapper;
        this.courseMapper = courseMapper;
        this.sysUserPositionMapper = sysUserPositionMapper;
        this.sysPositionMapper = sysPositionMapper;

    }



    /**
     * 查询全部成绩
     */
    @GetMapping("/list")
    public Result<List<ScoreVO>> list(){


        List<Score> scores =
                scoreService.list();


        return Result.success(convertVO(scores));

    }



    /**
     * 新增成绩
     */
    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody ScoreAddDTO dto
    ){


        Score score = new Score();


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


        scoreService.save(score);


        return Result.success(null);

    }




    /**
     * 修改成绩
     */
    @PutMapping("/update")
    public Result<Void> update(
            @RequestBody ScoreUpdateDTO dto
    ){


        Score score =
                scoreService.getById(
                        dto.getId()
                );


        if(score == null){

            return Result.fail("成绩不存在");

        }


        score.setScore(
                dto.getScore()
        );


        score.setSemester(
                dto.getSemester()
        );


        scoreService.updateById(score);


        return Result.success(null);

    }





    /**
     * 删除成绩
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable Long id
    ){


        boolean result =
                scoreService.removeById(id);


        if(!result){

            return Result.fail("成绩不存在");

        }


        return Result.success(null);

    }







    /**
     * 查询我的成绩
     *
     * 学生：自己的成绩
     * 老师：自己课程成绩
     * 管理员：全部成绩
     */
    @GetMapping("/my")
    public Result<List<ScoreVO>> my(
            HttpServletRequest request
    ){


        String token =
                request.getHeader("Authorization")
                        .replace(
                                "Bearer ",
                                ""
                        );



        Claims claims =
                JwtUtil.parseToken(token);



        Long userId =
                claims.get(
                        "userId",
                        Long.class
                );



        List<Score> scores;



        // 查询用户岗位

        SysUserPosition userPosition =
                sysUserPositionMapper.selectOne(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .eq(
                                        SysUserPosition::getUserId,
                                        userId
                                )
                );



        if(userPosition == null){

            return Result.fail("用户没有岗位");

        }



        SysPosition position =
                sysPositionMapper.selectById(
                        userPosition.getPositionId()
                );



        if(position == null){

            return Result.fail("岗位不存在");

        }





        String role =
                position.getName();



        // 学生

        if(role.equals("学生")){


            scores =
                    scoreService.list(
                            new LambdaQueryWrapper<Score>()
                                    .eq(
                                            Score::getStudentId,
                                            userId
                                    )
                    );


        }


        // 教师

        else if(role.equals("教师")){


            List<Course> courses =
                    courseMapper.selectList(
                            new LambdaQueryWrapper<Course>()
                                    .eq(
                                            Course::getTeacherId,
                                            userId
                                    )
                    );



            List<Long> ids =
                    courses.stream()
                            .map(Course::getId)
                            .toList();



            scores =
                    scoreService.list(
                            new LambdaQueryWrapper<Score>()
                                    .in(
                                            Score::getCourseId,
                                            ids
                                    )
                    );


        }



        // 管理员

        else{


            scores =
                    scoreService.list();

        }



        return Result.success(
                convertVO(scores)
        );


    }

    /**
     * 成绩统计
     */
    @GetMapping("/statistics")
    public Result<com.student.studentscoresystem.vo.ScoreStatisticsVO> statistics(){


        List<Score> scores =
                scoreService.list();


        com.student.studentscoresystem.vo.ScoreStatisticsVO vo =
                new com.student.studentscoresystem.vo.ScoreStatisticsVO();



        if(scores.isEmpty()){

            vo.setAvgScore(0D);
            vo.setMaxScore(0);
            vo.setMinScore(0);

            return Result.success(vo);

        }



        double avg =
                scores.stream()
                        .mapToInt(
                                Score::getScore
                        )
                        .average()
                        .orElse(0);



        int max =
                scores.stream()
                        .mapToInt(
                                Score::getScore
                        )
                        .max()
                        .getAsInt();



        int min =
                scores.stream()
                        .mapToInt(
                                Score::getScore
                        )
                        .min()
                        .getAsInt();



        vo.setAvgScore(avg);

        vo.setMaxScore(max);

        vo.setMinScore(min);



        return Result.success(vo);

    }
    /**
     * 学生成绩排名
     */
    @GetMapping("/rank")
    public Result<List<com.student.studentscoresystem.vo.ScoreRankVO>> rank(){


        List<Score> scores =
                scoreService.list();


        Map<Long,List<Score>> map =
                scores.stream()
                        .collect(
                                Collectors.groupingBy(
                                        Score::getStudentId
                                )
                        );



        List<com.student.studentscoresystem.vo.ScoreRankVO> list =
                new ArrayList<>();



        for(Map.Entry<Long,List<Score>> entry : map.entrySet()){


            Long studentId =
                    entry.getKey();


            List<Score> studentScores =
                    entry.getValue();



            double avg =
                    studentScores.stream()
                            .mapToInt(
                                    Score::getScore
                            )
                            .average()
                            .orElse(0);



            SysUser user =
                    sysUserMapper.selectById(
                            studentId
                    );



            com.student.studentscoresystem.vo.ScoreRankVO vo =
                    new com.student.studentscoresystem.vo.ScoreRankVO();


            vo.setStudentName(
                    user.getRealName()
            );


            vo.setAvgScore(avg);


            list.add(vo);


        }



        //按照平均分降序

        list.sort(
                (a,b) ->
                        Double.compare(
                                b.getAvgScore(),
                                a.getAvgScore()
                        )
        );



        //生成排名

        for(int i=0;i<list.size();i++){

            list.get(i)
                    .setRank(i+1);

        }



        return Result.success(list);

    }


    /**
     * Score转ScoreVO
     */
    private List<ScoreVO> convertVO(
            List<Score> scores
    ){


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


                    vo.setStudentName(
                            user.getRealName()
                    );


                    vo.setCourseName(
                            course.getCourseName()
                    );


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