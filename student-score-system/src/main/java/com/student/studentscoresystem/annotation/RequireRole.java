package com.student.studentscoresystem.annotation;


import java.lang.annotation.*;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {


    /**
     * 允许访问的岗位名称，命中任意一个即可通过。
     */
    String[] value();


}