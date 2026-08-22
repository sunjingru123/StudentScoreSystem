package com.student.studentscoresystem.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.ScoreFlow;
import com.student.studentscoresystem.service.IScoreFlowService;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/scoreFlow")
public class ScoreFlowController {


    private final IScoreFlowService scoreFlowService;



    public ScoreFlowController(
            IScoreFlowService scoreFlowService
    ){

        this.scoreFlowService =
                scoreFlowService;

    }



    /**
     * 查询学生积分流水
     */
    @GetMapping("/student/{id}")
    public Result<List<ScoreFlow>> list(
            @PathVariable Long id
    ){


        List<ScoreFlow> list =

                scoreFlowService.list(

                        new LambdaQueryWrapper<ScoreFlow>()

                                .eq(
                                        ScoreFlow::getStudentId,
                                        id
                                )

                                .orderByDesc(
                                        ScoreFlow::getCreateTime
                                )

                );


        return Result.success(list);

    }


}