package com.student.studentscoresystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.mapper.DepartmentScoreApplyMapper;
import com.student.studentscoresystem.service.IDepartmentScoreApplyService;
import org.springframework.stereotype.Service;

@Service
public class DepartmentScoreApplyServiceImpl
        extends ServiceImpl<DepartmentScoreApplyMapper, DepartmentScoreApply>
        implements IDepartmentScoreApplyService {
}