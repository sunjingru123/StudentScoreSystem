package com.student.studentscoresystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.DepartmentScoreApplyMapper;
import com.student.studentscoresystem.mapper.ScoreAdminAdjustmentMapper;
import com.student.studentscoresystem.mapper.ScoreApplyMapper;
import com.student.studentscoresystem.mapper.ScoreRuleMapper;
import com.student.studentscoresystem.service.ScoreProjectNameResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 成绩明细「备注」预取测试。
 *
 * 部门干部（部长 / 副部长 / 干事）申报加减分时填写的备注
 * 保存在 department_score_apply.description，
 * 学生成绩明细必须能看到它。
 *
 * 同时守住性能约束：多条成绩记录只能触发一次批量查询，
 * 不能再退化成逐条 selectById 的 N+1。
 */
@ExtendWith(MockitoExtension.class)
class ScoreProjectNameResolverRemarkTest {

    private static final String REMARK =
            "本周教室卫生检查不合格，扣 0.5 分";

    @Mock
    private ScoreRuleMapper scoreRuleMapper;

    @Mock
    private ScoreApplyMapper scoreApplyMapper;

    @Mock
    private DepartmentScoreApplyMapper departmentScoreApplyMapper;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private ScoreAdminAdjustmentMapper scoreAdminAdjustmentMapper;

    private ScoreProjectNameResolver resolver;

    @BeforeEach
    void setUp() {

        resolver =
                new ScoreProjectNameResolver(
                        scoreRuleMapper,
                        scoreApplyMapper,
                        departmentScoreApplyMapper,
                        departmentMapper,
                        scoreAdminAdjustmentMapper,
                        new ObjectMapper()
                );
    }

    /**
     * 部门申报备注必须能被学生成绩明细读到。
     */
    @Test
    void departmentApplyRemarkIsExposed() {

        when(departmentScoreApplyMapper.selectBatchIds(any()))
                .thenReturn(
                        List.of(
                                departmentApply(
                                        9L,
                                        3L,
                                        "教室卫生检查",
                                        REMARK
                                )
                        )
                );

        when(departmentMapper.selectBatchIds(any()))
                .thenReturn(
                        List.of(
                                department(
                                        3L,
                                        "生活部"
                                )
                        )
                );

        ScoreRecord record =
                departmentRecord(
                        9L
                );

        ScoreProjectNameResolver.PreloadedNames names =
                resolver.preload(
                        List.of(record)
                );

        assertEquals(
                REMARK,
                names.resolveRemark(record)
        );
    }

    /**
     * 备注为空（单条申报只带模板说明、模板说明也为空）时返回 null，
     * 前端不显示空白备注。
     */
    @Test
    void blankRemarkIsIgnored() {

        when(departmentScoreApplyMapper.selectBatchIds(any()))
                .thenReturn(
                        List.of(
                                departmentApply(
                                        9L,
                                        null,
                                        "教室卫生检查",
                                        "   "
                                )
                        )
                );

        ScoreRecord record =
                departmentRecord(
                        9L
                );

        ScoreProjectNameResolver.PreloadedNames names =
                resolver.preload(
                        List.of(record)
                );

        assertNull(
                names.resolveRemark(record)
        );
    }

    /**
     * 非部门申报（个人证书等）没有备注，不能把证书的 JSON 描述暴露出去。
     */
    @Test
    void nonDepartmentRecordHasNoRemark() {

        when(scoreApplyMapper.selectBatchIds(any()))
                .thenReturn(
                        Collections.emptyList()
                );

        ScoreRecord record =
                new ScoreRecord();

        record.setId(1L);
        record.setSourceType(
                ScoreProjectNameResolver.TYPE_CERTIFICATE
        );
        record.setSourceId(
                5L
        );

        ScoreProjectNameResolver.PreloadedNames names =
                resolver.preload(
                        List.of(record)
                );

        assertNull(
                names.resolveRemark(record)
        );
    }

    /**
     * 部门申报记录已经不存在时返回 null，不能抛异常。
     */
    @Test
    void missingDepartmentApplyHasNoRemark() {

        when(departmentScoreApplyMapper.selectBatchIds(any()))
                .thenReturn(
                        Collections.emptyList()
                );

        ScoreRecord record =
                departmentRecord(
                        9L
                );

        ScoreProjectNameResolver.PreloadedNames names =
                resolver.preload(
                        List.of(record)
                );

        assertNull(
                names.resolveRemark(record)
        );
    }

    /**
     * 多条部门申报成绩只允许一次批量查询（N+1 回归保护）。
     */
    @Test
    void departmentAppliesAreBatchLoadedOnce() {

        DepartmentScoreApply first =
                departmentApply(
                        9L,
                        3L,
                        "教室卫生检查",
                        REMARK
                );

        DepartmentScoreApply second =
                departmentApply(
                        10L,
                        3L,
                        "宿舍卫生检查",
                        "第二次检查仍不合格"
                );

        when(departmentScoreApplyMapper.selectBatchIds(any()))
                .thenReturn(
                        List.of(
                                first,
                                second
                        )
                );

        when(departmentMapper.selectBatchIds(any()))
                .thenReturn(
                        List.of(
                                department(
                                        3L,
                                        "生活部"
                                )
                        )
                );

        ScoreRecord firstRecord =
                departmentRecord(
                        9L
                );

        ScoreRecord secondRecord =
                departmentRecord(
                        10L
                );

        ScoreProjectNameResolver.PreloadedNames names =
                resolver.preload(
                        List.of(
                                firstRecord,
                                secondRecord
                        )
                );

        assertEquals(
                REMARK,
                names.resolveRemark(firstRecord)
        );

        assertEquals(
                "第二次检查仍不合格",
                names.resolveRemark(secondRecord)
        );

        verify(
                departmentScoreApplyMapper,
                times(1)
        ).selectBatchIds(any());

        verify(
                departmentMapper,
                times(1)
        ).selectBatchIds(any());
    }

    private static ScoreRecord departmentRecord(
            Long applyId
    ) {

        ScoreRecord record =
                new ScoreRecord();

        record.setId(
                applyId
        );

        record.setSourceType(
                ScoreProjectNameResolver.TYPE_DEPARTMENT
        );

        record.setSourceId(
                applyId
        );

        return record;
    }

    private static DepartmentScoreApply departmentApply(
            Long id,
            Long departmentId,
            String title,
            String description
    ) {

        DepartmentScoreApply apply =
                new DepartmentScoreApply();

        apply.setId(
                id
        );

        apply.setDepartmentId(
                departmentId
        );

        apply.setTitle(
                title
        );

        apply.setDescription(
                description
        );

        return apply;
    }

    private static Department department(
            Long id,
            String name
    ) {

        Department department =
                new Department();

        department.setId(
                id
        );

        department.setName(
                name
        );

        return department;
    }
}
