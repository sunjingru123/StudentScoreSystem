package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/teacher")
public class TeacherManageController {

    /**
     * 普通辅导员岗位名称
     */
    private static final String TEACHER_POSITION_NAME = "辅导员";

    /**
     * 高级辅导员岗位名称
     */
    private static final String SENIOR_COUNSELOR_POSITION_NAME = "高级辅导员";

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
     * 查询指定名称的启用岗位
     */
    private SysPosition getPositionByName(String name) {

        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        return positionMapper.selectOne(
                new LambdaQueryWrapper<SysPosition>()
                        .eq(SysPosition::getName, name.trim())
                        .eq(SysPosition::getStatus, (short) 1)
                        .last("LIMIT 1")
        );
    }

    /**
     * 规范化岗位名称，缺省为普通辅导员
     */
    private String normalizePositionName(String positionName) {

        if (positionName == null || positionName.trim().isEmpty()) {
            return TEACHER_POSITION_NAME;
        }

        return positionName.trim();
    }

    /**
     * 获取/创建独立占位的系统部门。
     *
     * 高级辅导员不负责具体业务部门，但 sys_user_position.department_id
     * 不允许为空，因此使用一个专用占位部门维持外键完整性。
     */
    private SysDepartment getOrCreateStandaloneSysDepartment(String name) {

        SysDepartment sysDepartment = sysDepartmentMapper.selectOne(
                new LambdaQueryWrapper<SysDepartment>()
                        .eq(SysDepartment::getName, name)
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
        sysDepartment.setName(name);
        sysDepartment.setStatus((short) 1);
        sysDepartmentMapper.insert(sysDepartment);

        return sysDepartment;
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
     * 解析前端提交的管理部门：
     *
     * departmentIds 优先，为空时回退到旧的单部门字段 departmentId。
     * 自动去重并过滤空值。
     */
    private List<Long> resolveDepartmentIds(TeacherAddDTO dto) {

        List<Long> result = new ArrayList<>();

        if (dto == null) {
            return result;
        }

        List<Long> source = dto.getDepartmentIds();

        if ((source == null || source.isEmpty())
                && dto.getDepartmentId() != null) {

            source = List.of(dto.getDepartmentId());
        }

        if (source == null) {
            return result;
        }

        for (Long departmentId : source) {

            if (departmentId != null && !result.contains(departmentId)) {

                result.add(departmentId);
            }
        }

        return result;
    }

    /**
     * 查询启用状态的业务部门，不存在或已停用返回 null。
     */
    private Department loadEnabledDepartment(Long departmentId) {

        if (departmentId == null) {
            return null;
        }

        Department department = departmentMapper.selectById(departmentId);

        if (department == null
                || !Short.valueOf((short) 1).equals(department.getStatus())) {

            return null;
        }

        return department;
    }

    /**
     * 查询所有辅导员（含高级辅导员）
     */
    @GetMapping("/list")
    public Result<List<TeacherVO>> list(HttpServletRequest request) {

        if (!isAdmin(request)) {
            return Result.fail("没有管理员权限");
        }

        List<SysPosition> positions = new ArrayList<>();

        SysPosition teacherPosition = getPositionByName(TEACHER_POSITION_NAME);

        if (teacherPosition != null) {
            positions.add(teacherPosition);
        }

        SysPosition seniorPosition = getPositionByName(SENIOR_COUNSELOR_POSITION_NAME);

        if (seniorPosition != null
                && (teacherPosition == null
                    || !seniorPosition.getId().equals(teacherPosition.getId()))) {
            positions.add(seniorPosition);
        }

        if (positions.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        Map<Long, String> positionNames = new HashMap<>();

        List<Long> positionIds = new ArrayList<>();

        for (SysPosition position : positions) {
            positionNames.put(position.getId(), position.getName());
            positionIds.add(position.getId());
        }

        List<SysUserPosition> relations = userPositionMapper.selectList(
                new LambdaQueryWrapper<SysUserPosition>()
                        .in(SysUserPosition::getPositionId, positionIds)
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
            vo.setPositionName(positionNames.get(relation.getPositionId()));

            /*
             * 辅导员可以同时分管多个部门：
             * department.teacher_id = 教师ID 的所有部门都是其负责部门。
             */
            List<Department> managedDepartments = departmentMapper.selectList(
                    new LambdaQueryWrapper<Department>()
                            .eq(Department::getTeacherId, user.getId())
                            .orderByAsc(Department::getId)
            );

            List<Long> managedIds = new ArrayList<>();
            List<String> managedNames = new ArrayList<>();

            if (managedDepartments != null) {

                for (Department department : managedDepartments) {

                    if (department == null) {
                        continue;
                    }

                    managedIds.add(department.getId());
                    managedNames.add(department.getName());
                }
            }

            /*
             * 兼容旧数据：未占用 department.teacher_id 时，
             * 回退到 sys_user_position 关联的部门。
             */
            if (managedNames.isEmpty()) {

                Department fallback = findBusinessDepartment(relation);

                if (fallback != null) {
                    managedIds.add(fallback.getId());
                    managedNames.add(fallback.getName());
                }
            }

            vo.setDepartmentIds(managedIds);
            vo.setDepartmentNames(managedNames);

            if (!managedIds.isEmpty()) {
                vo.setDepartmentId(managedIds.get(0));
                vo.setDepartmentName(managedNames.get(0));
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

        Long usernameCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );

        if (usernameCount != null && usernameCount > 0) {
            return Result.fail("账号已经存在");
        }

        String positionName = normalizePositionName(dto.getPositionName());

        boolean seniorCounselor =
                SENIOR_COUNSELOR_POSITION_NAME.equals(positionName);

        SysPosition position = getPositionByName(positionName);

        if (position == null) {
            return Result.fail(positionName + "岗位不存在，请先创建该岗位");
        }

        List<Long> departmentIds = resolveDepartmentIds(dto);

        List<Department> departments = new ArrayList<>();

        if (seniorCounselor) {

            /*
             * 高级辅导员不绑定 department.teacher_id，
             * 也不占用「一个部门一个辅导员」的名额；
             * 管理部门为可选项。
             */
            if (!departmentIds.isEmpty()) {

                Department department =
                        loadEnabledDepartment(departmentIds.get(0));

                if (department == null) {
                    return Result.fail("管理部门不存在");
                }

                departments.add(department);
            }

        } else {

            if (departmentIds.isEmpty()) {
                return Result.fail("请选择管理部门");
            }

            for (Long departmentId : departmentIds) {

                Department department =
                        loadEnabledDepartment(departmentId);

                if (department == null) {
                    return Result.fail("管理部门不存在");
                }

                // 一个部门只能有一个辅导员
                if (department.getTeacherId() != null) {
                    return Result.fail(
                            "部门「" + department.getName() + "」已经绑定辅导员"
                    );
                }

                departments.add(department);
            }
        }

        SysDepartment sysDepartment = departments.isEmpty()
                ? getOrCreateStandaloneSysDepartment(SENIOR_COUNSELOR_POSITION_NAME)
                : getOrCreateSysDepartment(departments.get(0));

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
        relation.setPositionId(position.getId());

        userPositionMapper.insert(relation);

        /*
         * 辅导员可以同时分管多个部门：
         * 把本次选择的部门全部绑定到该教师。
         */
        if (!seniorCounselor) {

            for (Department department : departments) {

                department.setTeacherId(user.getId());
                departmentMapper.updateById(department);
            }
        }

        return Result.success(null);
    }

    /**
     * 修改辅导员
     */
    @PutMapping("/update/{id}")
    @Transactional(rollbackFor = Exception.class)
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

        String positionName = normalizePositionName(dto.getPositionName());

        boolean seniorCounselor =
                SENIOR_COUNSELOR_POSITION_NAME.equals(positionName);

        SysPosition position = getPositionByName(positionName);

        if (position == null) {
            return Result.fail(positionName + "岗位不存在");
        }

        List<Long> departmentIds = resolveDepartmentIds(dto);

        List<Department> newDepartments = new ArrayList<>();

        if (seniorCounselor) {

            if (!departmentIds.isEmpty()) {

                Department department =
                        loadEnabledDepartment(departmentIds.get(0));

                if (department == null) {
                    return Result.fail("管理部门不存在");
                }

                newDepartments.add(department);
            }

        } else {

            if (departmentIds.isEmpty()) {
                return Result.fail("请选择管理部门");
            }

            for (Long departmentId : departmentIds) {

                Department department =
                        loadEnabledDepartment(departmentId);

                if (department == null) {
                    return Result.fail("管理部门不存在");
                }

                // 部门不能已经绑定其他辅导员
                if (department.getTeacherId() != null
                        && !department.getTeacherId().equals(id)) {

                    return Result.fail(
                            "部门「" + department.getName() + "」已经绑定其他辅导员"
                    );
                }

                newDepartments.add(department);
            }
        }

        SysDepartment newSysDepartment = newDepartments.isEmpty()
                ? getOrCreateStandaloneSysDepartment(SENIOR_COUNSELOR_POSITION_NAME)
                : getOrCreateSysDepartment(newDepartments.get(0));

        // 一个账号只挂一个岗位，按用户维度查找原有岗位关系
        SysUserPosition relation = userPositionMapper.selectOne(
                new LambdaQueryWrapper<SysUserPosition>()
                        .eq(SysUserPosition::getUserId, id)
                        .last("LIMIT 1")
        );

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

        if (relation == null) {
            relation = new SysUserPosition();
            relation.setUserId(id);
            relation.setPositionId(position.getId());
            relation.setDepartmentId(newSysDepartment.getId());
            userPositionMapper.insert(relation);
        } else {
            relation.setDepartmentId(newSysDepartment.getId());
            relation.setPositionId(position.getId());
            userPositionMapper.updateById(relation);
        }

        /*
         * 重新绑定「该教师负责的部门」：
         *
         * 1. 先解绑不再负责的部门
         *    （原负责部门不在本次选择内，或切换为高级辅导员）；
         * 2. 再绑定本次选择的部门。
         *
         * 辅导员可以同时分管多个部门，
         * 因此不再依赖单个 oldDepartment / newDepartment。
         */
        List<Department> selectedDepartments = new ArrayList<>();

        if (!seniorCounselor) {
            selectedDepartments.addAll(newDepartments);
        }

        List<Department> boundDepartments = departmentMapper.selectList(
                new LambdaQueryWrapper<Department>()
                        .eq(Department::getTeacherId, id)
        );

        if (boundDepartments != null) {

            for (Department boundDepartment : boundDepartments) {

                if (boundDepartment == null) {
                    continue;
                }

                boolean keep = false;

                for (Department selected : selectedDepartments) {

                    if (selected.getId() != null
                            && selected.getId().equals(boundDepartment.getId())) {

                        keep = true;
                        break;
                    }
                }

                if (!keep) {
                    /*
                     * 注意：MyBatis-Plus 默认 update-strategy 为 NOT_NULL，
                     * updateById 会忽略值为 null 的字段，
                     * 直接 setTeacherId(null) 后 updateById 不会生成
                     * teacher_id = NULL 的 SQL，解绑会静默失败。
                     * 因此这里必须使用 UpdateWrapper 显式置空。
                     */
                    if (boundDepartment.getId() != null) {
                        departmentMapper.update(
                                null,
                                new LambdaUpdateWrapper<Department>()
                                        .eq(Department::getId, boundDepartment.getId())
                                        .set(Department::getTeacherId, null)
                        );
                    }
                }
            }
        }

        // 高级辅导员不占用 department.teacher_id
        for (Department selected : selectedDepartments) {
            selected.setTeacherId(id);
            departmentMapper.updateById(selected);
        }

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