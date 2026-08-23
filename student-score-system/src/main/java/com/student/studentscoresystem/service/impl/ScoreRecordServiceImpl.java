package com.student.studentscoresystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.service.IScoreRecordService;
import org.springframework.stereotype.Service;

@Service
public class ScoreRecordServiceImpl
        extends ServiceImpl<ScoreRecordMapper, ScoreRecord>
        implements IScoreRecordService {
}