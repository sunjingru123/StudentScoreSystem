package com.student.studentscoresystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.studentscoresystem.entity.Department;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DepartmentMapper
        extends BaseMapper<Department> {
}