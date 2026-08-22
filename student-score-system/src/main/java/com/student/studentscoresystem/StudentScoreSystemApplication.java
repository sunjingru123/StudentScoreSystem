package com.student.studentscoresystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@MapperScan("com.student.studentscoresystem.mapper")
public class StudentScoreSystemApplication {

    public static void main(String[] args) {

        SpringApplication.run(
                StudentScoreSystemApplication.class,args
        );

    }
}