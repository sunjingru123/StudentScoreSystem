package com.student.studentscoresystem;

import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.controller.TeacherManageController;
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
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * =========================================================
 * 「编辑教师」不得改动密码与首次登录状态的回归测试
 * =========================================================
 *
 * 现象：
 * 管理员只修改辅导员的管理部门，
 * 教师下次登录却被要求「首次登录，请修改密码」。
 *
 * 规则：
 * 编辑教师只更新姓名 / 账号 / 岗位 / 负责部门，
 * 密码与 first_login 保持不变；
 * 修改密码只能走「重置密码」接口。
 * =========================================================
 */
class TeacherManageControllerUpdateCredentialTest {

    private static final long ADMIN_ID = 1L;
    private static final long TEACHER_ID = 9L;
    private static final long DEPT_ID = 1L;

    /**
     * 数据库中已有的 BCrypt 密文
     */
    private static final String EXISTING_HASH =
            "$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMN";

    private SysUserMapper sysUserMapper;
    private SysUserPositionMapper userPositionMapper;
    private SysPositionMapper positionMapper;
    private DepartmentMapper departmentMapper;
    private SysDepartmentMapper sysDepartmentMapper;
    private HttpServletRequest request;

    private TeacherManageController controller;

    @BeforeEach
    void setUp() {

        sysUserMapper = mock(SysUserMapper.class);
        userPositionMapper = mock(SysUserPositionMapper.class);
        positionMapper = mock(SysPositionMapper.class);
        departmentMapper = mock(DepartmentMapper.class);
        sysDepartmentMapper = mock(SysDepartmentMapper.class);
        request = mock(HttpServletRequest.class);

        controller = new TeacherManageController(
                sysUserMapper,
                userPositionMapper,
                positionMapper,
                departmentMapper,
                sysDepartmentMapper
        );
    }

    /**
     * 不传密码：密码与首次登录状态都必须原样保留。
     */
    @Test
    void updateShouldKeepPasswordWhenPasswordIsAbsent() {

        stubCommon();

        TeacherAddDTO dto = dto(null);

        Result<Void> result = controller.update(TEACHER_ID, dto, request);

        assertEquals(200, result.getCode(), result.getMessage());

        SysUser saved = captureSavedUser();

        assertEquals(EXISTING_HASH, saved.getPassword());
        assertEquals((short) 0, saved.getFirstLogin());
    }

    /**
     * 即使请求误带密码：编辑接口也不改密码、不改首次登录状态。
     *
     * 防止旧版前端 / 浏览器缓存继续携带默认密码 123456，
     * 把管理员「只改部门」误变成「重置密码 + 强制改密」。
     */
    @Test
    void updateShouldIgnorePasswordPayload() {

        stubCommon();

        TeacherAddDTO dto = dto("123456");

        Result<Void> result = controller.update(TEACHER_ID, dto, request);

        assertEquals(200, result.getCode(), result.getMessage());

        SysUser saved = captureSavedUser();

        assertEquals(
                EXISTING_HASH,
                saved.getPassword(),
                "编辑教师不允许修改密码，密码只能通过「重置密码」修改"
        );

        assertEquals(
                (short) 0,
                saved.getFirstLogin(),
                "编辑教师不允许把 first_login 改回 1"
        );
    }

    // =========================================================
    // helpers
    // =========================================================

    private void stubCommon() {

        when(request.getAttribute("userId")).thenReturn(ADMIN_ID);

        when(positionMapper.selectOne(any()))
                .thenReturn(
                        position(100L, "管理员"),
                        position(200L, "辅导员")
                );

        when(userPositionMapper.selectCount(any())).thenReturn(1L);

        SysUser user = new SysUser();
        user.setId(TEACHER_ID);
        user.setUsername("sunjingru");
        user.setRealName("孙老师");
        user.setPassword(EXISTING_HASH);
        user.setFirstLogin((short) 0);
        user.setStatus((short) 1);

        when(sysUserMapper.selectById(TEACHER_ID)).thenReturn(user);

        // 账号不重复
        when(sysUserMapper.selectCount(any())).thenReturn(0L);

        when(departmentMapper.selectById(DEPT_ID))
                .thenReturn(department(DEPT_ID, "档案部"));

        when(sysDepartmentMapper.selectOne(any()))
                .thenReturn(sysDepartment(10L, "档案部"));

        SysUserPosition relation = new SysUserPosition();
        relation.setId(5L);
        relation.setUserId(TEACHER_ID);

        when(userPositionMapper.selectOne(any())).thenReturn(relation);

        // 当前没有已绑定部门，避免解绑分支干扰
        when(departmentMapper.selectList(any()))
                .thenReturn(new ArrayList<>());
    }

    private SysUser captureSavedUser() {

        ArgumentCaptor<SysUser> captor =
                ArgumentCaptor.forClass(SysUser.class);

        verify(sysUserMapper).updateById(captor.capture());

        SysUser saved = captor.getValue();

        assertNotNull(saved);

        return saved;
    }

    private TeacherAddDTO dto(String password) {

        TeacherAddDTO dto = new TeacherAddDTO();
        dto.setRealName("孙老师");
        dto.setUsername("sunjingru");
        dto.setPositionName("辅导员");
        dto.setDepartmentIds(List.of(DEPT_ID));
        dto.setPassword(password);

        return dto;
    }

    private SysPosition position(Long id, String name) {

        SysPosition position = new SysPosition();
        position.setId(id);
        position.setName(name);
        position.setStatus((short) 1);

        return position;
    }

    private Department department(Long id, String name) {

        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setStatus((short) 1);

        return department;
    }

    private SysDepartment sysDepartment(Long id, String name) {

        SysDepartment sysDepartment = new SysDepartment();
        sysDepartment.setId(id);
        sysDepartment.setName(name);
        sysDepartment.setStatus((short) 1);

        return sysDepartment;
    }
}
