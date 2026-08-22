package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/student-department")
public class StudentDepartmentController {

    private final SysUserDepartmentMapper userDepartmentMapper;

    private final SysUserMapper userMapper;

    private final DepartmentMapper departmentMapper;

    public StudentDepartmentController(
            SysUserDepartmentMapper userDepartmentMapper,
            SysUserMapper userMapper,
            DepartmentMapper departmentMapper
    ) {
        this.userDepartmentMapper = userDepartmentMapper;
        this.userMapper = userMapper;
        this.departmentMapper = departmentMapper;
    }

    /**
     * 给学生加入部门
     */
    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody SysUserDepartment relation
    ) {

        SysUser user =
                userMapper.selectById(
                        relation.getUserId()
                );

        if (user == null) {
            return Result.error("学生不存在");
        }

        Department department =
                departmentMapper.selectById(
                        relation.getDepartmentId()
                );

        if (department == null) {
            return Result.error("部门不存在");
        }

        SysUserDepartment exist =
                userDepartmentMapper.selectOne(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getUserId,
                                        relation.getUserId()
                                )
                                .eq(
                                        SysUserDepartment::getDepartmentId,
                                        relation.getDepartmentId()
                                )
                );

        if (exist != null) {
            return Result.error("学生已经加入该部门");
        }

        if (relation.getPosition() == null
                || relation.getPosition().trim().isEmpty()) {

            relation.setPosition("干事");
        }

        relation.setStatus((short) 1);

        userDepartmentMapper.insert(relation);

        return Result.success(null);
    }

    /**
     * 查询学生所属部门
     */
    @GetMapping("/student/{studentId}")
    public Result<List<SysUserDepartment>> studentDepartments(
            @PathVariable Long studentId
    ) {

        List<SysUserDepartment> list =
                userDepartmentMapper.selectList(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getUserId,
                                        studentId
                                )
                                .eq(
                                        SysUserDepartment::getStatus,
                                        (short) 1
                                )
                );

        return Result.success(list);
    }

    /**
     * 修改学生在部门中的职位
     */
    @PutMapping("/{id}/position")
    public Result<Void> updatePosition(
            @PathVariable Long id,
            @RequestParam String position
    ) {

        SysUserDepartment relation =
                userDepartmentMapper.selectById(id);

        if (relation == null) {
            return Result.error("部门关系不存在");
        }

        if (!position.equals("干事")
                && !position.equals("副部长")
                && !position.equals("部长")) {

            return Result.error("职位不合法");
        }

        relation.setPosition(position);

        userDepartmentMapper.updateById(relation);

        return Result.success(null);
    }

    /**
     * 退出部门
     */
    @PutMapping("/{id}/leave")
    public Result<Void> leave(
            @PathVariable Long id
    ) {

        SysUserDepartment relation =
                userDepartmentMapper.selectById(id);

        if (relation == null) {
            return Result.error("部门关系不存在");
        }

        relation.setStatus((short) 0);

        userDepartmentMapper.updateById(relation);

        return Result.success(null);
    }
}