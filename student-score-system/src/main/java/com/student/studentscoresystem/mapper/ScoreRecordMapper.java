package com.student.studentscoresystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.studentscoresystem.entity.ScoreRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScoreRecordMapper
        extends BaseMapper<ScoreRecord> {
}