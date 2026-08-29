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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * 支持：
     *
     * pageNum
     * pageSize
     * keyword
     * studentNo
     * realName
     * className
     * status
     *
     * keyword 可以搜索：
     *
     * 1. 学号
     * 2. 姓名
     * 3. 用户名
     * 4. 班级
     *
     * =========================================================
     */
    @GetMapping("/student/list")
    public Result<Page<StudentVO>> studentList(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
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

        if (pageNum < 1) {
            pageNum = 1;
        }

        if (pageSize < 1) {
            pageSize = 10;
        }

        if (pageSize > 100) {
            pageSize = 100;
        }


        /*
         * =====================================================
         * 2. 查询学生用户 ID
         *
         * 这里不再依赖 selectOne。
         *
         * 同时兼容数据库中存在多个“学生”岗位。
         * =====================================================
         */

        Set<Long> studentUserIdSet =
                findStudentUserIds();


        /*
         * =====================================================
         * 3. 如果岗位关系能够找到学生
         * =====================================================
         */

        LambdaQueryWrapper<SysUser> wrapper =
                new LambdaQueryWrapper<>();


        if (!studentUserIdSet.isEmpty()) {

            wrapper.in(
                    SysUser::getId,
                    studentUserIdSet
            );

        } else {

            /*
             * =================================================
             * 4. 兜底方案
             *
             * 如果数据库里的“学生”岗位没有正确建立关系，
             * 不要直接让整个学生页面变成 0。
             *
             * 根据 studentNo 是否存在判断学生。
             *
             * 一般系统中学生都有学号，
             * 老师、管理员通常没有 studentNo。
             * =================================================
             */

            wrapper.isNotNull(
                    SysUser::getStudentNo
            );

            wrapper.ne(
                    SysUser::getStudentNo,
                    ""
            );
        }


        /*
         * =====================================================
         * 5. keyword 综合搜索
         * =====================================================
         */

        if (
                keyword != null
                        && !keyword.trim().isEmpty()
        ) {

            String key =
                    keyword.trim();

            wrapper.and(w ->
                    w.like(
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
            );
        }


        /*
         * =====================================================
         * 6. 单独学号搜索
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
         * 7. 单独姓名搜索
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
         * 8. 单独班级搜索
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
         * 9. 状态搜索
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
         * 10. 稳定排序
         * =====================================================
         */

        wrapper.orderByAsc(
                SysUser::getStudentNo
        );

        wrapper.orderByAsc(
                SysUser::getId
        );


        /*
         * =====================================================
         * 11. 真正分页查询
         * =====================================================
         */

        Page<SysUser> userPage =
                new Page<>(
                        pageNum,
                        pageSize
                );


        Page<SysUser> resultPage =
                sysUserMapper.selectPage(
                        userPage,
                        wrapper
                );


        /*
         * =====================================================
         * 12. Entity -> VO
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


        if (
                resultPage.getRecords() != null
        ) {

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
        }


        /*
         * =====================================================
         * 13. 设置记录
         * =====================================================
         */

        voPage.setRecords(
                voList
        );


        /*
         * =====================================================
         * 14. 控制台调试
         * =====================================================
         */

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "学生分页查询"
        );

        System.out.println(
                "pageNum = " + pageNum
        );

        System.out.println(
                "pageSize = " + pageSize
        );

        System.out.println(
                "keyword = " + keyword
        );

        System.out.println(
                "studentUserId数量 = "
                        + studentUserIdSet.size()
        );

        System.out.println(
                "查询到学生数量 = "
                        + resultPage.getRecords().size()
        );

        System.out.println(
                "学生总数 = "
                        + resultPage.getTotal()
        );

        System.out.println(
                "=========================================="
        );


        return Result.success(
                voPage
        );
    }


    /**
     * =========================================================
     * 查找所有学生 userId
     * =========================================================
     *
     * 这里专门负责：
     *
     * sys_position
     *       ↓
     * 学生岗位
     *       ↓
     * sys_user_position
     *       ↓
     * userId
     *
     * =========================================================
     */
    private Set<Long> findStudentUserIds() {

        Set<Long> userIdSet =
                new HashSet<>();


        /*
         * =====================================================
         * 1. 查询所有“学生”岗位
         * =====================================================
         */

        List<SysPosition> positions =
                positionMapper.selectList(
                        new LambdaQueryWrapper<SysPosition>()
                                .eq(
                                        SysPosition::getName,
                                        "学生"
                                )
                );


        if (
                positions == null
                        || positions.isEmpty()
        ) {

            System.out.println(
                    "没有找到名称为【学生】的岗位"
            );

            return userIdSet;
        }


        /*
         * =====================================================
         * 2. 获取岗位 ID
         * =====================================================
         */

        List<Long> positionIds =
                new ArrayList<>();


        for (
                SysPosition position
                : positions
        ) {

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

            return userIdSet;
        }


        /*
         * =====================================================
         * 3. 查询岗位关系
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

            System.out.println(
                    "找到学生岗位，但是没有找到岗位关系"
            );

            return userIdSet;
        }


        /*
         * =====================================================
         * 4. 获取 userId
         * =====================================================
         */

        for (
                SysUserPosition relation
                : relations
        ) {

            if (relation == null) {
                continue;
            }

            Long userId =
                    relation.getUserId();

            if (userId == null) {
                continue;
            }

            userIdSet.add(
                    userId
            );
        }


        System.out.println(
                "学生岗位数量 = "
                        + positionIds.size()
        );

        System.out.println(
                "学生用户数量 = "
                        + userIdSet.size()
        );


        return userIdSet;
    }


    /**
     * =========================================================
     * 获取全部学生
     * =========================================================
     *
     * GET /user/student/all
     * =========================================================
     */
    @GetMapping("/student/all")
    public Result<List<StudentVO>> allStudents() {

        Set<Long> studentUserIds =
                findStudentUserIds();


        List<SysUser> users;


        /*
         * 如果岗位关系正常
         */
        if (!studentUserIds.isEmpty()) {

            users =
                    sysUserMapper.selectList(
                            new LambdaQueryWrapper<SysUser>()
                                    .in(
                                            SysUser::getId,
                                            studentUserIds
                                    )
                                    .orderByAsc(
                                            SysUser::getStudentNo
                                    )
                    );

        } else {

            /*
             * 兜底：根据学号判断学生
             */

            users =
                    sysUserMapper.selectList(
                            new LambdaQueryWrapper<SysUser>()
                                    .isNotNull(
                                            SysUser::getStudentNo
                                    )
                                    .ne(
                                            SysUser::getStudentNo,
                                            ""
                                    )
                                    .orderByAsc(
                                            SysUser::getStudentNo
                                    )
                    );
        }


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
     *
     * GET /user/student/search?keyword=孙婧茹
     *
     * 这个接口保留。
     *
     * =========================================================
     */
    @GetMapping("/student/search")
    public Result<StudentVO> searchStudent(
            @RequestParam(required = false) String keyword
    ) {

        if (
                keyword == null
                        || keyword.trim().isEmpty()
        ) {

            return Result.success(null);
        }


        String key =
                keyword.trim();


        Set<Long> studentUserIds =
                findStudentUserIds();


        LambdaQueryWrapper<SysUser> wrapper =
                new LambdaQueryWrapper<>();


        /*
         * 优先使用学生岗位
         */

        if (!studentUserIds.isEmpty()) {

            wrapper.in(
                    SysUser::getId,
                    studentUserIds
            );

        } else {

            /*
             * 岗位关系不存在时使用学号兜底
             */

            wrapper.isNotNull(
                    SysUser::getStudentNo
            );

            wrapper.ne(
                    SysUser::getStudentNo,
                    ""
            );
        }


        /*
         * =====================================================
         * 综合搜索
         * =====================================================
         */

        wrapper.and(w ->
                w.like(
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
        );


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
         * 查询第一条
         * =====================================================
         */

        List<SysUser> users =
                sysUserMapper.selectList(
                        wrapper
                );


        if (
                users == null
                        || users.isEmpty()
        ) {

            return Result.success(null);
        }


        SysUser user =
                users.get(0);


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
         * 2. 查找学生岗位
         *
         * 不限制 status=1。
         *
         * 防止数据库里学生岗位因为 status
         * 导致找不到。
         * =====================================================
         */

        SysPosition studentPosition =
                positionMapper.selectList(
                                new LambdaQueryWrapper<SysPosition>()
                                        .eq(
                                                SysPosition::getName,
                                                "学生"
                                        )
                                        .orderByAsc(
                                                SysPosition::getId
                                        )
                        )
                        .stream()
                        .findFirst()
                        .orElse(null);


        if (studentPosition == null) {

            /*
             * 用户已经创建成功，
             * 但是岗位不存在。
             *
             * 不影响用户创建。
             */

            return Result.success(null);
        }


        /*
         * =====================================================
         * 3. 检查岗位关系
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
         * 4. 创建岗位关系
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
             * 你的数据库 department_id
             * 当前要求 NOT NULL。
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
     * 空分页
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
}