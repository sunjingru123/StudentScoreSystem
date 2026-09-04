package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.TeacherAddDTO;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysDepartment;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysDepartmentMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.utils.PasswordUtil;
import com.student.studentscoresystem.vo.TeacherVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/teacher")
public class TeacherManageController {

    private final SysUserMapper sysUserMapper;
    private final SysUserPositionMapper userPositionMapper;
    private final SysPositionMapper positionMapper;
    private final DepartmentMapper departmentMapper;
    private final SysDepartmentMapper sysDepartmentMapper;

    public TeacherManageController(
            SysUserMapper sysUserMapper,
            SysUserPositionMapper userPositionMapper,
            SysPositionMapper positionMapper,
            DepartmentMapper departmentMapper,
            SysDepartmentMapper sysDepartmentMapper
    ) {
        this.sysUserMapper = sysUserMapper;
        this.userPositionMapper = userPositionMapper;
        this.positionMapper = positionMapper;
        this.departmentMapper = departmentMapper;
        this.sysDepartmentMapper = sysDepartmentMapper;
    }

    /**
     * 管理员权限校验
     */
    private boolean isAdmin(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr == null) {
            return false;
        }

        Long userId;
        try {
            userId = Long.valueOf(userIdAttr.toString());
        } catch (Exception e) {
            return false;
        }

        SysPosition adminPosition = positionMapper.selectOne(
                new LambdaQueryWrapper<SysPosition>()
                        .eq(SysPosition::getName, "管理员")
                        .eq(SysPosition::getStatus, (short) 1)
                        .last("LIMIT 1")
        );

        if (adminPosition == null) {
            return false;
        }

        Long count = userPositionMapper.selectCount(
                new LambdaQueryWrapper<SysUserPosition>()
                        .eq(SysUserPosition::getUserId, userId)
                        .eq(SysUserPosition::getPositionId, adminPosition.getId())
        );

        return count != null && count > 0;
    }

    /**
     * 查询辅导员岗位
     */
    private SysPosition getTeacherPosition() {
        return positionMapper.selectOne(
                new LambdaQueryWrapper<SysPosition>()
                        .eq(SysPosition::getName, "辅导员")
                        .eq(SysPosition::getStatus, (short) 1)
                        .last("LIMIT 1")
        );
    }

    /**
     * 获取/创建与业务部门同名的系统部门。
     *
     * 注意：
     * sys_user_position.department_id 外键指向 sys_department，
     * 不能直接填写 department 表的 id。
     */
    private SysDepartment getOrCreateSysDepartment(Department department) {
        SysDepartment sysDepartment = sysDepartmentMapper.selectOne(
                new LambdaQueryWrapper<SysDepartment>()
                        .eq(SysDepartment::getName, department.getName())
                        .last("LIMIT 1")
        );

        if (sysDepartment != null) {
            if (!Short.valueOf((short) 1).equals(sysDepartment.getStatus())) {
                sysDepartment.setStatus((short) 1);
                sysDepartmentMapper.updateById(sysDepartment);
            }
            return sysDepartment;
        }

        sysDepartment = new SysDepartment();
        sysDepartment.setName(department.getName());
        sysDepartment.setStatus((short) 1);
        sysDepartmentMapper.insert(sysDepartment);
        return sysDepartment;
    }

    /**
     * 将 sys_user_position 的系统部门转换为业务 department
     */
    private Department findBusinessDepartment(SysUserPosition relation) {
        if (relation == null || relation.getDepartmentId() == null) {
            return null;
        }

        SysDepartment sysDepartment =
                sysDepartmentMapper.selectById(relation.getDepartmentId());

        if (sysDepartment == null) {
            return null;
        }

        return departmentMapper.selectOne(
                new LambdaQueryWrapper<Department>()
                        .eq(Department::getName, sysDepartment.getName())
                        .last("LIMIT 1")
        );
    }

    /**
     * 查询所有辅导员
     */
    @GetMapping("/list")
    public Result<List<TeacherVO>> list(HttpServletRequest request) {

        if (!isAdmin(request)) {
            return Result.fail("没有管理员权限");
        }

        SysPosition teacherPosition = getTeacherPosition();
        if (teacherPosition == null) {
            return Result.success(new ArrayList<>());
        }

        List<SysUserPosition> relations = userPositionMapper.selectList(
                new LambdaQueryWrapper<SysUserPosition>()
                        .eq(SysUserPosition::getPositionId, teacherPosition.getId())
                        .orderByAsc(SysUserPosition::getId)
        );

        if (relations.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        List<TeacherVO> result = new ArrayList<>();

        for (SysUserPosition relation : relations) {

            SysUser user = sysUserMapper.selectById(relation.getUserId());

            if (user == null) {
                continue;
            }

            TeacherVO vo = new TeacherVO();
            vo.setId(user.getId());
            vo.setRealName(user.getRealName());
            vo.setUsername(user.getUsername());
            vo.setStatus(user.getStatus());

            Department department = findBusinessDepartment(relation);

            if (department != null) {
                vo.setDepartmentId(department.getId());
                vo.setDepartmentName(department.getName());
            }

            result.add(vo);
        }

        return Result.success(result);
    }

    /**
     * 查询业务部门
     */
    @GetMapping("/departments")
    public Result<List<Department>> departments(HttpServletRequest request) {

        if (!isAdmin(request)) {
            return Result.fail("没有管理员权限");
        }

        List<Department> list = departmentMapper.selectList(
                new LambdaQueryWrapper<Department>()
                        .eq(Department::getStatus, (short) 1)
                        .orderByAsc(Department::getId)
        );

        return Result.success(list);
    }

    /**
     * 新增辅导员
     */
    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody TeacherAddDTO dto,
            HttpServletRequest request
    ) {

        if (!isAdmin(request)) {
            return Result.fail("没有管理员权限");
        }

        if (dto == null) {
            return Result.fail("参数不能为空");
        }

        String realName = dto.getRealName() == null
                ? ""
                : dto.getRealName().trim();

        String username = dto.getUsername() == null
                ? ""
                : dto.getUsername().trim();

        String password = dto.getPassword() == null
                ? ""
                : dto.getPassword();

        if (realName.isEmpty()) {
            return Result.fail("老师姓名不能为空");
        }

        if (username.isEmpty()) {
            return Result.fail("账号不能为空");
        }

        if (password.isEmpty()) {
            return Result.fail("密码不能为空");
        }

        if (dto.getDepartmentId() == null) {
            return Result.fail("请选择管理部门");
        }

        Long usernameCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );

        if (usernameCount != null && usernameCount > 0) {
            return Result.fail("账号已经存在");
        }

        Department department = departmentMapper.selectById(dto.getDepartmentId());

        if (department == null
                || !Short.valueOf((short) 1).equals(department.getStatus())) {
            return Result.fail("管理部门不存在");
        }

        // 一个部门只能有一个辅导员
        if (department.getTeacherId() != null) {
            return Result.fail("该部门已经绑定辅导员");
        }

        SysPosition teacherPosition = getTeacherPosition();

        if (teacherPosition == null) {
            return Result.fail("辅导员岗位不存在，请先创建辅导员岗位");
        }

        SysDepartment sysDepartment = getOrCreateSysDepartment(department);

        SysUser user = new SysUser();
        user.setStudentNo("T" + System.currentTimeMillis());
        user.setUsername(username);

        // BCrypt 加密密码
        user.setPassword(PasswordUtil.encode(password));

        user.setRealName(realName);
        user.setStatus((short) 1);

        // 新建教师第一次登录必须修改密码
        user.setFirstLogin((short) 1);

        if (sysUserMapper.insert(user) <= 0) {
            return Result.fail("教师创建失败");
        }

        SysUserPosition relation = new SysUserPosition();
        relation.setUserId(user.getId());
        relation.setDepartmentId(sysDepartment.getId());
        relation.setPositionId(teacherPosition.getId());

        userPositionMapper.insert(relation);

        department.setTeacherId(user.getId());
        departmentMapper.updateById(department);

        return Result.success(null);
    }

    /**
     * 修改辅导员
     */
    @PutMapping("/update/{id}")
    public Result<Void> update(
            @PathVariable Long id,
            @RequestBody TeacherAddDTO dto,
            HttpServletRequest request
    ) {

        if (!isAdmin(request)) {
            return Result.fail("没有管理员权限");
        }

        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            return Result.fail("教师不存在");
        }

        if (dto == null) {
            return Result.fail("参数不能为空");
        }

        String realName = dto.getRealName() == null
                ? ""
                : dto.getRealName().trim();

        String username = dto.getUsername() == null
                ? ""
                : dto.getUsername().trim();

        if (realName.isEmpty()) {
            return Result.fail("老师姓名不能为空");
        }

        if (username.isEmpty()) {
            return Result.fail("账号不能为空");
        }

        Long usernameCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .ne(SysUser::getId, id)
        );

        if (usernameCount != null && usernameCount > 0) {
            return Result.fail("账号已经存在");
        }

        if (dto.getDepartmentId() == null) {
            return Result.fail("请选择管理部门");
        }

        Department newDepartment =
                departmentMapper.selectById(dto.getDepartmentId());

        if (newDepartment == null
                || !Short.valueOf((short) 1).equals(newDepartment.getStatus())) {
            return Result.fail("管理部门不存在");
        }

        // 找到原教师岗位关系
        SysPosition teacherPosition = getTeacherPosition();
        if (teacherPosition == null) {
            return Result.fail("辅导员岗位不存在");
        }

        SysUserPosition relation = userPositionMapper.selectOne(
                new LambdaQueryWrapper<SysUserPosition>()
                        .eq(SysUserPosition::getUserId, id)
                        .eq(SysUserPosition::getPositionId, teacherPosition.getId())
                        .last("LIMIT 1")
        );

        Department oldDepartment = findBusinessDepartment(relation);

        // 换部门时，新部门不能已经有其他辅导员
        if (newDepartment.getTeacherId() != null
                && !newDepartment.getTeacherId().equals(id)) {
            return Result.fail("该部门已经绑定其他辅导员");
        }

        user.setRealName(realName);
        user.setUsername(username);

        // 编辑时密码为空代表不修改密码
        if (dto.getPassword() != null
                && !dto.getPassword().isEmpty()) {

            user.setPassword(PasswordUtil.encode(dto.getPassword()));

            // 管理员重新设置密码后，要求教师下次登录修改
            user.setFirstLogin((short) 1);
        }

        sysUserMapper.updateById(user);

        SysDepartment newSysDepartment =
                getOrCreateSysDepartment(newDepartment);

        if (relation == null) {
            relation = new SysUserPosition();
            relation.setUserId(id);
            relation.setPositionId(teacherPosition.getId());
            relation.setDepartmentId(newSysDepartment.getId());
            userPositionMapper.insert(relation);
        } else {
            relation.setDepartmentId(newSysDepartment.getId());
            userPositionMapper.updateById(relation);
        }

        if (oldDepartment != null
                && !oldDepartment.getId().equals(newDepartment.getId())
                && id.equals(oldDepartment.getTeacherId())) {

            oldDepartment.setTeacherId(null);
            departmentMapper.updateById(oldDepartment);
        }

        newDepartment.setTeacherId(id);
        departmentMapper.updateById(newDepartment);

        return Result.success(null);
    }

    /**
     * 停用教师
     */
    @PutMapping("/disable/{id}")
    public Result<Void> disable(
            @PathVariable Long id,
            HttpServletRequest request
    ) {

        if (!isAdmin(request)) {
            return Result.fail("没有管理员权限");
        }

        SysUser user = sysUserMapper.selectById(id);

        if (user == null) {
            return Result.fail("教师不存在");
        }

        user.setStatus((short) 0);
        sysUserMapper.updateById(user);

        return Result.success(null);
    }

    /**
     * 启用教师
     */
    @PutMapping("/enable/{id}")
    public Result<Void> enable(
            @PathVariable Long id,
            HttpServletRequest request
    ) {

        if (!isAdmin(request)) {
            return Result.fail("没有管理员权限");
        }

        SysUser user = sysUserMapper.selectById(id);

        if (user == null) {
            return Result.fail("教师不存在");
        }

        user.setStatus((short) 1);
        sysUserMapper.updateById(user);

        return Result.success(null);
    }

    /**
     * 重置密码
     */
    @PutMapping("/reset-password/{id}")
    public Result<Void> resetPassword(
            @PathVariable Long id,
            HttpServletRequest request
    ) {

        if (!isAdmin(request)) {
            return Result.fail("没有管理员权限");
        }

        SysUser user = sysUserMapper.selectById(id);

        if (user == null) {
            return Result.fail("教师不存在");
        }

        // 默认密码 123456，数据库中保存 BCrypt 密文
        user.setPassword(PasswordUtil.encode("123456"));

        // 重置密码后，教师第一次登录必须修改密码
        user.setFirstLogin((short) 1);

        sysUserMapper.updateById(user);

        return Result.success(null);
    }
}