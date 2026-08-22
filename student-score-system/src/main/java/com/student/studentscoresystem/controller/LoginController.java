package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.dto.LoginDTO;
import com.student.studentscoresystem.entity.*;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.utils.JwtUtil;
import com.student.studentscoresystem.vo.LoginVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.student.studentscoresystem.service.ISysUserService;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.vo.DepartmentMemberVO;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/login")
public class LoginController {


    private final ISysUserService sysUserService;

    private final SysUserPositionMapper sysUserPositionMapper;

    private final SysPositionMapper sysPositionMapper;

    private final SysUserDepartmentMapper sysUserDepartmentMapper;
    private final DepartmentMapper departmentMapper;

    public LoginController(
            ISysUserService sysUserService,
            SysUserPositionMapper sysUserPositionMapper,
            SysPositionMapper sysPositionMapper,
            SysUserDepartmentMapper sysUserDepartmentMapper,
            DepartmentMapper departmentMapper
    ){

        this.sysUserService = sysUserService;
        this.sysUserPositionMapper = sysUserPositionMapper;
        this.sysPositionMapper = sysPositionMapper;
        this.sysUserDepartmentMapper = sysUserDepartmentMapper;
        this.departmentMapper = departmentMapper;

    }


    @PostMapping
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        // 查询用户
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getUsername, loginDTO.getUsername());
        SysUser user = sysUserService.getOne(userWrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!user.getPassword().equals(loginDTO.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        LoginVO vo = new LoginVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());

        // 查询岗位角色
        LambdaQueryWrapper<SysUserPosition> upWrapper = new LambdaQueryWrapper<>();
        upWrapper.eq(SysUserPosition::getUserId, user.getId());
        SysUserPosition userPosition = sysUserPositionMapper.selectOne(upWrapper);

        if (userPosition != null) {
            SysPosition position = sysPositionMapper.selectById(userPosition.getPositionId());
            if (position != null) {
                vo.setRole(position.getName());
            }

        }


// ============================
// 查询学生所属部门及职位
// ============================

        List<SysUserDepartment> relations =
                sysUserDepartmentMapper.selectList(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getUserId,
                                        user.getId()
                                )
                                .eq(
                                        SysUserDepartment::getStatus,
                                        (short) 1
                                )
                );

        List<DepartmentMemberVO> departments =
                new ArrayList<>();

        for (SysUserDepartment relation : relations) {

            Department department =
                    departmentMapper.selectById(
                            relation.getDepartmentId()
                    );

            if (department == null) {
                continue;
            }

            DepartmentMemberVO departmentVO =
                    new DepartmentMemberVO();

            departmentVO.setDepartmentId(
                    department.getId()
            );

            departmentVO.setDepartmentName(
                    department.getName()
            );

            departmentVO.setPosition(
                    relation.getPosition()
            );

            departments.add(departmentVO);
        }

        vo.setDepartments(departments);
        // 无论有没有角色，都生成token
        String token = JwtUtil.createToken(user.getId(), user.getUsername());
        vo.setToken(token);

        return Result.success(vo);
    }
}