package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.LoginDTO;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.ISysUserService;
import com.student.studentscoresystem.utils.JwtUtil;
import com.student.studentscoresystem.vo.DepartmentMemberVO;
import com.student.studentscoresystem.vo.LoginVO;
import org.springframework.web.bind.annotation.*;

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
    ) {
        this.sysUserService = sysUserService;
        this.sysUserPositionMapper = sysUserPositionMapper;
        this.sysPositionMapper = sysPositionMapper;
        this.sysUserDepartmentMapper = sysUserDepartmentMapper;
        this.departmentMapper = departmentMapper;
    }

    @PostMapping
    public Result<LoginVO> login(
            @RequestBody LoginDTO loginDTO
    ) {

        // =====================================================
        // 1. 查询用户
        // =====================================================

        LambdaQueryWrapper<SysUser> userWrapper =
                new LambdaQueryWrapper<>();

        userWrapper.eq(
                SysUser::getUsername,
                loginDTO.getUsername()
        );

        SysUser user =
                sysUserService.getOne(userWrapper);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // =====================================================
        // 2. 校验密码
        // =====================================================

        if (user.getPassword() == null
                || !user.getPassword().equals(loginDTO.getPassword())) {

            throw new RuntimeException("密码错误");
        }

        // =====================================================
        // 3. 检查账号状态
        // =====================================================

        if (user.getStatus() != null
                && user.getStatus() != 1) {

            throw new RuntimeException("账号已停用");
        }

        // =====================================================
        // 4. 构造登录返回对象
        // =====================================================

        LoginVO vo = new LoginVO();

        vo.setId(user.getId());

        vo.setUsername(user.getUsername());

        vo.setRealName(user.getRealName());

        // =====================================================
        // 5. 默认角色
        //
        // 所有通过学生 Excel 导入的学生，
        // 默认就是“学生”。
        //
        // 注意：
        // 这里不需要在 sys_user 里面增加角色字段。
        //
        // 因为普通学生没有 SysUserPosition，
        // 所以直接默认“学生”。
        // =====================================================

        vo.setRole("学生");

        // =====================================================
        // 6. 查询用户岗位
        //
        // 部门成员导入以后：
        //
        // 学生会 + 部长
        // 学生会 + 副部长
        // 学生会 + 干事
        //
        // 才会存在 SysUserPosition。
        // =====================================================

        List<SysUserPosition> userPositions =
                sysUserPositionMapper.selectList(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .eq(
                                        SysUserPosition::getUserId,
                                        user.getId()
                                )
                );

        // =====================================================
        // 7. 如果存在岗位，则使用岗位作为角色
        //
        // 没有岗位：
        //     学生
        //
        // 有岗位：
        //     部长 / 副部长 / 干事 / 管理员
        // =====================================================

        if (userPositions != null
                && !userPositions.isEmpty()) {

            /*
             * 先取第一条有效岗位。
             *
             * 后面如果你需要：
             *
             * 一个学生在不同部门拥有不同职位，
             *
             * 可以再进一步做角色优先级。
             */
            for (SysUserPosition userPosition : userPositions) {

                if (userPosition == null) {
                    continue;
                }

                if (userPosition.getPositionId() == null) {
                    continue;
                }

                SysPosition position =
                        sysPositionMapper.selectById(
                                userPosition.getPositionId()
                        );

                if (position == null) {
                    continue;
                }

                if (position.getStatus() != null
                        && position.getStatus() != 1) {
                    continue;
                }

                if (position.getName() != null
                        && !position.getName().trim().isEmpty()) {

                    vo.setRole(position.getName());

                    break;
                }
            }
        }

        // =====================================================
        // 8. 查询学生所属部门
        // =====================================================

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

        if (relations != null) {

            for (SysUserDepartment relation : relations) {

                if (relation == null) {
                    continue;
                }

                if (relation.getDepartmentId() == null) {
                    continue;
                }

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
        }

        // =====================================================
        // 9. 设置部门
        // =====================================================

        vo.setDepartments(departments);

        // =====================================================
        // 10. 生成 Token
        // =====================================================

        String token =
                JwtUtil.createToken(
                        user.getId(),
                        user.getUsername()
                );

        vo.setToken(token);

        // =====================================================
        // 11. 返回
        // =====================================================

        return Result.success(vo);
    }
}