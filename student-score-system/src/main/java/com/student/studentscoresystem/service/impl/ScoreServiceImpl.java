package com.student.studentscoresystem.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.student.studentscoresystem.entity.Score;
import com.student.studentscoresystem.mapper.ScoreMapper;
import com.student.studentscoresystem.service.IScoreService;
import org.springframework.stereotype.Service;


@Service
public class ScoreServiceImpl
        extends ServiceImpl<ScoreMapper, Score>
        implements IScoreService {


}