package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.vo.StudentVO;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/user")
public class SysUserController {

    private final SysUserMapper sysUserMapper;

    private final SysUserPositionMapper userPositionMapper;

    private final SysPositionMapper positionMapper;

    public SysUserController(
            SysUserMapper sysUserMapper,
            SysUserPositionMapper userPositionMapper,
            SysPositionMapper positionMapper
    ) {
        this.sysUserMapper = sysUserMapper;
        this.userPositionMapper = userPositionMapper;
        this.positionMapper = positionMapper;
    }


    /**
     * =========================================================
     * 查询全部学生
     * =========================================================
     *
     * 学生名单直接来自 sys_user
     *
     * 规则：
     * 1. studentNo 不为空 = 学生
     * 2. 管理员通常没有 studentNo，因此自动排除
     * 3. 不依赖 sys_user_position
     * 4. Excel 导入多少学生，这里就显示多少学生
     */
    @GetMapping("/student/list")
    public Result<List<StudentVO>> studentList() {

        List<SysUser> users =
                sysUserMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .isNotNull(SysUser::getStudentNo)
                                .orderByAsc(SysUser::getStudentNo)
                );

        List<StudentVO> result =
                new ArrayList<>();

        for (SysUser user : users) {

            if (user == null) {
                continue;
            }

            StudentVO vo = buildStudentVO(user);

            result.add(vo);
        }

        System.out.println(
                "========== 学生管理查询数量："
                        + result.size()
                        + " =========="
        );

        return Result.success(result);
    }


    /**
     * =========================================================
     * 搜索学生
     * =========================================================
     *
     * GET /user/student/search?keyword=xxx
     */
    @GetMapping("/student/search")
    public Result<StudentVO> searchStudent(
            @RequestParam String keyword
    ) {

        if (
                keyword == null
                        || keyword.trim().isEmpty()
        ) {
            return Result.success(null);
        }


        String key =
                keyword.trim();


        /*
         * 先获取学生列表
         */
        Result<List<StudentVO>> result =
                studentList();


        List<StudentVO> students =
                result.getData();


        if (
                students == null
                        || students.isEmpty()
        ) {
            return Result.success(null);
        }


        /*
         * 模糊搜索
         */
        for (StudentVO student : students) {

            if (
                    contains(
                            student.getStudentNo(),
                            key
                    )
                            ||
                            contains(
                                    student.getRealName(),
                                    key
                            )
                            ||
                            contains(
                                    student.getUsername(),
                                    key
                            )
                            ||
                            contains(
                                    student.getClassName(),
                                    key
                            )
            ) {

                return Result.success(
                        student
                );
            }
        }


        return Result.success(null);
    }


    /**
     * =========================================================
     * 禁用学生
     * =========================================================
     */
    @PutMapping("/student/disable/{id}")
    public Result<Void> disableStudent(
            @PathVariable Long id
    ) {

        SysUser user =
                sysUserMapper.selectById(id);


        if (user == null) {
            return Result.error(
                    "用户不存在"
            );
        }


        user.setStatus(
                (short) 0
        );


        sysUserMapper.updateById(
                user
        );


        return Result.success(null);
    }


    /**
     * =========================================================
     * 启用学生
     * =========================================================
     */
    @PutMapping("/student/enable/{id}")
    public Result<Void> enableStudent(
            @PathVariable Long id
    ) {

        SysUser user =
                sysUserMapper.selectById(id);


        if (user == null) {
            return Result.error(
                    "用户不存在"
            );
        }


        user.setStatus(
                (short) 1
        );


        sysUserMapper.updateById(
                user
        );


        return Result.success(null);
    }


    /**
     * =========================================================
     * 添加学生
     * =========================================================
     *
     * POST /user/student/add
     */
    /**
     * =========================================================
     * 添加学生
     * =========================================================
     *
     * 学生创建后：
     *
     * 1. 写入 sys_user
     * 2. 默认就是学生
     * 3. 不创建 sys_user_position
     * 4. 不创建 sys_user_department
     *
     * 以后如果这个学生加入某个部门，
     * 再通过部门成员 Excel 导入：
     *
     * 部门 + 学号 + 姓名 + 职位
     *
     * 创建部门和职位关系。
     */
    @PostMapping("/student/add")
    public Result<Void> addStudent(
            @RequestBody
            com.student.studentscoresystem.dto.StudentAddDTO dto
    ) {

        System.out.println(
                "========== 添加学生接口进入 =========="
        );

        System.out.println(dto);

        /*
         * =====================================================
         * 1. 基本校验
         * =====================================================
         */

        if (
                dto.getStudentNo() == null
                        || dto.getStudentNo().trim().isEmpty()
        ) {
            return Result.error("学号不能为空");
        }

        if (
                dto.getRealName() == null
                        || dto.getRealName().trim().isEmpty()
        ) {
            return Result.error("姓名不能为空");
        }

        /*
         * =====================================================
         * 2. 检查学号是否已经存在
         * =====================================================
         */

        SysUser existUser =
                sysUserMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(
                                        SysUser::getStudentNo,
                                        dto.getStudentNo().trim()
                                )
                                .last("LIMIT 1")
                );

        if (existUser != null) {

            return Result.error(
                    "该学号已经存在：" +
                            dto.getStudentNo()
            );
        }

        /*
         * =====================================================
         * 3. 创建学生
         * =====================================================
         */

        SysUser user =
                new SysUser();

        /*
         * 学号
         */
        user.setStudentNo(
                dto.getStudentNo().trim()
        );

        /*
         * 如果没有填写用户名，
         * 默认使用学号。
         */
        if (
                dto.getUsername() == null
                        || dto.getUsername().trim().isEmpty()
        ) {

            user.setUsername(
                    dto.getStudentNo().trim()
            );

        } else {

            user.setUsername(
                    dto.getUsername().trim()
            );
        }

        /*
         * 如果没有填写密码，
         * 默认使用学号。
         */
        if (
                dto.getPassword() == null
                        || dto.getPassword().trim().isEmpty()
        ) {

            user.setPassword(
                    dto.getStudentNo().trim()
            );

        } else {

            user.setPassword(
                    dto.getPassword()
            );
        }

        /*
         * 姓名
         */
        user.setRealName(
                dto.getRealName().trim()
        );

        /*
         * 性别
         */
        user.setGender(
                dto.getGender()
        );

        /*
         * 手机号
         */
        user.setPhone(
                dto.getPhone()
        );

        /*
         * 邮箱
         */
        user.setEmail(
                dto.getEmail()
        );

        /*
         * 班级
         */
        user.setClassName(
                dto.getClassName()
        );

        /*
         * 默认启用
         */
        user.setStatus(
                (short) 1
        );

        /*
         * =====================================================
         * 4. 插入 sys_user
         * =====================================================
         */

        int insert =
                sysUserMapper.insert(user);

        if (insert <= 0) {

            return Result.error(
                    "学生创建失败"
            );
        }

        /*
         * =====================================================
         * 注意：
         *
         * 这里故意不创建：
         *
         * sys_user_position
         *
         * 也不创建：
         *
         * sys_user_department
         *
         * 因为普通学生不需要部门职位。
         *
         * 学生加入部门以后，
         * 再通过部门成员 Excel 导入。
         * =====================================================
         */

        System.out.println(
                "========== 学生创建成功 =========="
        );

        System.out.println(
                "id = " + user.getId()
        );

        System.out.println(
                "studentNo = " + user.getStudentNo()
        );

        return Result.success(null);
    }


    /**
     * =========================================================
     * 测试接口
     * =========================================================
     */
    @GetMapping("/test")
    public String test() {
        return "ok";
    }


    /**
     * =========================================================
     * 构造 StudentVO
     * =========================================================
     */
    private StudentVO buildStudentVO(
            SysUser user
    ) {

        StudentVO vo =
                new StudentVO();


        vo.setId(
                user.getId()
        );

        vo.setStudentNo(
                user.getStudentNo()
        );

        vo.setUsername(
                user.getUsername()
        );

        vo.setRealName(
                user.getRealName()
        );

        vo.setPhone(
                user.getPhone()
        );

        vo.setClassName(
                user.getClassName()
        );

        vo.setStatus(
                user.getStatus()
        );


        return vo;
    }


    /**
     * =========================================================
     * 模糊匹配
     * =========================================================
     */
    private boolean contains(
            String value,
            String keyword
    ) {

        if (
                value == null
                        || keyword == null
        ) {
            return false;
        }


        return value
                .toLowerCase()
                .contains(
                        keyword.toLowerCase()
                );
    }
}