package com.student.studentscoresystem;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.controller.SysSemesterController;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysSemester;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysSemesterMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * =========================================================
 * 学期时间区间重叠校验（测试报告 DQ-03）
 * =========================================================
 *
 * 现象：
 * 管理员新增 / 编辑学期时可以录入与已有学期重叠的时间区间，
 * 导致「当前学期」判定出现歧义、成绩按学期统计口径错乱。
 *
 * 规则：
 * 新增或编辑学期时，若 [startDate, endDate] 与其它已存在学期
 * 有交集，必须直接拒绝，不能落库。
 * =========================================================
 */
class SysSemesterControllerDateOverlapTest {

    private static final long ADMIN_ID = 1L;

    private SysSemesterMapper sysSemesterMapper;
    private SysPositionMapper positionMapper;
    private SysUserPositionMapper userPositionMapper;
    private HttpServletRequest request;

    private SysSemesterController controller;

    @BeforeEach
    void setUp() {

        /*
         * LambdaUpdateWrapper 需要 MyBatis-Plus 的实体缓存，
         * 纯单元测试环境必须手动初始化。
         */
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(
                        new MybatisConfiguration(),
                        ""
                );

        TableInfoHelper.initTableInfo(
                assistant,
                SysSemester.class
        );

        sysSemesterMapper = mock(SysSemesterMapper.class);

        positionMapper = mock(SysPositionMapper.class);

        userPositionMapper = mock(SysUserPositionMapper.class);

        request = mock(HttpServletRequest.class);

        controller = new SysSemesterController(
                sysSemesterMapper,
                positionMapper,
                userPositionMapper
        );

        /* 当前请求用户：管理员 */
        when(request.getAttribute("userId"))
                .thenReturn(ADMIN_ID);

        SysPosition adminPosition = new SysPosition();

        adminPosition.setId(10L);

        adminPosition.setName("管理员");

        adminPosition.setStatus((short) 1);

        when(positionMapper.selectOne(any()))
                .thenReturn(adminPosition);

        when(userPositionMapper.selectCount(any()))
                .thenReturn(1L);
    }

    /**
     * 与已有学期区间重叠：必须拒绝新增
     */
    @Test
    void addShouldRejectOverlappedDateRange() {

        /* 已存在学期：2024-09-01 ~ 2025-01-31 */
        when(sysSemesterMapper.selectCount(any()))
                .thenReturn(1L);

        SysSemester semester = new SysSemester();

        semester.setName("2024-2025学年第二学期");

        semester.setStartDate(LocalDate.of(2025, 1, 1));

        semester.setEndDate(LocalDate.of(2025, 7, 14));

        Result<Void> result = controller.add(semester, request);

        assertEquals(400, result.getCode());

        assertTrue(
                result.getMessage().contains("重叠"),
                "应提示学期区间重叠，实际为：" + result.getMessage()
        );

        /* 校验失败时不允许写入 */
        org.mockito.Mockito.verify(
                sysSemesterMapper,
                org.mockito.Mockito.never()
        ).insert(any(SysSemester.class));
    }

    /**
     * 区间不重叠：正常保存
     */
    @Test
    void addShouldAcceptNonOverlappedDateRange() {

        when(sysSemesterMapper.selectCount(any()))
                .thenReturn(0L);

        when(sysSemesterMapper.insert(any(SysSemester.class)))
                .thenReturn(1);

        SysSemester semester = new SysSemester();

        semester.setName("2024-2025学年第二学期");

        semester.setStartDate(LocalDate.of(2025, 2, 1));

        semester.setEndDate(LocalDate.of(2025, 7, 14));

        Result<Void> result = controller.add(semester, request);

        assertEquals(200, result.getCode());

        assertFalse(
                result.getMessage() != null
                        && result.getMessage().contains("重叠")
        );
    }

    /**
     * 编辑时排除自身：只有自身不构成重叠
     */
    @Test
    void updateShouldExcludeItselfWhenCheckingOverlap() {

        SysSemester existing = new SysSemester();

        existing.setId(3L);

        existing.setName("2024-2025学年第一学期");

        existing.setStartDate(LocalDate.of(2024, 9, 1));

        existing.setEndDate(LocalDate.of(2025, 1, 31));

        existing.setStatus((short) 1);

        when(sysSemesterMapper.selectById(3L))
                .thenReturn(existing);

        /* 除自身外没有其它学期重叠 */
        when(sysSemesterMapper.selectCount(any()))
                .thenReturn(0L);

        when(sysSemesterMapper.updateById(any(SysSemester.class)))
                .thenReturn(1);

        SysSemester semester = new SysSemester();

        semester.setName("2024-2025学年第一学期");

        semester.setStartDate(LocalDate.of(2024, 9, 1));

        semester.setEndDate(LocalDate.of(2025, 1, 31));

        Result<Void> result = controller.update(3L, semester, request);

        assertEquals(200, result.getCode());
    }
}
