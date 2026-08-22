package com.student.studentscoresystem.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.student.studentscoresystem.entity.Course;
import com.student.studentscoresystem.mapper.CourseMapper;
import com.student.studentscoresystem.service.ICourseService;
import org.springframework.stereotype.Service;


@Service
public class CourseServiceImpl
        extends ServiceImpl<CourseMapper, Course>
        implements ICourseService {


}