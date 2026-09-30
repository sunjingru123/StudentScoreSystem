package com.student.studentscoresystem.common;


import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;


@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(Exception.class)
    public Result<String> handleException(
            Exception e,
            HttpServletRequest request
    ){

        // 登录接口统一错误响应，避免暴露用户是否存在、数据库或 Java 异常细节。
        if ("/login".equals(request.getRequestURI())) {
            return Result.fail("用户名或密码错误");
        }


        return Result.error(
                "请求处理失败"
        );

    }

}
