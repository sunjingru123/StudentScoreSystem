package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/department")
public class DepartmentController {

    private final DepartmentMapper departmentMapper;

    private final SysUserMapper sysUserMapper;

    public DepartmentController(
            DepartmentMapper departmentMapper,
            SysUserMapper sysUserMapper
    ) {
        this.departmentMapper = departmentMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 获取所有正常部门
     */
    @GetMapping("/list")
    public Result<List<Department>> list() {

        List<Department> list =
                departmentMapper.selectList(
                        new LambdaQueryWrapper<Department>()
                                .eq(
                                        Department::getStatus,
                                        (short) 1
                                )
                                .orderByAsc(
                                        Department::getId
                                )
                );

        return Result.success(list);
    }

    /**
     * 新增部门
     */
    @PostMapping
    public Result<Void> add(
            @RequestBody Department department
    ) {

        if (department.getName() == null
                || department.getName().trim().isEmpty()) {

            return Result.error("部门名称不能为空");
        }

        Department exist =
                departmentMapper.selectOne(
                        new LambdaQueryWrapper<Department>()
                                .eq(
                                        Department::getName,
                                        department.getName()
                                )
                );

        if (exist != null) {
            return Result.error("部门已经存在");
        }

        department.setStatus((short) 1);

        departmentMapper.insert(department);

        return Result.success(null);
    }

    /**
     * 修改部门
     */
    @PutMapping("/{id}")
    public Result<Void> update(
            @PathVariable Long id,
            @RequestBody Department department
    ) {

        Department old =
                departmentMapper.selectById(id);

        if (old == null) {
            return Result.error("部门不存在");
        }

        old.setName(department.getName());

        old.setTeacherId(
                department.getTeacherId()
        );

        departmentMapper.updateById(old);

        return Result.success(null);
    }

    /**
     * 停用部门
     */
    @PutMapping("/{id}/disable")
    public Result<Void> disable(
            @PathVariable Long id
    ) {

        Department department =
                departmentMapper.selectById(id);

        if (department == null) {
            return Result.error("部门不存在");
        }

        department.setStatus((short) 0);

        departmentMapper.updateById(department);

        return Result.success(null);
    }
}