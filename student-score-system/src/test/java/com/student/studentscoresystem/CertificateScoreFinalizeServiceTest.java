package com.student.studentscoresystem;

import com.student.studentscoresystem.entity.ScoreApply;
import com.student.studentscoresystem.entity.ScoreFlow;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.SysSemester;
import com.student.studentscoresystem.mapper.ScoreApplyMapper;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.service.CertificateScoreFinalizeService;
import com.student.studentscoresystem.service.IScoreFlowService;
import com.student.studentscoresystem.service.ISysSemesterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * 个人证书终审的单元测试。
 *
 * 重点验证：
 *
 * 1. 档案部副部长 / 部长可以在终审时修改认定分值；
 * 2. 不传分值（null）时沿用初审写入的分值；
 * 3. 修改分值必须大于 0，否则阻断且不生成成绩。
 */
@ExtendWith(MockitoExtension.class)
class CertificateScoreFinalizeServiceTest {

    @Mock
    private ScoreRecordMapper scoreRecordMapper;

    @Mock
    private ScoreApplyMapper scoreApplyMapper;

    @Mock
    private IScoreFlowService scoreFlowService;

    @Mock
    private ISysSemesterService sysSemesterService;

    private CertificateScoreFinalizeService service;

    @BeforeEach
    void setUp() {

        service = new CertificateScoreFinalizeService(
                scoreRecordMapper,
                scoreApplyMapper,
                scoreFlowService,
                sysSemesterService
        );
    }

    @Test
    void usesOverriddenScoreWhenProvided() {

        stubHappyPath();

        ScoreApply apply = pendingApply("1.00");

        String message =
                service.finalizeApproval(
                        apply,
                        7L,
                        new BigDecimal("2.50")
                );

        assertNull(message, "终审应成功，实际：" + message);

        ScoreRecord record = captureRecord();

        assertScore("2.50", record.getScore());
        assertScore("2.50", apply.getApplyScore());
        assertEquals((short) 1, apply.getFinalStatus());
    }

    @Test
    void keepsOriginalScoreWhenOverrideIsNull() {

        stubHappyPath();

        ScoreApply apply = pendingApply("1.00");

        String message =
                service.finalizeApproval(
                        apply,
                        7L,
                        null
                );

        assertNull(message, "终审应成功，实际：" + message);

        assertScore("1.00", captureRecord().getScore());
    }

    @Test
    void rejectsNonPositiveOverride() {

        ScoreApply apply = pendingApply("1.00");

        String message =
                service.finalizeApproval(
                        apply,
                        7L,
                        BigDecimal.ZERO
                );

        assertNotNull(message);
        assertEquals("认定分值必须大于0", message);
    }

    private void stubHappyPath() {

        SysSemester semester = new SysSemester();

        semester.setId(9L);

        lenient()
                .when(sysSemesterService.getCurrentSemester())
                .thenReturn(semester);

        lenient()
                .when(scoreRecordMapper.selectCount(any()))
                .thenReturn(0L);

        lenient()
                .when(scoreRecordMapper.selectList(any()))
                .thenReturn(Collections.emptyList());

        lenient()
                .when(scoreRecordMapper.insert(any(ScoreRecord.class)))
                .thenReturn(1);

        lenient()
                .when(scoreFlowService.save(any(ScoreFlow.class)))
                .thenReturn(true);

        lenient()
                .when(scoreApplyMapper.updateById(any(ScoreApply.class)))
                .thenReturn(1);
    }

    private ScoreApply pendingApply(String applyScore) {

        ScoreApply apply =
                new ScoreApply();

        apply.setId(20L);
        apply.setStudentId(100L);
        apply.setApplyType("CERTIFICATE");
        apply.setPreliminaryStatus((short) 1);
        apply.setFinalStatus((short) 0);
        apply.setApplyScore(new BigDecimal(applyScore));

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
            BigDecimal actual) {

        assertNotNull(actual);

        assertEquals(
                0,
                new BigDecimal(expected).compareTo(actual),
                "分值应为 " + expected
        );
    }
}
