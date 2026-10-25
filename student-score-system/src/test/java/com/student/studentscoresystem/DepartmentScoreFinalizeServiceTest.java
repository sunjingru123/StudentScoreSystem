package com.student.studentscoresystem;

import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.entity.SysSemester;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.ScoreRuleMapper;
import com.student.studentscoresystem.service.DepartmentScoreFinalizeService;
import com.student.studentscoresystem.service.IDepartmentScoreApplyService;
import com.student.studentscoresystem.service.ISysSemesterService;
import com.student.studentscoresystem.service.ScoreProjectNameResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * 部门加减分申报终审的规则匹配测试。
 *
 * 重点验证：同一部门存在同名但分值 / 类型不同的规则时，
 * 必须匹配到分值、类型与申报完全一致的那条，
 * 不能因为同名规则而匹配到错误规则。
 *
 * 另外验证：档案部审核时调整过的分值可能不再等于任何正式规则，
 * 此时不能阻断成绩生成，只是不再关联 rule_id。
 */
@ExtendWith(MockitoExtension.class)
class DepartmentScoreFinalizeServiceTest {

    private static final String TITLE =
            "教室卫生检查：卫生优良";

    @Mock
    private ScoreRecordMapper scoreRecordMapper;

    @Mock
    private ScoreRuleMapper scoreRuleMapper;

    @Mock
    private IDepartmentScoreApplyService applyService;

    @Mock
    private ISysSemesterService sysSemesterService;

    private DepartmentScoreFinalizeService service;

    @BeforeEach
    void setUp() {

        service = new DepartmentScoreFinalizeService(
                scoreRecordMapper,
                scoreRuleMapper,
                applyService,
                sysSemesterService
        );

        lenient()
                .when(scoreRecordMapper.selectCount(any()))
                .thenReturn(0L);

        lenient()
                .when(scoreRecordMapper.insert(any(ScoreRecord.class)))
                .thenReturn(1);

        lenient()
                .when(applyService.updateById(any(DepartmentScoreApply.class)))
                .thenReturn(true);

        SysSemester semester = new SysSemester();

        semester.setId(9L);

        lenient()
                .when(sysSemesterService.getCurrentSemester())
                .thenReturn(semester);
    }

    @Test
    void matchesRuleWithSameScoreInsteadOfFirstSameNameRule() {

        whenRules(
                rule(1L, "0.50", (short) 1),
                rule(2L, "1.00", (short) 1)
        );

        String message =
                service.finalizeApproval(
                        apply("1.00", (short) 1),
                        1L,
                        "通过"
                );

        assertNull(message, "应成功匹配到分值一致的规则");

        ScoreRecord record = captureRecord();

        assertEquals(2L, record.getRuleId());
        assertScore("1.00", record.getScore());
    }

    @Test
    void matchesRuleWithSameScoreAndType() {

        whenRules(
                rule(1L, "1.00", (short) 1),
                rule(2L, "1.00", (short) -1)
        );

        String message =
                service.finalizeApproval(
                        apply("1.00", (short) -1),
                        1L,
                        "通过"
                );

        assertNull(message, "应匹配到加减分类型一致的规则");

        ScoreRecord record = captureRecord();

        assertEquals(2L, record.getRuleId());
        assertScore("-1.00", record.getScore());
    }

    @Test
    void allowsAdjustedScoreWithoutMatchingFormalRule() {

        whenRules(
                rule(1L, "0.50", (short) 1),
                rule(2L, "1.00", (short) 1)
        );

        String message =
                service.finalizeApproval(
                        apply("2.00", (short) 1),
                        1L,
                        "通过"
                );

        assertNull(
                message,
                "档案部调整过的分值不应阻断终审，实际：" + message
        );

        ScoreRecord record = captureRecord();

        assertNull(
                record.getRuleId(),
                "分值不匹配任何正式规则时不应误挂 rule_id"
        );

        assertScore("2.00", record.getScore());
    }

    @Test
    void allowsDepartmentActivityWithoutFormalRule() {

        whenRules();

        String message =
                service.finalizeApproval(
                        apply("0.30", (short) 1),
                        1L,
                        "通过"
                );

        assertNull(message);

        ScoreRecord record = captureRecord();

        assertNull(record.getRuleId());
        assertScore("0.30", record.getScore());
        assertEquals(
                ScoreProjectNameResolver.TYPE_DEPARTMENT,
                record.getSourceType()
        );
    }

    private void whenRules(ScoreRule... rules) {

        lenient()
                .when(scoreRuleMapper.selectList(any()))
                .thenReturn(
                        rules.length == 0
                                ? Collections.emptyList()
                                : List.of(rules)
                );
    }

    private ScoreRule rule(
            Long id,
            String score,
            Short scoreType
    ) {

        ScoreRule rule = new ScoreRule();

        rule.setId(id);
        rule.setDepartmentId(5L);
        rule.setName(TITLE);
        rule.setScore(new BigDecimal(score));
        rule.setScoreType(scoreType);
        rule.setStatus((short) 1);

        return rule;
    }

    private DepartmentScoreApply apply(
            String score,
            Short scoreType
    ) {

        DepartmentScoreApply apply =
                new DepartmentScoreApply();

        apply.setId(10L);
        apply.setStudentId(100L);
        apply.setDepartmentId(5L);
        apply.setTitle(TITLE);
        apply.setScore(new BigDecimal(score));
        apply.setScoreType(scoreType);

        return apply;
    }

    private ScoreRecord captureRecord() {

        ArgumentCaptor<ScoreRecord> captor =
                ArgumentCaptor.forClass(ScoreRecord.class);

        verify(scoreRecordMapper).insert(captor.capture());

        return captor.getValue();
    }

    private void assertScore(
            String expected,
            BigDecimal actual
    ) {

        assertNotNull(actual);

        assertEquals(
                0,
                new BigDecimal(expected).compareTo(actual),
                "成绩分值应为 " + expected
        );
    }
}
