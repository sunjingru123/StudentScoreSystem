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
     * GET /user/student/list
     */
    @GetMapping("/student/list")
    public Result<List<StudentVO>> studentList() {

        /*
         * 1. 查询“学生”岗位
         */
        SysPosition position =
                positionMapper.selectOne(
                        new LambdaQueryWrapper<SysPosition>()
                                .eq(
                                        SysPosition::getName,
                                        "学生"
                                )
                );

        if (position == null) {
            System.out.println(
                    "========== 学生岗位不存在 =========="
            );

            return Result.success(
                    new ArrayList<>()
            );
        }

        System.out.println(
                "========== 学生岗位ID："
                        + position.getId()
                        + " =========="
        );


        /*
         * 2. 查询学生岗位关系
         */
        List<SysUserPosition> relations =
                userPositionMapper.selectList(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .eq(
                                        SysUserPosition::getPositionId,
                                        position.getId()
                                )
                );

        System.out.println(
                "========== 学生关系数量："
                        + relations.size()
                        + " =========="
        );


        /*
         * 3. 组装学生
         */
        List<StudentVO> result =
                new ArrayList<>();


        for (SysUserPosition relation : relations) {

            if (relation == null) {
                continue;
            }

            Long userId =
                    relation.getUserId();

            if (userId == null) {
                continue;
            }


            SysUser user =
                    sysUserMapper.selectById(
                            userId
                    );


            /*
             * 防止关系表存在脏数据
             */
            if (user == null) {
                System.out.println(
                        "学生关系对应用户不存在，userId="
                                + userId
                );

                continue;
            }


            StudentVO vo =
                    buildStudentVO(user);

            result.add(vo);
        }


        System.out.println(
                "========== 最终学生数量："
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
         * 1. 创建用户
         */
        SysUser user =
                new SysUser();

        user.setStudentNo(
                dto.getStudentNo()
        );

        user.setUsername(
                dto.getUsername()
        );

        user.setPassword(
                dto.getPassword()
        );

        user.setRealName(
                dto.getRealName()
        );

        user.setGender(
                dto.getGender()
        );

        user.setPhone(
                dto.getPhone()
        );

        user.setEmail(
                dto.getEmail()
        );

        user.setClassName(
                dto.getClassName()
        );

        user.setStatus(
                (short) 1
        );


        int insert =
                sysUserMapper.insert(
                        user
                );


        if (insert <= 0) {

            return Result.error(
                    "学生创建失败"
            );
        }


        /*
         * 2. 查询学生岗位
         */
        SysPosition studentPosition =
                positionMapper.selectOne(
                        new LambdaQueryWrapper<SysPosition>()
                                .eq(
                                        SysPosition::getName,
                                        "学生"
                                )
                );


        if (studentPosition == null) {

            /*
             * 用户已经创建了，
             * 但岗位不存在。
             *
             * 这里直接提示。
             */
            return Result.error(
                    "学生岗位不存在，请先创建“学生”岗位"
            );
        }


        /*
         * 3. 建立学生岗位关系
         */
        SysUserPosition relation =
                new SysUserPosition();

        relation.setUserId(
                user.getId()
        );

        relation.setPositionId(
                studentPosition.getId()
        );

        relation.setDepartmentId(
                1L
        );


        int relationInsert =
                userPositionMapper.insert(
                        relation
                );


        if (relationInsert <= 0) {

            return Result.error(
                    "学生岗位绑定失败"
            );
        }


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