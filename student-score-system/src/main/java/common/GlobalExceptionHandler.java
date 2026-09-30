package com.student.studentscoresystem.common;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(Exception.class)
    public Result<String> handleException(
            Exception e,
            HttpServletRequest request
    ){

        // 对外提示保持不变，只把真实异常（如数据库连接/认证失败）记入服务端日志，便于排查
        log.error("请求处理异常: {} {}", request.getMethod(), request.getRequestURI(), e);

        // 登录接口统一错误响应，避免暴露用户是否存在、数据库或 Java 异常细节。
        if ("/login".equals(request.getRequestURI())) {
            return Result.fail("用户名或密码错误");
        }


        return Result.error(
                "请求处理失败"
        );

    }

}
