package com.student.studentscoresystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.student.studentscoresystem.entity.ScoreFlow;
import com.student.studentscoresystem.mapper.ScoreFlowMapper;
import com.student.studentscoresystem.service.IScoreFlowService;
import org.springframework.stereotype.Service;

@Service
public class ScoreFlowServiceImpl
        extends ServiceImpl<ScoreFlowMapper, ScoreFlow>
        implements IScoreFlowService {

}