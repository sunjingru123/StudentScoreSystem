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

import java.util.List;

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
            // 验证 Token
            JwtUtil.parseToken(token.substring(7));

            // 查询“学生”岗位
            SysPosition studentPosition =
                    sysPositionMapper.selectOne(
                            new LambdaQueryWrapper<SysPosition>()
                                    .eq(SysPosition::getName, "学生")
                    );

            if (studentPosition == null) {
                return Result.fail("学生岗位不存在");
            }

            // 查询拥有“学生”岗位的用户
            List<SysUserPosition> relations =
                    sysUserPositionMapper.selectList(
                            new LambdaQueryWrapper<SysUserPosition>()
                                    .eq(
                                            SysUserPosition::getPositionId,
                                            studentPosition.getId()
                                    )
                    );

            List<SysUser> students = new java.util.ArrayList<>();

            for (SysUserPosition relation : relations) {

                SysUser student =
                        sysUserService.getById(relation.getUserId());

                if (student != null
                        && student.getStatus() != null
                        && student.getStatus() == 1) {

                    students.add(student);
                }
            }

            return Result.success(students);

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