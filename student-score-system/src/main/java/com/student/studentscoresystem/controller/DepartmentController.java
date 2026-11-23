package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.annotation.RequireRole;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysDepartment;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysDepartmentMapper;
import com.student.studentscoresystem.utils.OrgPositionLevel;
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

        /*
         * 组织类型与父子关系校验
         */
        short departmentType =
                normalizeType(
                        department.getDepartmentType()
                );

        Long parentId = null;

        if (departmentType == OrgPositionLevel.TYPE_SMALL_CLUB) {

            String parentError =
                    validateParent(
                            department.getParentId(),
                            null
                    );

            if (parentError != null) {
                return Result.error(parentError);
            }

            parentId = department.getParentId();
        }

        department.setName(name);
        department.setTeacherId(null);
        department.setDepartmentType(departmentType);
        department.setParentId(parentId);
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

        /*
         * 调用方未传类型时保持原类型，
         * 避免把大社团 / 小社团误改回普通部门。
         */
        short departmentType =
                department.getDepartmentType() == null
                        && old.getDepartmentType() != null
                        ? old.getDepartmentType()
                        : normalizeType(department.getDepartmentType());

        Long parentId = null;

        if (departmentType == OrgPositionLevel.TYPE_SMALL_CLUB) {

            String parentError =
                    validateParent(
                            department.getParentId(),
                            id
                    );

            if (parentError != null) {
                return Result.error(parentError);
            }

            parentId = department.getParentId();
        }

        /*
         * 大社团下面还有小社团时，不能改成其它类型，
         * 否则会断掉父子关系。
         */
        if (old.getDepartmentType() != null
                && old.getDepartmentType() == OrgPositionLevel.TYPE_BIG_CLUB
                && departmentType != OrgPositionLevel.TYPE_BIG_CLUB) {

            Long childCount =
                    departmentMapper.selectCount(
                            new LambdaQueryWrapper<Department>()
                                    .eq(
                                            Department::getParentId,
                                            id
                                    )
                    );

            if (childCount != null && childCount > 0) {

                return Result.error(
                        "该大社团下还有小社团，不能修改组织类型"
                );
            }
        }

        old.setName(name);
        old.setDepartmentType(departmentType);
        old.setParentId(parentId);

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

    /**
     * 组织类型兜底：默认为普通部门
     */
    private short normalizeType(
            Short departmentType
    ) {

        if (departmentType == null) {

            return OrgPositionLevel.TYPE_DEPARTMENT;
        }

        if (departmentType != OrgPositionLevel.TYPE_DEPARTMENT
                && departmentType != OrgPositionLevel.TYPE_BIG_CLUB
                && departmentType != OrgPositionLevel.TYPE_SMALL_CLUB) {

            return OrgPositionLevel.TYPE_DEPARTMENT;
        }

        return departmentType;
    }

    /**
     * 校验小社团的上级组织
     *
     * @param parentId    上级组织 ID
     * @param currentId   当前组织 ID（新增时为 null）
     * @return 错误信息，校验通过返回 null
     */
    private String validateParent(
            Long parentId,
            Long currentId
    ) {

        if (parentId == null) {

            return "小社团必须选择所属大社团";
        }

        if (currentId != null && parentId.equals(currentId)) {

            return "上级组织不能是自身";
        }

        Department parent =
                departmentMapper.selectById(parentId);

        if (parent == null) {

            return "所属大社团不存在";
        }

        if (parent.getDepartmentType() == null
                || parent.getDepartmentType() != OrgPositionLevel.TYPE_BIG_CLUB) {

            return "所属大社团必须是「大社团」类型的组织";
        }

        return null;
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
