package com.student.studentscoresystem.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.studentscoresystem.entity.Score;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface ScoreMapper
        extends BaseMapper<Score> {

}
