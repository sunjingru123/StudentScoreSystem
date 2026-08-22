package com.student.studentscoresystem.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.service.IScoreRecordService;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/scoreRecord")
public class ScoreRecordController {


    private final IScoreRecordService scoreRecordService;


    public ScoreRecordController(
            IScoreRecordService scoreRecordService
    ){

        this.scoreRecordService =
                scoreRecordService;

    }



    /**
     * 查询全部综合测评记录
     */
    @GetMapping("/list")
    public Result<List<ScoreRecord>> list(){


        return Result.success(
                scoreRecordService.list()
        );

    }



    /**
     * 查询学生综合测评记录
     */
    @GetMapping("/student/{studentId}")
    public Result<List<ScoreRecord>> student(
            @PathVariable Long studentId
    ) {

            List<ScoreRecord> list =
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

            return Result.success(list);
        }
    @PutMapping("/admin/hide/{id}")
    public Result<Void> hide(
            @PathVariable Long id
    ) {

        ScoreRecord record =
                scoreRecordService.getById(id);

        if (record == null) {
            return Result.error("成绩记录不存在");
        }

        record.setAdminHidden((short) 1);

        scoreRecordService.updateById(record);

        return Result.success(null);
    }
    @PutMapping("/admin/show/{id}")
    public Result<Void> show(
            @PathVariable Long id
    ) {

        ScoreRecord record =
                scoreRecordService.getById(id);

        if (record == null) {
            return Result.error("成绩记录不存在");
        }

        record.setAdminHidden((short) 0);

        scoreRecordService.updateById(record);

        return Result.success(null);
    }
}