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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

    /**
     * BCrypt 密码编码器
     */
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public LoginController(
            ISysUserService sysUserService,
            SysUserPositionMapper sysUserPositionMapper,
            SysPositionMapper sysPositionMapper,
            SysUserDepartmentMapper sysUserDepartmentMapper,
            DepartmentMapper departmentMapper
    ) {

        this.sysUserService =
                sysUserService;

        this.sysUserPositionMapper =
                sysUserPositionMapper;

        this.sysPositionMapper =
                sysPositionMapper;

        this.sysUserDepartmentMapper =
                sysUserDepartmentMapper;

        this.departmentMapper =
                departmentMapper;
    }

    /**
     * 登录
     */
    @PostMapping
    public Result<LoginVO> login(
            @RequestBody LoginDTO loginDTO
    ) {

        // =====================================================
        // 1. 参数检查
        // =====================================================

        if (loginDTO == null
                || loginDTO.getUsername() == null
                || loginDTO.getUsername().trim().isEmpty()) {

            throw new RuntimeException("请输入用户名");
        }

        if (loginDTO.getPassword() == null
                || loginDTO.getPassword().isEmpty()) {

            throw new RuntimeException("请输入密码");
        }

        String username =
                loginDTO.getUsername().trim();

        String inputPassword =
                loginDTO.getPassword();

        // =====================================================
        // 2. 查询用户
        // =====================================================

        SysUser user =
                sysUserService.getOne(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(
                                        SysUser::getUsername,
                                        username
                                )
                );

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // =====================================================
        // 3. 检查账号状态
        // =====================================================

        if (user.getStatus() != null
                && user.getStatus() != 1) {

            throw new RuntimeException("账号已停用");
        }

        // =====================================================
        // 4. 校验密码
        //
        // 新密码：
        //     BCrypt
        //
        // 老账号：
        //     可能还是明文
        //
        // 为了避免你数据库清空之前创建的账号全部不能登录，
        // 这里暂时兼容两种密码。
        // =====================================================

        String databasePassword =
                user.getPassword();

        boolean passwordCorrect =
                false;

        boolean oldPlainPassword =
                false;

        if (databasePassword != null
                && !databasePassword.isEmpty()) {

            /*
             * BCrypt 密码一般以这些前缀开头
             */
            if (databasePassword.startsWith("$2a$")
                    || databasePassword.startsWith("$2b$")
                    || databasePassword.startsWith("$2y$")) {

                try {

                    passwordCorrect =
                            passwordEncoder.matches(
                                    inputPassword,
                                    databasePassword
                            );

                } catch (Exception ignored) {

                    passwordCorrect = false;
                }

            } else {

                /*
                 * 兼容旧版明文密码
                 */
                passwordCorrect =
                        databasePassword.equals(
                                inputPassword
                        );

                oldPlainPassword =
                        passwordCorrect;
            }
        }

        if (!passwordCorrect) {

            throw new RuntimeException("密码错误");
        }

        // =====================================================
        // 5. 老账号登录成功后自动升级成 BCrypt
        // =====================================================

        if (oldPlainPassword) {

            user.setPassword(
                    passwordEncoder.encode(
                            inputPassword
                    )
            );

            /*
             * 如果数据库中没有 first_login，
             * 默认不强制老账号改密码。
             */
            if (user.getFirstLogin() == null) {

                user.setFirstLogin(
                        (short) 0
                );
            }

            sysUserService.updateById(user);
        }

        // =====================================================
        // 6. 构造登录返回对象
        // =====================================================

        LoginVO vo =
                new LoginVO();

        vo.setId(
                user.getId()
        );

        vo.setUsername(
                user.getUsername()
        );

        vo.setRealName(
                user.getRealName()
        );

        // =====================================================
        // 7. 默认角色：学生
        // =====================================================

        vo.setRole("学生");

        // =====================================================
        // 8. 查询岗位
        // =====================================================

        List<SysUserPosition> userPositions =
                sysUserPositionMapper.selectList(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .eq(
                                        SysUserPosition::getUserId,
                                        user.getId()
                                )
                );

        if (userPositions != null
                && !userPositions.isEmpty()) {

            for (
                    SysUserPosition userPosition
                    : userPositions
            ) {

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
                        && !position.getName()
                        .trim()
                        .isEmpty()) {

                    vo.setRole(
                            position.getName()
                    );

                    break;
                }
            }
        }

        // =====================================================
        // 9. 查询部门
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

            for (
                    SysUserDepartment relation
                    : relations
            ) {

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

                departments.add(
                        departmentVO
                );
            }
        }

        vo.setDepartments(
                departments
        );

        // =====================================================
        // 10. 首次登录状态
        // =====================================================

        vo.setFirstLogin(
                user.getFirstLogin() != null
                        && user.getFirstLogin() == 1
        );

        // =====================================================
        // 11. 创建 Token
        // =====================================================

        String token =
                JwtUtil.createToken(
                        user.getId(),
                        user.getUsername()
                );

        vo.setToken(token);

        // =====================================================
        // 12. 返回
        // =====================================================

        return Result.success(vo);
    }
}