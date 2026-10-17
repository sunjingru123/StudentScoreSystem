package com.student.studentscoresystem;

import com.student.studentscoresystem.entity.ScoreApply;
import com.student.studentscoresystem.mapper.ScoreApplyMapper;
import com.student.studentscoresystem.service.CertificatePreliminaryAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 个人证书初审业务规则测试。
 *
 * 覆盖：分值必填且必须大于 0、只允许 CERTIFICATE 申请、
 * 只有未初审的申请可以初审、通过后状态流转正确。
 */
@ExtendWith(MockitoExtension.class)
class CertificatePreliminaryAuditServiceTest {

    @Mock
    private ScoreApplyMapper scoreApplyMapper;

    private CertificatePreliminaryAuditService service;

    @BeforeEach
    void setUp() {

        service =
                new CertificatePreliminaryAuditService(
                        scoreApplyMapper
                );
    }

    @Test
    void rejectsNullId() {

        assertEquals(
                "申请ID不能为空",
                service.approveOne(null, new BigDecimal("3"), 7L)
        );

        verifyNoInteractions(scoreApplyMapper);
    }

    @Test
    void rejectsMissingScore() {

        assertEquals(
                "初审通过时必须填写认定分值",
                service.approveOne(1L, null, 7L)
        );

        verifyNoInteractions(scoreApplyMapper);
    }

    @Test
    void rejectsZeroOrNegativeScore() {

        assertEquals(
                "认定分值必须大于0",
                service.approveOne(1L, BigDecimal.ZERO, 7L)
        );

        assertEquals(
                "认定分值必须大于0",
                service.approveOne(1L, new BigDecimal("-1"), 7L)
        );

        verifyNoInteractions(scoreApplyMapper);
    }

    @Test
    void rejectsMissingApply() {

        when(scoreApplyMapper.selectById(1L)).thenReturn(null);

        assertEquals(
                "申请不存在",
                service.approveOne(1L, new BigDecimal("3"), 7L)
        );

        verify(scoreApplyMapper, never()).updateById(any(ScoreApply.class));
    }

    @Test
    void rejectsNonCertificateApply() {

        ScoreApply apply =
                new ScoreApply();

        apply.setId(1L);
        apply.setApplyType("ACTIVITY");
        apply.setPreliminaryStatus((short) 0);

        when(scoreApplyMapper.selectById(1L)).thenReturn(apply);

        assertEquals(
                "该申请不是个人证书申请",
                service.approveOne(1L, new BigDecimal("3"), 7L)
        );

        verify(scoreApplyMapper, never()).updateById(any(ScoreApply.class));
    }

    @Test
    void rejectsAlreadyPreliminaryReviewedApply() {

        ScoreApply apply =
                new ScoreApply();

        apply.setId(1L);
        apply.setApplyType("CERTIFICATE");
        apply.setPreliminaryStatus((short) 1);

        when(scoreApplyMapper.selectById(1L)).thenReturn(apply);

        assertEquals(
                "该申请已经完成初审，不能重复审核",
                service.approveOne(1L, new BigDecimal("3"), 7L)
        );

        verify(scoreApplyMapper, never()).updateById(any(ScoreApply.class));
    }

    @Test
    void approvesPendingCertificateAndTransitionsStatus() {

        ScoreApply apply =
                new ScoreApply();

        apply.setId(1L);
        apply.setApplyType("CERTIFICATE");
        apply.setPreliminaryStatus((short) 0);

        when(scoreApplyMapper.selectById(1L)).thenReturn(apply);
        when(scoreApplyMapper.updateById(any(ScoreApply.class)))
                .thenReturn(1);

        assertNull(
                service.approveOne(1L, new BigDecimal("3.5"), 7L)
        );

        assertEquals(
                0,
                apply.getApplyScore().compareTo(new BigDecimal("3.5"))
        );

        assertEquals(
                Short.valueOf((short) 1),
                apply.getPreliminaryStatus()
        );

        assertEquals(
                Short.valueOf((short) 0),
                apply.getFinalStatus()
        );

        assertEquals(
                Short.valueOf((short) 0),
                apply.getStatus()
        );

        assertEquals(
                Long.valueOf(7L),
                apply.getPreliminaryReviewerId()
        );

        assertNotNull(apply.getPreliminaryReviewTime());
        assertNotNull(apply.getUpdateTime());

        verify(scoreApplyMapper).updateById(any(ScoreApply.class));
    }
}
