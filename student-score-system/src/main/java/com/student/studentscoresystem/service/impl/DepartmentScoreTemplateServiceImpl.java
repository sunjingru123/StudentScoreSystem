package com.student.studentscoresystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.student.studentscoresystem.entity.DepartmentScoreTemplate;
import com.student.studentscoresystem.mapper.DepartmentScoreTemplateMapper;
import com.student.studentscoresystem.service.IDepartmentScoreTemplateService;
import org.springframework.stereotype.Service;

@Service
public class DepartmentScoreTemplateServiceImpl
        extends ServiceImpl<DepartmentScoreTemplateMapper, DepartmentScoreTemplate>
        implements IDepartmentScoreTemplateService {

}