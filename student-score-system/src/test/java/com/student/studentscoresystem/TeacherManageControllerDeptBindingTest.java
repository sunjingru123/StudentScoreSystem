package com.student.studentscoresystem;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * =========================================================
 * 「教师管理 - 编辑教师」管理部门绑定回归测试
 * =========================================================
 *
 * 背景：
 * MyBatis-Plus 默认 update-strategy = NOT_NULL，
 * updateById 会忽略值为 null 的字段。
 * 之前解绑旧部门用的是 setTeacherId(null) + updateById，
 * 生成的 SQL 里根本没有 teacher_id，
 * 导致「改成另一个部门」后旧部门仍然绑在该教师名下，
 * 前端刷新后看起来「修改成功但没生效」。
 *
 * 本测试保证：
 *   1. 取消勾选的部门必须显式 SET teacher_id = NULL；
 *   2. 仍然勾选的部门不能被误清空；
 *   3. 切换为高级辅导员时，原负责部门必须全部解绑。
 * =========================================================
 */
class TeacherManageControllerDeptBindingTest {

    private static final long ADMIN_ID = 1L;
    private static final long TEACHER_ID = 9L;

    private static final long ARCHIVE_DEPT_ID = 1L;
    private static final long PUBLICITY_DEPT_ID = 2L;

    private static boolean departmentTableInfoReady = false;

    private SysUserMapper sysUserMapper;
    private SysUserPositionMapper userPositionMapper;
    private SysPositionMapper positionMapper;
    private DepartmentMapper departmentMapper;
    private SysDepartmentMapper sysDepartmentMapper;
    private HttpServletRequest request;

    private TeacherManageController controller;

    @BeforeEach
    void setUp() {

        initDepartmentTableInfo();

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
     * 更换管理部门：原部门必须真正解绑（teacher_id = NULL）。
     */
    @Test
    void updateShouldClearTeacherIdOfRemovedDepartment() {

        stubAdminAndPositions(position(200L, "辅导员"));

        SysUser teacher = user(TEACHER_ID);

        when(sysUserMapper.selectById(TEACHER_ID)).thenReturn(teacher);
        when(sysUserMapper.selectCount(any())).thenReturn(0L);

        Department archive =
                department(ARCHIVE_DEPT_ID, "档案部", TEACHER_ID);

        Department publicity =
                department(PUBLICITY_DEPT_ID, "宣传部", null);

        when(departmentMapper.selectById(PUBLICITY_DEPT_ID))
                .thenReturn(publicity);

        when(sysDepartmentMapper.selectOne(any()))
                .thenReturn(sysDepartment(20L, "宣传部"));

        when(userPositionMapper.selectOne(any()))
                .thenReturn(relation(5L, TEACHER_ID));

        /*
         * 该教师原本负责「档案部」，本次提交只勾选「宣传部」。
         */
        when(departmentMapper.selectList(any()))
                .thenReturn(new ArrayList<>(List.of(archive)));

        TeacherAddDTO dto = dto("辅导员", List.of(PUBLICITY_DEPT_ID));

        Result<Void> result = controller.update(TEACHER_ID, dto, request);

        assertEquals(200, result.getCode(), result.getMessage());

        LambdaUpdateWrapper<Department> unbind =
                captureUnbindWrapper(times(1));

        String sqlSet = unbind.getSqlSet();

        assertNotNull(sqlSet);
        assertTrue(
                sqlSet.contains("teacher_id"),
                "解绑 SQL 必须包含 teacher_id，实际：" + sqlSet
        );
        assertTrue(
                unbind.getParamNameValuePairs().containsValue(null),
                "解绑必须把 teacher_id 置为 NULL"
        );

        // 新勾选的部门正常绑定
        assertEquals(TEACHER_ID, publicity.getTeacherId());
        verify(departmentMapper).updateById(publicity);
    }

    /**
     * 部门没有变化时不能误清空 teacher_id。
     */
    @Test
    void updateShouldNotTouchDepartmentStillSelected() {

        stubAdminAndPositions(position(200L, "辅导员"));

        when(sysUserMapper.selectById(TEACHER_ID)).thenReturn(user(TEACHER_ID));
        when(sysUserMapper.selectCount(any())).thenReturn(0L);

        Department archive =
                department(ARCHIVE_DEPT_ID, "档案部", TEACHER_ID);

        when(departmentMapper.selectById(ARCHIVE_DEPT_ID))
                .thenReturn(archive);

        when(sysDepartmentMapper.selectOne(any()))
                .thenReturn(sysDepartment(10L, "档案部"));

        when(userPositionMapper.selectOne(any()))
                .thenReturn(relation(5L, TEACHER_ID));

        when(departmentMapper.selectList(any()))
                .thenReturn(new ArrayList<>(List.of(archive)));

        TeacherAddDTO dto = dto("辅导员", List.of(ARCHIVE_DEPT_ID));

        Result<Void> result = controller.update(TEACHER_ID, dto, request);

        assertEquals(200, result.getCode(), result.getMessage());

        // 没有解绑动作
        verify(departmentMapper, never()).update(any(), any());
        // 绑定关系保持不变
        assertEquals(TEACHER_ID, archive.getTeacherId());
    }

    /**
     * 切换为高级辅导员：原负责部门必须全部解绑。
     */
    @Test
    void updateShouldUnbindAllDepartmentsWhenSwitchingToSeniorCounselor() {

        stubAdminAndPositions(position(300L, "高级辅导员"));

        when(sysUserMapper.selectById(TEACHER_ID)).thenReturn(user(TEACHER_ID));
        when(sysUserMapper.selectCount(any())).thenReturn(0L);

        Department archive =
                department(ARCHIVE_DEPT_ID, "档案部", TEACHER_ID);

        SysUserPosition relation = relation(5L, TEACHER_ID);

        when(userPositionMapper.selectOne(any())).thenReturn(relation);

        when(sysDepartmentMapper.selectOne(any()))
                .thenReturn(sysDepartment(30L, "高级辅导员"));

        when(departmentMapper.selectList(any()))
                .thenReturn(new ArrayList<>(List.of(archive)));

        TeacherAddDTO dto = dto("高级辅导员", null);

        Result<Void> result = controller.update(TEACHER_ID, dto, request);

        assertEquals(200, result.getCode(), result.getMessage());

        LambdaUpdateWrapper<Department> unbind =
                captureUnbindWrapper(times(1));

        assertTrue(
                unbind.getSqlSet() != null
                        && unbind.getSqlSet().contains("teacher_id"),
                "高级辅导员不应占用 department.teacher_id"
        );

        assertEquals(30L, relation.getDepartmentId());
    }

    // =========================================================
    // helpers
    // =========================================================

    /**
     * 纯单元测试没有 MyBatis-Plus 的 Spring 上下文，
     * LambdaUpdateWrapper 解析列名依赖 TableInfo 缓存，
     * 这里手动完成 Department 的实体元数据初始化。
     */
    private static synchronized void initDepartmentTableInfo() {

        if (departmentTableInfoReady) {
            return;
        }

        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");

        assistant.setCurrentNamespace(
                DepartmentMapper.class.getName()
        );

        TableInfoHelper.initTableInfo(assistant, Department.class);

        departmentTableInfoReady = true;
    }

    /**
     * 管理员校验 + 岗位查询。
     *
     * 第一次 positionMapper.selectOne 是 isAdmin（管理员），
     * 第二次是 getPositionByName（目标岗位）。
     */
    private void stubAdminAndPositions(SysPosition targetPosition) {

        when(request.getAttribute("userId")).thenReturn(ADMIN_ID);

        when(positionMapper.selectOne(any()))
                .thenReturn(position(100L, "管理员"), targetPosition);

        when(userPositionMapper.selectCount(any())).thenReturn(1L);
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<Department> captureUnbindWrapper(
            org.mockito.verification.VerificationMode mode
    ) {

        ArgumentCaptor<Wrapper<Department>> captor =
                ArgumentCaptor.forClass(Wrapper.class);

        verify(departmentMapper, mode).update(isNull(), captor.capture());

        return (LambdaUpdateWrapper<Department>) captor.getValue();
    }

    private TeacherAddDTO dto(String positionName, List<Long> departmentIds) {

        TeacherAddDTO dto = new TeacherAddDTO();
        dto.setRealName("李老师");
        dto.setUsername("li");
        dto.setPositionName(positionName);
        dto.setDepartmentIds(departmentIds);

        return dto;
    }

    private SysUser user(Long id) {

        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("li");
        user.setRealName("李老师");
        user.setStatus((short) 1);

        return user;
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

    private SysUserPosition relation(Long id, Long userId) {

        SysUserPosition relation = new SysUserPosition();
        relation.setId(id);
        relation.setUserId(userId);

        return relation;
    }
}
