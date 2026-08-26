package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.StudentAddDTO;
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
     * 学生分页查询
     * =========================================================
     *
     * GET /user/student/list
     *
     * 参数：
     *
     * page=1
     * pageSize=10
     * studentNo=
     * realName=
     * className=
     * status=
     *
     * 重点：
     *
     * 不能使用 selectOne 查询“学生”岗位。
     *
     * 因为数据库中可能存在多个“学生”岗位，
     * 例如：
     *
     * id=6  学生
     * id=7  学生
     *
     * 所以这里查询所有“学生”岗位，
     * 然后查询所有绑定这些岗位的用户。
     * =========================================================
     */
    @GetMapping("/student/list")
    public Result<Page<StudentVO>> studentList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) Short status
    ) {

        /*
         * =====================================================
         * 1. 修正分页参数
         * =====================================================
         */

        if (page < 1) {
            page = 1;
        }

        if (pageSize < 1) {
            pageSize = 10;
        }

        if (pageSize > 100) {
            pageSize = 100;
        }

        /*
         * =====================================================
         * 2. 查询所有“学生”岗位
         *
         * 注意：
         * 这里绝对不能 selectOne()
         *
         * 因为你的数据库现在已经存在：
         *
         * 6  学生
         * 7  学生
         *
         * 所以必须 selectList()
         * =====================================================
         */

        List<SysPosition> studentPositions =
                positionMapper.selectList(
                        new LambdaQueryWrapper<SysPosition>()
                                .eq(
                                        SysPosition::getName,
                                        "学生"
                                )
                                .eq(
                                        SysPosition::getStatus,
                                        (short) 1
                                )
                );

        /*
         * 没有学生岗位
         */
        if (
                studentPositions == null
                        || studentPositions.isEmpty()
        ) {

            return Result.success(
                    emptyStudentPage(
                            page,
                            pageSize
                    )
            );
        }

        /*
         * =====================================================
         * 3. 得到所有“学生”岗位 ID
         * =====================================================
         */

        List<Long> positionIds =
                new ArrayList<>();

        for (SysPosition position : studentPositions) {

            if (position == null) {
                continue;
            }

            if (position.getId() == null) {
                continue;
            }

            positionIds.add(
                    position.getId()
            );
        }

        if (positionIds.isEmpty()) {

            return Result.success(
                    emptyStudentPage(
                            page,
                            pageSize
                    )
            );
        }

        /*
         * =====================================================
         * 4. 查询所有绑定“学生”岗位的用户关系
         * =====================================================
         */

        List<SysUserPosition> relations =
                userPositionMapper.selectList(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .in(
                                        SysUserPosition::getPositionId,
                                        positionIds
                                )
                );

        if (
                relations == null
                        || relations.isEmpty()
        ) {

            return Result.success(
                    emptyStudentPage(
                            page,
                            pageSize
                    )
            );
        }

        /*
         * =====================================================
         * 5. 得到学生用户 ID
         * =====================================================
         *
         * 使用 List 去重。
         *
         * 因为同一个学生可能因为历史数据，
         * 同时绑定了两个“学生”岗位。
         * =====================================================
         */

        List<Long> userIds =
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

            if (!userIds.contains(userId)) {

                userIds.add(userId);
            }
        }

        if (userIds.isEmpty()) {

            return Result.success(
                    emptyStudentPage(
                            page,
                            pageSize
                    )
            );
        }

        /*
         * =====================================================
         * 6. 查询学生
         * =====================================================
         */

        Page<SysUser> userPage =
                new Page<>(
                        page,
                        pageSize
                );

        LambdaQueryWrapper<SysUser> wrapper =
                new LambdaQueryWrapper<>();

        /*
         * 只查询已经绑定“学生”岗位的用户
         */
        wrapper.in(
                SysUser::getId,
                userIds
        );

        /*
         * =====================================================
         * 学号搜索
         * =====================================================
         */

        if (
                studentNo != null
                        && !studentNo.trim().isEmpty()
        ) {

            wrapper.like(
                    SysUser::getStudentNo,
                    studentNo.trim()
            );
        }

        /*
         * =====================================================
         * 姓名搜索
         * =====================================================
         */

        if (
                realName != null
                        && !realName.trim().isEmpty()
        ) {

            wrapper.like(
                    SysUser::getRealName,
                    realName.trim()
            );
        }

        /*
         * =====================================================
         * 班级搜索
         * =====================================================
         */

        if (
                className != null
                        && !className.trim().isEmpty()
        ) {

            wrapper.like(
                    SysUser::getClassName,
                    className.trim()
            );
        }

        /*
         * =====================================================
         * 状态搜索
         * =====================================================
         */

        if (status != null) {

            wrapper.eq(
                    SysUser::getStatus,
                    status
            );
        }

        /*
         * =====================================================
         * 按学号排序
         * =====================================================
         */

        wrapper.orderByAsc(
                SysUser::getStudentNo
        );

        /*
         * =====================================================
         * 执行分页查询
         * =====================================================
         */

        Page<SysUser> resultPage =
                sysUserMapper.selectPage(
                        userPage,
                        wrapper
                );

        /*
         * =====================================================
         * 7. SysUser -> StudentVO
         * =====================================================
         */

        Page<StudentVO> voPage =
                new Page<>(
                        resultPage.getCurrent(),
                        resultPage.getSize(),
                        resultPage.getTotal()
                );

        List<StudentVO> voList =
                new ArrayList<>();

        for (
                SysUser user
                : resultPage.getRecords()
        ) {

            if (user == null) {
                continue;
            }

            voList.add(
                    buildStudentVO(user)
            );
        }

        voPage.setRecords(
                voList
        );

        return Result.success(
                voPage
        );
    }

    /**
     * =========================================================
     * 获取全部学生
     * =========================================================
     *
     * 给：
     * 1. 成绩管理
     * 2. 成绩调整
     * 3. 下拉选择学生
     *
     * 使用。
     *
     * GET /user/student/all
     *
     * 返回：
     *
     * [
     *   {
     *      id: 8,
     *      studentNo: "2025405884",
     *      realName: "阿拉帕提",
     *      username: "...",
     *      className: "...",
     *      phone: "...",
     *      status: 1
     *   }
     * ]
     */
    @GetMapping("/student/all")
    public Result<List<StudentVO>> allStudents() {

        /*
         * =====================================================
         * 1. 查询“学生”岗位
         * =====================================================
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

            return Result.success(
                    new ArrayList<>()
            );
        }


        /*
         * =====================================================
         * 2. 查询所有学生岗位关系
         * =====================================================
         */

        List<SysUserPosition> relations =
                userPositionMapper.selectList(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .eq(
                                        SysUserPosition::getPositionId,
                                        studentPosition.getId()
                                )
                );

        if (
                relations == null
                        || relations.isEmpty()
        ) {

            return Result.success(
                    new ArrayList<>()
            );
        }


        /*
         * =====================================================
         * 3. 获取学生 userId
         * =====================================================
         */

        List<Long> userIds =
                new ArrayList<>();

        for (
                SysUserPosition relation
                : relations
        ) {

            if (relation == null) {
                continue;
            }

            if (relation.getUserId() == null) {
                continue;
            }

            userIds.add(
                    relation.getUserId()
            );
        }


        if (userIds.isEmpty()) {

            return Result.success(
                    new ArrayList<>()
            );
        }


        /*
         * =====================================================
         * 4. 查询所有学生
         * =====================================================
         */

        List<SysUser> users =
                sysUserMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .in(
                                        SysUser::getId,
                                        userIds
                                )
                                .orderByAsc(
                                        SysUser::getStudentNo
                                )
                );


        /*
         * =====================================================
         * 5. SysUser -> StudentVO
         * =====================================================
         */

        List<StudentVO> result =
                new ArrayList<>();


        if (
                users == null
                        || users.isEmpty()
        ) {

            return Result.success(
                    result
            );
        }


        for (
                SysUser user
                : users
        ) {

            if (user == null) {
                continue;
            }

            result.add(
                    buildStudentVO(user)
            );
        }


        return Result.success(
                result
        );
    }
    /**
     * =========================================================
     * 搜索学生
     * =========================================================
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

        SysUser user =
                sysUserMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>()
                                .and(wrapper ->
                                        wrapper
                                                .like(
                                                        SysUser::getStudentNo,
                                                        key
                                                )
                                                .or()
                                                .like(
                                                        SysUser::getRealName,
                                                        key
                                                )
                                                .or()
                                                .like(
                                                        SysUser::getUsername,
                                                        key
                                                )
                                                .or()
                                                .like(
                                                        SysUser::getClassName,
                                                        key
                                                )
                                )
                                .last(
                                        "LIMIT 1"
                                )
                );

        if (user == null) {

            return Result.success(null);
        }

        return Result.success(
                buildStudentVO(user)
        );
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
     */
    @PostMapping("/student/add")
    public Result<Void> addStudent(
            @RequestBody StudentAddDTO dto
    ) {

        if (dto == null) {

            return Result.error(
                    "参数不能为空"
            );
        }

        /*
         * =====================================================
         * 1. 创建用户
         * =====================================================
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
         * =====================================================
         * 2. 查询“学生”岗位
         *
         * 不再使用 selectOne()
         *
         * 因为数据库可能存在多个“学生”岗位。
         *
         * 这里取 ID 最小的一个作为默认学生岗位。
         * =====================================================
         */

        SysPosition studentPosition =
                positionMapper.selectList(
                                new LambdaQueryWrapper<SysPosition>()
                                        .eq(
                                                SysPosition::getName,
                                                "学生"
                                        )
                                        .eq(
                                                SysPosition::getStatus,
                                                (short) 1
                                        )
                                        .orderByAsc(
                                                SysPosition::getId
                                        )
                                        .last(
                                                "LIMIT 1"
                                        )
                        )
                        .stream()
                        .findFirst()
                        .orElse(null);

        if (studentPosition == null) {

            return Result.error(
                    "学生岗位不存在，请先创建“学生”岗位"
            );
        }

        /*
         * =====================================================
         * 3. 检查学生岗位关系是否已经存在
         * =====================================================
         */

        SysUserPosition relation =
                userPositionMapper.selectOne(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .eq(
                                        SysUserPosition::getUserId,
                                        user.getId()
                                )
                                .eq(
                                        SysUserPosition::getPositionId,
                                        studentPosition.getId()
                                )
                                .last(
                                        "LIMIT 1"
                                )
                );

        /*
         * =====================================================
         * 4. 不存在 → 创建岗位关系
         * =====================================================
         */

        if (relation == null) {

            relation =
                    new SysUserPosition();

            relation.setUserId(
                    user.getId()
            );

            relation.setPositionId(
                    studentPosition.getId()
            );

            /*
             * 你的表 department_id 是 NOT NULL，
             * 所以这里必须赋值。
             *
             * 普通学生不属于部门，
             * 但你的数据库设计要求不能为空。
             *
             * 因此暂时使用 1。
             */
            relation.setDepartmentId(
                    1L
            );

            userPositionMapper.insert(
                    relation
            );
        }

        return Result.success(null);
    }


    /**
     * =========================================================
     * 测试
     * =========================================================
     */
    @GetMapping("/test")
    public String test() {

        return "ok";
    }


    /**
     * =========================================================
     * 构造空学生分页
     * =========================================================
     */
    private Page<StudentVO> emptyStudentPage(
            long page,
            long pageSize
    ) {

        Page<StudentVO> emptyPage =
                new Page<>(
                        page,
                        pageSize,
                        0
                );

        emptyPage.setRecords(
                new ArrayList<>()
        );

        return emptyPage;
    }


    /**
     * =========================================================
     * StudentVO
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
}