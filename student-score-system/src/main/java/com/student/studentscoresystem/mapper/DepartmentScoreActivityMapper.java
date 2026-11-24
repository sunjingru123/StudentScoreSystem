package com.student.studentscoresystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.studentscoresystem.entity.DepartmentScoreActivity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 活动批量加分申报：活动记录。
 */
@Mapper
public interface DepartmentScoreActivityMapper
        extends BaseMapper<DepartmentScoreActivity> {
}
