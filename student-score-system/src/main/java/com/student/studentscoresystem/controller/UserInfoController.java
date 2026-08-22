package com.student.studentscoresystem.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.service.ISysUserService;
import com.student.studentscoresystem.utils.JwtUtil;
import com.student.studentscoresystem.vo.LoginVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;

@RestController
@RequestMapping("/user")
public class UserInfoController {


    private final ISysUserService sysUserService;
    private final SysUserPositionMapper sysUserPositionMapper;
    private final SysPositionMapper sysPositionMapper;

    public UserInfoController(
            ISysUserService sysUserService,
            SysUserPositionMapper sysUserPositionMapper,
            SysPositionMapper sysPositionMapper
    ){

        this.sysUserService = sysUserService;

        this.sysUserPositionMapper =
                sysUserPositionMapper;

        this.sysPositionMapper =
                sysPositionMapper;

    }


    @GetMapping("/student-list")
    public Result<?> studentList(HttpServletRequest request) {

        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            return Result.fail("请先登录");
        }

        try {
            Claims claims = JwtUtil.parseToken(token.substring(7));

            Long userId = claims.get("userId", Long.class);

            return Result.success(
                    sysUserService.list(
                            new LambdaQueryWrapper<SysUser>()
                                    .eq(SysUser::getStatus, (short) 1)
                                    .orderByAsc(SysUser::getStudentNo)
                    )
            );

        } catch (Exception e) {
            return Result.fail("Token无效");
        }
    }
    @GetMapping("/info")
    public Result<LoginVO> info(
            HttpServletRequest request
    ){


        //1.获取请求头token

        String token =
                request.getHeader("Authorization");



        //2.去掉Bearer

        token =
                token.replace(
                        "Bearer ",
                        ""
                );



        //3.解析token

        Claims claims =
                JwtUtil.parseToken(token);



        Long userId =
                claims.get("userId",Long.class);


        LoginVO vo =
                new LoginVO();

        //4.根据id查询用户

        SysUser user =
                sysUserService.getById(userId);
        SysUserPosition userPosition =
                sysUserPositionMapper.selectOne(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .eq(
                                        SysUserPosition::getUserId,
                                        userId
                                )
                );


        if(userPosition != null){

            SysPosition position =
                    sysPositionMapper.selectById(
                            userPosition.getPositionId()
                    );


            if(position != null){

                vo.setRole(
                        position.getName()
                );

            }

        }





        vo.setId(user.getId());

        vo.setUsername(
                user.getUsername()
        );

        vo.setRealName(
                user.getRealName()
        );



        return Result.success(vo);

    }

}