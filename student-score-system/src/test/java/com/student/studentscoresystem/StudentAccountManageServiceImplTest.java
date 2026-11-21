package com.student.studentscoresystem;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.student.studentscoresystem.entity.OperationLog;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.OperationLogMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.impl.StudentAccountManageServiceImpl;
import com.student.studentscoresystem.vo.RecycledStudentVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 学生软删除（回收站）服务测试。
 *
 * 重点验证：
 *
 * 1. 只能删除拥有「学生」岗位的账号；
 * 2. 删除原因必填；
 * 3. 删除写入 deleted 标记、操作管理员、原因，并写操作日志；
 * 4. 恢复清除删除标记；
 * 5. 重复删除 / 重复恢复被拒绝。
 */
@ExtendWith(MockitoExtension.class)
class StudentAccountManageServiceImplTest {

    private static final Long STUDENT_ID = 100L;

    private static final Long ADMIN_ID = 1L;

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private SysUserPositionMapper userPositionMapper;

    @Mock
    private SysPositionMapper positionMapper;

    @Mock
    private OperationLogMapper operationLogMapper;

    private StudentAccountManageServiceImpl service;

    @BeforeAll
    static void initTableInfo() {

        /*
         * LambdaUpdateWrapper 需要实体的表元信息，
         * 单元测试中没有 Spring 容器，手动初始化。
         */
        if (TableInfoHelper.getTableInfo(SysUser.class) != null) {

            return;
        }

        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(
                        new MybatisConfiguration(),
                        ""
                );

        assistant.setCurrentNamespace(
                SysUserMapper.class.getName()
        );

        TableInfoHelper.initTableInfo(
                assistant,
                SysUser.class
        );
    }

    @BeforeEach
    void setUp() {

        service =
                new StudentAccountManageServiceImpl(
                        userMapper,
                        userPositionMapper,
                        positionMapper,
                        operationLogMapper
                );
    }

    /* =====================================================
     * 软删除
     * ===================================================== */

    @Test
    void softDeleteShouldRejectBlankReason() {

        IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.softDeleteStudent(
                                STUDENT_ID,
                                "   ",
                                ADMIN_ID
                        )
                );

        assertEquals(
                "请填写删除原因",
                error.getMessage()
        );

        verify(userMapper, never()).update(any(), any());
    }

    @Test
    void softDeleteShouldRejectNonStudentAccount() {

        when(userMapper.selectById(STUDENT_ID))
                .thenReturn(user(0));

        /* 该账号只有「管理员」岗位，没有「学生」岗位 */
        when(userPositionMapper.selectList(any()))
                .thenReturn(Collections.singletonList(relation(9L)));

        SysPosition adminPosition = new SysPosition();
        adminPosition.setId(9L);
        adminPosition.setName("管理员");

        when(positionMapper.selectById(9L))
                .thenReturn(adminPosition);

        IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.softDeleteStudent(
                                STUDENT_ID,
                                "测试原因",
                                ADMIN_ID
                        )
                );

        assertEquals(
                "只能删除学生账号",
                error.getMessage()
        );

        verify(userMapper, never()).update(any(), any());
        verify(operationLogMapper, never()).insert(any(OperationLog.class));
    }

    @Test
    void softDeleteShouldMarkStudentAsDeleted() {

        when(userMapper.selectById(STUDENT_ID))
                .thenReturn(user(0));

        when(userPositionMapper.selectList(any()))
                .thenReturn(Collections.singletonList(relation(3L)));

        SysPosition studentPosition = new SysPosition();
        studentPosition.setId(3L);
        studentPosition.setName("学生");

        when(positionMapper.selectById(3L))
                .thenReturn(studentPosition);

        when(userMapper.update(any(), any())).thenReturn(1);

        service.softDeleteStudent(
                STUDENT_ID,
                "  已退学  ",
                ADMIN_ID
        );

        ArgumentCaptor<Wrapper<SysUser>> wrapperCaptor =
                ArgumentCaptor.forClass(Wrapper.class);

        verify(userMapper).update(
                any(),
                wrapperCaptor.capture()
        );

        LambdaUpdateWrapper<SysUser> wrapper =
                (LambdaUpdateWrapper<SysUser>) wrapperCaptor.getValue();

        /*
         * 5 个 set 绑定参数：
         * deleted、deleted_time、deleted_by、
         * delete_reason、active_session_id
         */
        assertEquals(
                5,
                wrapper.getParamNameValuePairs().size()
        );

        /*
         * 删除原因首尾空白被裁剪
         */
        assertTrue(
                wrapper.getParamNameValuePairs()
                        .containsValue("已退学")
        );

        assertTrue(
                wrapper.getParamNameValuePairs()
                        .containsValue((short) 1)
        );

        assertTrue(
                wrapper.getParamNameValuePairs()
                        .containsValue(ADMIN_ID)
        );

        /* active_session_id 被置空，旧 Token 立即失效 */
        assertTrue(
                wrapper.getParamNameValuePairs()
                        .containsValue(null)
        );

        ArgumentCaptor<OperationLog> logCaptor =
                ArgumentCaptor.forClass(OperationLog.class);

        verify(operationLogMapper).insert(logCaptor.capture());

        OperationLog log = logCaptor.getValue();

        assertEquals("删除学生", log.getOperation());
        assertEquals(ADMIN_ID, log.getUserId());
        assertNotNull(log.getDescription());
        assertTrue(log.getDescription().contains("已退学"));
    }

    @Test
    void softDeleteShouldRejectAlreadyDeletedStudent() {

        when(userMapper.selectById(STUDENT_ID))
                .thenReturn(user(1));

        IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.softDeleteStudent(
                                STUDENT_ID,
                                "测试原因",
                                ADMIN_ID
                        )
                );

        assertEquals(
                "该学生已在回收站中，无需重复删除",
                error.getMessage()
        );

        verify(userMapper, never()).update(any(), any());
    }

    /* =====================================================
     * 恢复
     * ===================================================== */

    @Test
    void restoreShouldRejectStudentThatIsNotDeleted() {

        when(userMapper.selectById(STUDENT_ID))
                .thenReturn(user(0));

        IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.restoreStudent(
                                STUDENT_ID,
                                ADMIN_ID
                        )
                );

        assertEquals(
                "该学生未被删除，无需恢复",
                error.getMessage()
        );

        verify(userMapper, never()).update(any(), any());
    }

    @Test
    void restoreShouldClearDeletedFlags() {

        when(userMapper.selectById(STUDENT_ID))
                .thenReturn(user(1));

        when(userMapper.update(any(), any())).thenReturn(1);

        service.restoreStudent(
                STUDENT_ID,
                ADMIN_ID
        );

        ArgumentCaptor<Wrapper<SysUser>> wrapperCaptor =
                ArgumentCaptor.forClass(Wrapper.class);

        verify(userMapper).update(
                any(),
                wrapperCaptor.capture()
        );

        LambdaUpdateWrapper<SysUser> wrapper =
                (LambdaUpdateWrapper<SysUser>) wrapperCaptor.getValue();

        /*
         * 4 个 set 绑定参数：
         * deleted、deleted_time、deleted_by、delete_reason
         */
        assertEquals(
                4,
                wrapper.getParamNameValuePairs().size()
        );

        assertTrue(
                wrapper.getParamNameValuePairs()
                        .containsValue((short) 0)
        );

        /* 删除时间 / 删除人 / 删除原因全部置空 */
        assertTrue(
                wrapper.getParamNameValuePairs()
                        .containsValue(null)
        );

        ArgumentCaptor<OperationLog> logCaptor =
                ArgumentCaptor.forClass(OperationLog.class);

        verify(operationLogMapper).insert(logCaptor.capture());

        assertEquals(
                "恢复学生",
                logCaptor.getValue().getOperation()
        );
    }

    /* =====================================================
     * 回收站列表
     * ===================================================== */

    @Test
    void pageRecycledStudentsShouldMapDeletionInfo() {

        SysUser deleted = user(1);

        deleted.setDeletedTime(
                LocalDateTime.of(
                        2026,
                        1,
                        2,
                        3,
                        4,
                        5
                )
        );

        deleted.setDeletedBy(ADMIN_ID);
        deleted.setDeleteReason("已退学");

        Page<SysUser> page =
                new Page<>(1, 10, 1);

        page.setRecords(
                new ArrayList<>(
                        List.of(deleted)
                )
        );

        when(userMapper.selectPage(any(), any()))
                .thenReturn(page);

        SysUser operator = new SysUser();

        operator.setId(ADMIN_ID);
        operator.setRealName("张三");

        when(userMapper.selectBatchIds(any()))
                .thenReturn(
                        new ArrayList<>(
                                List.of(operator)
                        )
                );

        Page<RecycledStudentVO> result =
                service.pageRecycledStudents(
                        1,
                        10,
                        null,
                        null
                );

        assertEquals(1, result.getRecords().size());

        RecycledStudentVO vo =
                result.getRecords().get(0);

        assertEquals(STUDENT_ID, vo.getId());
        assertEquals("20250001", vo.getStudentNo());
        assertEquals("李四", vo.getRealName());
        assertEquals("已退学", vo.getDeleteReason());
        assertEquals("张三", vo.getDeletedByName());
        assertNotNull(vo.getDeletedTime());
    }

    @Test
    void pageRecycledStudentsShouldReturnEmptyPageWhenNoData() {

        Page<SysUser> page =
                new Page<>(1, 10, 0);

        page.setRecords(
                new ArrayList<>()
        );

        when(userMapper.selectPage(any(), any()))
                .thenReturn(page);

        Page<RecycledStudentVO> result =
                service.pageRecycledStudents(
                        1,
                        10,
                        "李四",
                        null
                );

        assertNotNull(result.getRecords());
        assertTrue(result.getRecords().isEmpty());

        verify(userMapper, never()).selectBatchIds(any());
    }

    /* =====================================================
     * 辅助
     * ===================================================== */

    private SysUser user(int deleted) {

        SysUser user = new SysUser();

        user.setId(STUDENT_ID);
        user.setStudentNo("20250001");
        user.setUsername("20250001");
        user.setRealName("李四");
        user.setClassName("25级7班");
        user.setStatus((short) 1);
        user.setDeleted((short) deleted);

        return user;
    }

    private SysUserPosition relation(Long positionId) {

        SysUserPosition relation =
                new SysUserPosition();

        relation.setUserId(STUDENT_ID);
        relation.setPositionId(positionId);

        return relation;
    }

}
