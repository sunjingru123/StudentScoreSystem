package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.annotation.RequireRole;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysDepartment;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysDepartmentMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/department")
public class DepartmentController {

    private final DepartmentMapper departmentMapper;

    private final SysDepartmentMapper sysDepartmentMapper;

    public DepartmentController(
            DepartmentMapper departmentMapper,
            SysDepartmentMapper sysDepartmentMapper
    ) {
        this.departmentMapper = departmentMapper;
        this.sysDepartmentMapper = sysDepartmentMapper;
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
     * 获取全部部门（管理员管理页使用，包含已停用部门）
     */
    @GetMapping("/manage-list")
    @RequireRole("管理员")
    public Result<List<Department>> manageList() {

        List<Department> list =
                departmentMapper.selectList(
                        new LambdaQueryWrapper<Department>()
                                .orderByDesc(Department::getStatus)
                                .orderByAsc(Department::getId)
                );

        return Result.success(list);
    }

    /**
     * 新增部门
     */
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    @RequireRole("管理员")
    public Result<Void> add(
            @RequestBody Department department
    ) {

        String name = normalizeName(department.getName());

        if (name == null) {
            return Result.error("部门名称不能为空");
        }

        Department exist =
                departmentMapper.selectOne(
                        new LambdaQueryWrapper<Department>()
                                .eq(
                                        Department::getName,
                                        name
                                )
                );

        if (exist != null) {
            return Result.error("部门已经存在");
        }

        department.setName(name);
        department.setTeacherId(null);
        department.setStatus((short) 1);
        department.setCreateTime(LocalDateTime.now());

        if (departmentMapper.insert(department) <= 0) {
            return Result.error("部门创建失败");
        }

        syncSysDepartment(null, name, (short) 1);

        return Result.success(null);
    }

    /**
     * 修改部门
     */
    @PutMapping("/{id}")
    @RequireRole("管理员")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(
            @PathVariable Long id,
            @RequestBody Department department
    ) {

        Department old =
                departmentMapper.selectById(id);

        if (old == null) {
            return Result.error("部门不存在");
        }

        String name = normalizeName(department.getName());

        if (name == null) {
            return Result.error("部门名称不能为空");
        }

        Long duplicateCount = departmentMapper.selectCount(
                new LambdaQueryWrapper<Department>()
                        .eq(Department::getName, name)
                        .ne(Department::getId, id)
        );

        if (duplicateCount != null && duplicateCount > 0) {
            return Result.error("部门名称已经存在");
        }

        String oldName = old.getName();
        old.setName(name);

        if (departmentMapper.updateById(old) <= 0) {
            return Result.error("部门修改失败");
        }

        syncSysDepartment(oldName, name, old.getStatus());

        return Result.success(null);
    }

    /**
     * 停用部门
     */
    @PutMapping("/{id}/disable")
    @RequireRole("管理员")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> disable(
            @PathVariable Long id
    ) {

        Department department =
                departmentMapper.selectById(id);

        if (department == null) {
            return Result.error("部门不存在");
        }

        department.setStatus((short) 0);

        if (departmentMapper.updateById(department) <= 0) {
            return Result.error("部门停用失败");
        }

        syncSysDepartment(department.getName(), department.getName(), (short) 0);

        return Result.success(null);
    }

    /**
     * 启用部门
     */
    @PutMapping("/{id}/enable")
    @RequireRole("管理员")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> enable(@PathVariable Long id) {

        Department department = departmentMapper.selectById(id);

        if (department == null) {
            return Result.error("部门不存在");
        }

        department.setStatus((short) 1);

        if (departmentMapper.updateById(department) <= 0) {
            return Result.error("部门启用失败");
        }

        syncSysDepartment(department.getName(), department.getName(), (short) 1);

        return Result.success(null);
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return name.trim();
    }

    private void syncSysDepartment(
            String oldName,
            String newName,
            Short status
    ) {
        String queryName = oldName == null ? newName : oldName;
        SysDepartment sysDepartment = sysDepartmentMapper.selectOne(
                new LambdaQueryWrapper<SysDepartment>()
                        .eq(SysDepartment::getName, queryName)
                        .last("LIMIT 1")
        );

        LocalDateTime now = LocalDateTime.now();

        if (sysDepartment == null) {
            sysDepartment = new SysDepartment();
            sysDepartment.setName(newName);
            sysDepartment.setDescription("由部门管理创建");
            sysDepartment.setStatus(status);
            sysDepartment.setCreateTime(now);
            sysDepartment.setUpdateTime(now);
            sysDepartmentMapper.insert(sysDepartment);
            return;
        }

        sysDepartment.setName(newName);
        sysDepartment.setStatus(status);
        sysDepartment.setUpdateTime(now);
        sysDepartmentMapper.updateById(sysDepartment);
    }
}
