package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysSemester;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysSemesterMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/sysSemester")
public class SysSemesterController {

    private final SysSemesterMapper sysSemesterMapper;

    private final SysPositionMapper positionMapper;

    private final SysUserPositionMapper userPositionMapper;


    public SysSemesterController(
            SysSemesterMapper sysSemesterMapper,
            SysPositionMapper positionMapper,
            SysUserPositionMapper userPositionMapper
    ) {
        this.sysSemesterMapper = sysSemesterMapper;
        this.positionMapper = positionMapper;
        this.userPositionMapper = userPositionMapper;
    }


    /**
     * =========================================================
     * 判断当前用户是否为管理员
     * =========================================================
     */
    private boolean isAdmin(
            HttpServletRequest request
    ) {

        Object userIdAttr =
                request.getAttribute("userId");

        if (userIdAttr == null) {
            return false;
        }

        Long userId;

        try {

            userId =
                    Long.valueOf(
                            userIdAttr.toString()
                    );

        } catch (Exception e) {

            return false;
        }


        /*
         * 查询管理员岗位
         */
        SysPosition adminPosition =
                positionMapper.selectOne(
                        new LambdaQueryWrapper<SysPosition>()
                                .eq(
                                        SysPosition::getName,
                                        "管理员"
                                )
                                .eq(
                                        SysPosition::getStatus,
                                        (short) 1
                                )
                                .last(
                                        "LIMIT 1"
                                )
                );


        if (adminPosition == null) {
            return false;
        }


        /*
         * 查询用户是否绑定管理员岗位
         */
        Long count =
                userPositionMapper.selectCount(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .eq(
                                        SysUserPosition::getUserId,
                                        userId
                                )
                                .eq(
                                        SysUserPosition::getPositionId,
                                        adminPosition.getId()
                                )
                );


        return count != null
                && count > 0;
    }


    /**
     * =========================================================
     * 查询全部学期
     * =========================================================
     *
     * GET /sysSemester/list
     *
     * =========================================================
     */
    @GetMapping("/list")
    public Result<List<SysSemester>> list(
            HttpServletRequest request
    ) {

        if (!isAdmin(request)) {

            return Result.fail(
                    "没有管理员权限"
            );
        }


        List<SysSemester> list =
                sysSemesterMapper.selectList(
                        new LambdaQueryWrapper<SysSemester>()
                                .orderByDesc(
                                        SysSemester::getStartDate
                                )
                                .orderByDesc(
                                        SysSemester::getId
                                )
                );


        return Result.success(
                list
        );
    }


    /**
     * =========================================================
     * 查询当前学期
     * =========================================================
     *
     * GET /sysSemester/current
     *
     * =========================================================
     */
    @GetMapping("/current")
    public Result<SysSemester> current(
            HttpServletRequest request
    ) {

        if (!isAdmin(request)) {

            return Result.fail(
                    "没有管理员权限"
            );
        }


        SysSemester semester =
                sysSemesterMapper.selectOne(
                        new LambdaQueryWrapper<SysSemester>()
                                .eq(
                                        SysSemester::getStatus,
                                        (short) 1
                                )
                                .orderByDesc(
                                        SysSemester::getStartDate
                                )
                                .last(
                                        "LIMIT 1"
                                )
                );


        return Result.success(
                semester
        );
    }


    /**
     * =========================================================
     * 新增学期
     * =========================================================
     *
     * POST /sysSemester/add
     *
     * 默认：
     *
     * 新建学期自动设置为当前学期。
     *
     * =========================================================
     */
    @PostMapping("/add")
    @Transactional
    public Result<Void> add(
            @RequestBody SysSemester semester,
            HttpServletRequest request
    ) {

        if (!isAdmin(request)) {

            return Result.fail(
                    "没有管理员权限"
            );
        }


        if (semester == null) {

            return Result.fail(
                    "参数不能为空"
            );
        }


        /*
         * =====================================================
         * 1. 检查学期名称
         * =====================================================
         */

        String name =
                semester.getName() == null
                        ? ""
                        : semester.getName().trim();


        if (name.isEmpty()) {

            return Result.fail(
                    "学期名称不能为空"
            );
        }


        /*
         * =====================================================
         * 2. 检查日期
         * =====================================================
         */

        LocalDate startDate =
                semester.getStartDate();

        LocalDate endDate =
                semester.getEndDate();


        if (startDate == null) {

            return Result.fail(
                    "请选择开始日期"
            );
        }


        if (endDate == null) {

            return Result.fail(
                    "请选择结束日期"
            );
        }


        if (endDate.isBefore(startDate)) {

            return Result.fail(
                    "结束日期不能早于开始日期"
            );
        }


        /*
         * =====================================================
         * 3. 检查名称是否重复
         * =====================================================
         */

        Long count =
                sysSemesterMapper.selectCount(
                        new LambdaQueryWrapper<SysSemester>()
                                .eq(
                                        SysSemester::getName,
                                        name
                                )
                );


        if (count != null && count > 0) {

            return Result.fail(
                    "该学期已经存在"
            );
        }


        /*
         * =====================================================
         * 4. 设置基础数据
         * =====================================================
         */

        semester.setName(name);

        semester.setStartDate(
                startDate
        );

        semester.setEndDate(
                endDate
        );


        /*
         * 新增学期默认设置为当前学期
         */
        semester.setStatus(
                (short) 1
        );


        /*
         * =====================================================
         * 5. 先取消原来的当前学期
         * =====================================================
         */

        sysSemesterMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysSemester>()
                        .set(
                                SysSemester::getStatus,
                                (short) 0
                        )
                        .eq(
                                SysSemester::getStatus,
                                (short) 1
                        )
        );


        /*
         * =====================================================
         * 6. 创建新学期
         * =====================================================
         */

        int result =
                sysSemesterMapper.insert(
                        semester
                );


        if (result <= 0) {

            return Result.fail(
                    "学期创建失败"
            );
        }


        return Result.success(
                null
        );
    }


    /**
     * =========================================================
     * 修改学期
     * =========================================================
     *
     * PUT /sysSemester/update/{id}
     *
     * =========================================================
     */
    @PutMapping("/update/{id}")
    @Transactional
    public Result<Void> update(
            @PathVariable Long id,
            @RequestBody SysSemester semester,
            HttpServletRequest request
    ) {

        if (!isAdmin(request)) {

            return Result.fail(
                    "没有管理员权限"
            );
        }


        if (id == null) {

            return Result.fail(
                    "学期 ID 不能为空"
            );
        }


        if (semester == null) {

            return Result.fail(
                    "参数不能为空"
            );
        }


        /*
         * =====================================================
         * 1. 查询原学期
         * =====================================================
         */

        SysSemester oldSemester =
                sysSemesterMapper.selectById(id);


        if (oldSemester == null) {

            return Result.fail(
                    "学期不存在"
            );
        }


        /*
         * =====================================================
         * 2. 检查名称
         * =====================================================
         */

        String name =
                semester.getName() == null
                        ? ""
                        : semester.getName().trim();


        if (name.isEmpty()) {

            return Result.fail(
                    "学期名称不能为空"
            );
        }


        /*
         * =====================================================
         * 3. 检查日期
         * =====================================================
         */

        LocalDate startDate =
                semester.getStartDate();

        LocalDate endDate =
                semester.getEndDate();


        if (startDate == null) {

            return Result.fail(
                    "请选择开始日期"
            );
        }


        if (endDate == null) {

            return Result.fail(
                    "请选择结束日期"
            );
        }


        if (endDate.isBefore(startDate)) {

            return Result.fail(
                    "结束日期不能早于开始日期"
            );
        }


        /*
         * =====================================================
         * 4. 检查名称重复
         * =====================================================
         */

        Long count =
                sysSemesterMapper.selectCount(
                        new LambdaQueryWrapper<SysSemester>()
                                .eq(
                                        SysSemester::getName,
                                        name
                                )
                                .ne(
                                        SysSemester::getId,
                                        id
                                )
                );


        if (count != null && count > 0) {

            return Result.fail(
                    "该学期名称已经存在"
            );
        }


        /*
         * =====================================================
         * 5. 修改
         * =====================================================
         */

        oldSemester.setName(
                name
        );

        oldSemester.setStartDate(
                startDate
        );

        oldSemester.setEndDate(
                endDate
        );


        /*
         * 修改时不允许通过普通编辑改变当前学期状态。
         *
         * 当前状态保持原样。
         */
        sysSemesterMapper.updateById(
                oldSemester
        );


        return Result.success(
                null
        );
    }


    /**
     * =========================================================
     * 设置当前学期
     * =========================================================
     *
     * PUT /sysSemester/set-current/{id}
     *
     * =========================================================
     */
    @PutMapping("/set-current/{id}")
    @Transactional
    public Result<Void> setCurrent(
            @PathVariable Long id,
            HttpServletRequest request
    ) {

        if (!isAdmin(request)) {

            return Result.fail(
                    "没有管理员权限"
            );
        }


        if (id == null) {

            return Result.fail(
                    "学期 ID 不能为空"
            );
        }


        /*
         * =====================================================
         * 1. 查询目标学期
         * =====================================================
         */

        SysSemester semester =
                sysSemesterMapper.selectById(id);


        if (semester == null) {

            return Result.fail(
                    "学期不存在"
            );
        }


        /*
         * =====================================================
         * 2. 取消所有当前学期
         * =====================================================
         */

        sysSemesterMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysSemester>()
                        .set(
                                SysSemester::getStatus,
                                (short) 0
                        )
                        .eq(
                                SysSemester::getStatus,
                                (short) 1
                        )
        );


        /*
         * =====================================================
         * 3. 设置目标学期为当前学期
         * =====================================================
         */

        semester.setStatus(
                (short) 1
        );


        sysSemesterMapper.updateById(
                semester
        );


        return Result.success(
                null
        );
    }
}