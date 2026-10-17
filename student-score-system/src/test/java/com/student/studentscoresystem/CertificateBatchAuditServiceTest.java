package com.student.studentscoresystem;

import com.student.studentscoresystem.dto.BatchPreliminaryAuditDTO;
import com.student.studentscoresystem.entity.ScoreApply;
import com.student.studentscoresystem.mapper.ScoreApplyMapper;
import com.student.studentscoresystem.service.CertificateBatchAuditService;
import com.student.studentscoresystem.service.CertificatePreliminaryAuditService;
import com.student.studentscoresystem.service.CertificateScoreFinalizeService;
import com.student.studentscoresystem.vo.BatchAuditResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 个人证书批量审核（初审 / 终审）业务规则测试。
 *
 * 重点验证「逐条独立处理、部分失败不影响其它记录」的核心设计：
 * 一条失败不会中断整批，也不会伪造成功。
 */
@ExtendWith(MockitoExtension.class)
class CertificateBatchAuditServiceTest {

    @Mock
    private CertificatePreliminaryAuditService preliminaryAuditService;

    @Mock
    private CertificateScoreFinalizeService certificateScoreFinalizeService;

    @Mock
    private ScoreApplyMapper scoreApplyMapper;

    private CertificateBatchAuditService service;

    @BeforeEach
    void setUp() {

        service =
                new CertificateBatchAuditService(
                        preliminaryAuditService,
                        certificateScoreFinalizeService,
                        scoreApplyMapper
                );
    }

    private BatchPreliminaryAuditDTO.Item item(
            Long id,
            String score
    ) {

        BatchPreliminaryAuditDTO.Item item =
                new BatchPreliminaryAuditDTO.Item();

        item.setId(id);

        item.setFinalScore(
                score == null
                        ? null
                        : new BigDecimal(score)
        );

        return item;
    }

    private ScoreApply certificateApply(
            Long id,
            short preliminaryStatus,
            short finalStatus,
            String score
    ) {

        ScoreApply apply =
                new ScoreApply();

        apply.setId(id);

        apply.setApplyType("CERTIFICATE");

        apply.setPreliminaryStatus(preliminaryStatus);

        apply.setFinalStatus(finalStatus);

        apply.setApplyScore(
                score == null
                        ? null
                        : new BigDecimal(score)
        );

        return apply;
    }

    @Test
    void batchPreliminaryCollectsSuccessAndFailurePerItem() {

        when(
                preliminaryAuditService.approveOne(
                        eq(1L),
                        any(),
                        eq(9L)
                )
        ).thenReturn(null);

        when(
                preliminaryAuditService.approveOne(
                        eq(2L),
                        any(),
                        eq(9L)
                )
        ).thenReturn("认定分值必须大于0");

        BatchAuditResultVO result =
                service.batchPreliminary(
                        List.of(
                                item(1L, "3"),
                                item(2L, "0")
                        ),
                        9L
                );

        assertEquals(2, result.getTotal());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertEquals(1, result.getFailures().size());
        assertTrue(
                result.getFailures().get(0).contains("申请[2]")
        );
    }

    @Test
    void batchPreliminaryRejectsItemWithMissingId() {

        BatchAuditResultVO result =
                service.batchPreliminary(
                        List.of(
                                item(null, "3")
                        ),
                        9L
                );

        assertEquals(1, result.getTotal());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        verifyNoInteractions(preliminaryAuditService);
    }

    @Test
    void batchFinalIsolatesSuccessGuardAndDuplicateFailures() {

        ScoreApply ok =
                certificateApply(1L, (short) 1, (short) 0, "5");

        ScoreApply alreadyFinal =
                certificateApply(2L, (short) 1, (short) 1, "5");

        ScoreApply duplicate =
                certificateApply(3L, (short) 1, (short) 0, "5");

        when(scoreApplyMapper.selectById(1L)).thenReturn(ok);
        when(scoreApplyMapper.selectById(2L)).thenReturn(alreadyFinal);
        when(scoreApplyMapper.selectById(3L)).thenReturn(duplicate);

        when(
                certificateScoreFinalizeService.finalizeApproval(
                        eq(ok),
                        eq(7L)
                )
        ).thenReturn(null);

        when(
                certificateScoreFinalizeService.finalizeApproval(
                        eq(duplicate),
                        eq(7L)
                )
        ).thenThrow(
                new DuplicateKeyException("dup")
        );

        BatchAuditResultVO result =
                service.batchFinal(
                        List.of(1L, 2L, 3L),
                        7L
                );

        assertEquals(3, result.getTotal());
        assertEquals(1, result.getSuccessCount());
        assertEquals(2, result.getFailCount());

        assertTrue(
                result.getFailures()
                        .stream()
                        .anyMatch(m -> m.contains("已经完成终审"))
        );

        assertTrue(
                result.getFailures()
                        .stream()
                        .anyMatch(m -> m.contains("已经生成正式成绩记录"))
        );
    }

    @Test
    void batchFinalDeduplicatesRepeatedIds() {

        when(scoreApplyMapper.selectById(1L))
                .thenReturn(
                        certificateApply(1L, (short) 1, (short) 0, "2")
                );

        when(
                certificateScoreFinalizeService.finalizeApproval(
                        any(),
                        anyLong()
                )
        ).thenReturn(null);

        BatchAuditResultVO result =
                service.batchFinal(
                        List.of(1L, 1L),
                        7L
                );

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getSuccessCount());

        verify(
                certificateScoreFinalizeService,
                times(1)
        ).finalizeApproval(
                any(),
                anyLong()
        );
    }

    @Test
    void batchFinalGuardsNonCertificateAndPendingPreliminary() {

        ScoreApply nonCertificate =
                certificateApply(1L, (short) 1, (short) 0, "2");

        nonCertificate.setApplyType("ACTIVITY");

        ScoreApply pendingPreliminary =
                certificateApply(2L, (short) 0, (short) 0, "2");

        when(scoreApplyMapper.selectById(1L))
                .thenReturn(nonCertificate);

        when(scoreApplyMapper.selectById(2L))
                .thenReturn(pendingPreliminary);

        BatchAuditResultVO result =
                service.batchFinal(
                        List.of(1L, 2L),
                        7L
                );

        assertEquals(0, result.getSuccessCount());
        assertEquals(2, result.getFailCount());
        verifyNoInteractions(certificateScoreFinalizeService);
    }

    @Test
    void batchFinalGuardsMissingApplyAndInvalidScore() {

        when(scoreApplyMapper.selectById(1L)).thenReturn(null);

        when(scoreApplyMapper.selectById(2L))
                .thenReturn(
                        certificateApply(2L, (short) 1, (short) 0, null)
                );

        BatchAuditResultVO result =
                service.batchFinal(
                        List.of(1L, 2L),
                        7L
                );

        assertEquals(0, result.getSuccessCount());
        assertEquals(2, result.getFailCount());
        verifyNoInteractions(certificateScoreFinalizeService);
    }
}
