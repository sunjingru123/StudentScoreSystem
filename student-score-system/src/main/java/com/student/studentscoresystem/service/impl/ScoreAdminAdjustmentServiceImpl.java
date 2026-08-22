package com.student.studentscoresystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.student.studentscoresystem.entity.ScoreAdminAdjustment;
import com.student.studentscoresystem.mapper.ScoreAdminAdjustmentMapper;
import com.student.studentscoresystem.service.IScoreAdminAdjustmentService;
import org.springframework.stereotype.Service;

@Service
public class ScoreAdminAdjustmentServiceImpl
        extends ServiceImpl<
        ScoreAdminAdjustmentMapper,
        ScoreAdminAdjustment
        >
        implements IScoreAdminAdjustmentService {

}