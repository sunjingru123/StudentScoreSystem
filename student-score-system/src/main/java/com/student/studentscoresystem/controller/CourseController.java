package com.student.studentscoresystem.controller;


import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.Course;
import com.student.studentscoresystem.service.ICourseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/course")
public class CourseController {


    private final ICourseService courseService;


    public CourseController(
            ICourseService courseService
    ){

        this.courseService = courseService;

    }



    /**
     * 查询所有课程
     */
    @GetMapping("/list")
    public Result<List<Course>> list(){


        List<Course> courses =
                courseService.list();


        return Result.success(courses);

    }

}
