package com.student.studentscoresystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.studentscoresystem.entity.ScoreRecordOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScoreRecordOperationLogMapper
        extends BaseMapper<ScoreRecordOperationLog> {
}