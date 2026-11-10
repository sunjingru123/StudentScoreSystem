package com.student.studentscoresystem;

import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.controller.TeacherManageController;
import com.student.studentscoresystem.dto.TeacherAddDTO;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysDepartment;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysDepartmentMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * =========================================================
 * 新增教师：账号自动生成回归测试
 * =========================================================
 *
 * 管理员新增教师时不填写账号，
 * 后端根据姓名全拼生成登录账号：
 *
 *   孙靖茹 -> sunjingru
 *
 * 同名时追加序号，避免主键/唯一性冲突。
 * =========================================================
 */
class TeacherManageControllerAddUsernameTest {

    private static final long ADMIN_ID = 1L;
    private static final long DEPT_ID = 1L;

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
     * 账号必须由姓名全拼生成，并且忽略前端传入的账号。
     */
    @Test
    void addShouldGenerateUsernameFromRealNamePinyin() {

        stubAdminAndPositions(position(200L, "辅导员"));

        when(sysUserMapper.insert(any(SysUser.class))).thenReturn(1);

        // 账号不存在
        when(sysUserMapper.selectCount(any())).thenReturn(0L);

        when(departmentMapper.selectById(DEPT_ID))
                .thenReturn(department(DEPT_ID, "档案部", null));

        when(sysDepartmentMapper.selectOne(any()))
                .thenReturn(sysDepartment(10L, "档案部"));

        TeacherAddDTO dto = new TeacherAddDTO();
        dto.setRealName("孙靖茹");
        dto.setPositionName("辅导员");
        dto.setDepartmentIds(List.of(DEPT_ID));
        dto.setUsername("随便填的账号");
        dto.setPassword("123456");

        Result<String> result = controller.add(dto, request);

        assertEquals(200, result.getCode(), result.getMessage());
        assertEquals("sunjingru", result.getData());

        ArgumentCaptor<SysUser> captor =
                ArgumentCaptor.forClass(SysUser.class);

        verify(sysUserMapper).insert(captor.capture());

        SysUser saved = captor.getValue();

        assertNotNull(saved);
        assertEquals("sunjingru", saved.getUsername());
        assertEquals("孙靖茹", saved.getRealName());
    }

    /**
     * 同名教师：账号自动追加序号。
     */
    @Test
    void addShouldAppendSuffixWhenUsernameExists() {

        stubAdminAndPositions(position(200L, "辅导员"));

        when(sysUserMapper.insert(any(SysUser.class))).thenReturn(1);

        // 第一次查询已存在，第二次不存在
        when(sysUserMapper.selectCount(any())).thenReturn(1L, 0L);

        when(departmentMapper.selectById(DEPT_ID))
                .thenReturn(department(DEPT_ID, "档案部", null));

        when(sysDepartmentMapper.selectOne(any()))
                .thenReturn(sysDepartment(10L, "档案部"));

        TeacherAddDTO dto = new TeacherAddDTO();
        dto.setRealName("孙靖茹");
        dto.setPositionName("辅导员");
        dto.setDepartmentIds(List.of(DEPT_ID));
        dto.setPassword("123456");

        Result<String> result = controller.add(dto, request);

        assertEquals(200, result.getCode(), result.getMessage());
        assertEquals("sunjingru2", result.getData());
    }

    // =========================================================
    // helpers
    // =========================================================

    private void stubAdminAndPositions(SysPosition targetPosition) {

        when(request.getAttribute("userId")).thenReturn(ADMIN_ID);

        when(positionMapper.selectOne(any()))
                .thenReturn(position(100L, "管理员"), targetPosition);

        when(userPositionMapper.selectCount(any())).thenReturn(1L);
    }

    private SysPosition position(Long id, String name) {

        SysPosition position = new SysPosition();
        position.setId(id);
        position.setName(name);
        position.setStatus((short) 1);

        return position;
    }

    private Department department(Long id, String name, Long teacherId) {

        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setTeacherId(teacherId);
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
