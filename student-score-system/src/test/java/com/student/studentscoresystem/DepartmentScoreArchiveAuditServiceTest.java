package com.student.studentscoresystem;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.mapper.ScoreRuleMapper;
import com.student.studentscoresystem.service.DepartmentScoreArchiveAuditService;
import com.student.studentscoresystem.service.IDepartmentScoreApplyService;
import com.student.studentscoresystem.service.ScoreManagerPermissionService;
import com.student.studentscoresystem.vo.ArchiveScoreOptionsVO;
import com.student.studentscoresystem.vo.BatchAuditResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 部门加减分申报「档案部审核」环节的单元测试。
 *
 * 重点验证：
 *
 * 1. 只有档案部的部长 / 副部长可以审核；
 * 2. 只有「部门初审已通过 + 档案部尚未审核」的申报可以审核；
 * 3. 通过 / 驳回会正确写入 archiveStatus 及审核人信息；
 * 4. 不能审核自己提交的申报；
 * 5. 一键通过会逐条复用单条审核逻辑；
 * 6. 审核时可调整申报分值，但必须落在正式规则分值集合内。
 */
@ExtendWith(MockitoExtension.class)
class DepartmentScoreArchiveAuditServiceTest {

    @Mock
    private IDepartmentScoreApplyService applyService;

    @Mock
    private ScoreManagerPermissionService scoreManagerPermissionService;

    @Mock
    private ScoreRuleMapper scoreRuleMapper;

    private DepartmentScoreArchiveAuditService service;

    @BeforeEach
    void setUp() {

        service = new DepartmentScoreArchiveAuditService(
                applyService,
                scoreManagerPermissionService,
                scoreRuleMapper
        );
    }

    @Test
    void rejectsWhenNotArchiveLeader() {

        when(scoreManagerPermissionService.isArchiveLeader(7L))
                .thenReturn(false);

        String message =
                service.archiveAudit(
                        10L,
                        7L,
                        (short) 1,
                        "通过"
                );

        assertNotNull(message);
        assertTrue(
                message.contains("档案部"),
                "非档案部负责人应被拦截，实际：" + message
        );

        verify(applyService, never())
                .updateById(any(DepartmentScoreApply.class));
    }

    @Test
    void rejectsWhenDepartmentAuditNotPassed() {

        when(scoreManagerPermissionService.isArchiveLeader(7L))
                .thenReturn(true);

        DepartmentScoreApply apply = pendingApply(10L, 99L);

        apply.setStatus((short) 0);

        when(applyService.getById(10L)).thenReturn(apply);

        String message =
                service.archiveAudit(
                        10L,
                        7L,
                        (short) 1,
                        "通过"
                );

        assertNotNull(message);
        assertTrue(message.contains("部门审核"));

        verify(applyService, never())
                .updateById(any(DepartmentScoreApply.class));
    }

    @Test
    void rejectsWhenAlreadyArchived() {

        when(scoreManagerPermissionService.isArchiveLeader(7L))
                .thenReturn(true);

        DepartmentScoreApply apply = pendingApply(10L, 99L);

        apply.setArchiveStatus((short) 1);

        when(applyService.getById(10L)).thenReturn(apply);

        String message =
                service.archiveAudit(
                        10L,
                        7L,
                        (short) 1,
                        "通过"
                );

        assertNotNull(message);
        assertTrue(message.contains("已经完成档案部审核"));
    }

    @Test
    void rejectsSelfAudit() {

        when(scoreManagerPermissionService.isArchiveLeader(7L))
                .thenReturn(true);

        DepartmentScoreApply apply = pendingApply(10L, 7L);

        when(applyService.getById(10L)).thenReturn(apply);

        String message =
                service.archiveAudit(
                        10L,
                        7L,
                        (short) 1,
                        "通过"
                );

        assertNotNull(message);
        assertTrue(message.contains("自己"));
    }

    @Test
    void passesAndWritesArchiveFields() {

        when(scoreManagerPermissionService.isArchiveLeader(7L))
                .thenReturn(true);

        DepartmentScoreApply apply = pendingApply(10L, 99L);

        when(applyService.getById(10L)).thenReturn(apply);

        when(applyService.updateById(any(DepartmentScoreApply.class)))
                .thenReturn(true);

        String message =
                service.archiveAudit(
                        10L,
                        7L,
                        (short) 1,
                        "材料齐全"
                );

        assertNull(message);

        DepartmentScoreApply updated =
                captureUpdate();

        assertEquals((short) 1, updated.getArchiveStatus());
        assertEquals(7L, updated.getArchiveReviewerId());
        assertEquals("材料齐全", updated.getArchiveReviewRemark());
        assertNotNull(updated.getArchiveReviewTime());
        assertNotNull(updated.getUpdateTime());
    }

    @Test
    void rejectsAndWritesArchiveStatusTwo() {

        when(scoreManagerPermissionService.isArchiveLeader(7L))
                .thenReturn(true);

        when(applyService.getById(10L))
                .thenReturn(pendingApply(10L, 99L));

        when(applyService.updateById(any(DepartmentScoreApply.class)))
                .thenReturn(true);

        String message =
                service.archiveAudit(
                        10L,
                        7L,
                        (short) 2,
                        "材料不足"
                );

        assertNull(message);

        assertEquals(
                (short) 2,
                captureUpdate().getArchiveStatus()
        );
    }

    @Test
    void batchPassApprovesEachPendingApply() {

        when(scoreManagerPermissionService.isArchiveLeader(7L))
                .thenReturn(true);

        Wrapper<DepartmentScoreApply> anyWrapper =
                any();

        when(applyService.list(anyWrapper))
                .thenReturn(
                        List.of(
                                pendingApply(10L, 99L),
                                pendingApply(11L, 99L)
                        )
                );

        when(applyService.getById(10L))
                .thenReturn(pendingApply(10L, 99L));

        when(applyService.getById(11L))
                .thenReturn(pendingApply(11L, 99L));

        when(applyService.updateById(any(DepartmentScoreApply.class)))
                .thenReturn(true);

        BatchAuditResultVO result =
                service.batchPass(7L);

        assertEquals(2, result.getTotal());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
    }

    @Test
    void batchPassIsDeniedWhenNotArchiveLeader() {

        when(scoreManagerPermissionService.isArchiveLeader(7L))
                .thenReturn(false);

        BatchAuditResultVO result =
                service.batchPass(7L);

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailCount());

        verify(applyService, never())
                .updateById(any(DepartmentScoreApply.class));
    }

    @Test
    void updateScoreAllowsValueOutsideFormalRules() {

        when(scoreManagerPermissionService.isArchiveLeader(7L))
                .thenReturn(true);

        when(applyService.getById(10L))
                .thenReturn(pendingApply(10L, 99L));

        when(applyService.updateById(any(DepartmentScoreApply.class)))
                .thenReturn(true);

        String message =
                service.updateScore(
                        10L,
                        7L,
                        new BigDecimal("0.7")
                );

        assertNull(
                message,
                "档案部应能自由填写分值，实际：" + message
        );

        assertEquals(
                0,
                captureUpdate().getScore()
                        .compareTo(new BigDecimal("0.7"))
        );

        /*
         * 调整分值不再受正式规则约束，不应再查询 score_rule
         */
        verifyNoInteractions(
                scoreRuleMapper
        );
    }

    @Test
    void updateScoreAcceptsFormalRuleValue() {

        when(scoreManagerPermissionService.isArchiveLeader(7L))
                .thenReturn(true);

        when(applyService.getById(10L))
                .thenReturn(pendingApply(10L, 99L));

        when(applyService.updateById(any(DepartmentScoreApply.class)))
                .thenReturn(true);

        String message =
                service.updateScore(
                        10L,
                        7L,
                        new BigDecimal("0.5")
                );

        assertNull(message);

        DepartmentScoreApply updated =
                captureUpdate();

        assertEquals(
                0,
                updated.getScore().compareTo(new BigDecimal("0.5"))
        );
        assertNotNull(updated.getUpdateTime());
    }

    @Test
    void updateScoreAllowsFreeValueWhenNoFormalRules() {

        when(scoreManagerPermissionService.isArchiveLeader(7L))
                .thenReturn(true);

        when(applyService.getById(10L))
                .thenReturn(pendingApply(10L, 99L));

        when(applyService.updateById(any(DepartmentScoreApply.class)))
                .thenReturn(true);

        String message =
                service.updateScore(
                        10L,
                        7L,
                        new BigDecimal("0.3")
                );

        assertNull(message);

        assertEquals(
                0,
                captureUpdate().getScore()
                        .compareTo(new BigDecimal("0.3"))
        );
    }

    @Test
    void updateScoreRejectsNonPositive() {

        when(scoreManagerPermissionService.isArchiveLeader(7L))
                .thenReturn(true);

        when(applyService.getById(10L))
                .thenReturn(pendingApply(10L, 99L));

        String message =
                service.updateScore(
                        10L,
                        7L,
                        BigDecimal.ZERO
                );

        assertNotNull(message);
        assertTrue(message.contains("大于 0"));

        verify(applyService, never())
                .updateById(any(DepartmentScoreApply.class));
    }

    @Test
    void updateScoreRejectsTooManyDecimals() {

        when(scoreManagerPermissionService.isArchiveLeader(7L))
                .thenReturn(true);

        when(applyService.getById(10L))
                .thenReturn(pendingApply(10L, 99L));

        String message =
                service.updateScore(
                        10L,
                        7L,
                        new BigDecimal("0.555")
                );

        assertNotNull(message);
        assertTrue(message.contains("两位小数"));

        verify(applyService, never())
                .updateById(any(DepartmentScoreApply.class));
    }

    @Test
    void updateScoreDeniedWhenNotArchiveLeader() {

        when(scoreManagerPermissionService.isArchiveLeader(7L))
                .thenReturn(false);

        String message =
                service.updateScore(
                        10L,
                        7L,
                        new BigDecimal("1")
                );

        assertNotNull(message);
        assertTrue(message.contains("档案部"));

        verify(applyService, never())
                .updateById(any(DepartmentScoreApply.class));
    }

    @Test
    void updateScoreRejectsAlreadyArchived() {

        when(scoreManagerPermissionService.isArchiveLeader(7L))
                .thenReturn(true);

        DepartmentScoreApply apply =
                pendingApply(10L, 99L);

        apply.setArchiveStatus((short) 1);

        when(applyService.getById(10L))
                .thenReturn(apply);

        String message =
                service.updateScore(
                        10L,
                        7L,
                        new BigDecimal("1")
                );

        assertNotNull(message);
        assertTrue(message.contains("已经完成档案部审核"));

        verify(applyService, never())
                .updateById(any(DepartmentScoreApply.class));
    }

    @Test
    void scoreOptionsReturnsSuggestedScores() {

        when(applyService.getById(10L))
                .thenReturn(pendingApply(10L, 99L));

        stubFormalRules(
                List.of(
                        rule(1L, (short) 1, "1"),
                        rule(2L, (short) 1, "0.5"),
                        rule(3L, (short) -1, "1")
                )
        );

        ArchiveScoreOptionsVO options =
                service.scoreOptions(10L);

        assertEquals(2, options.getSuggestedScores().size());

        assertEquals(
                0,
                options.getSuggestedScores().get(0)
                        .compareTo(new BigDecimal("0.5"))
        );

        assertEquals(
                0,
                options.getSuggestedScores().get(1)
                        .compareTo(new BigDecimal("1"))
        );

        assertEquals((short) 1, options.getScoreType());
    }

    private DepartmentScoreApply pendingApply(
            Long id,
            Long applicantId) {

        DepartmentScoreApply apply =
                new DepartmentScoreApply();

        apply.setId(id);
        apply.setStudentId(100L);
        apply.setDepartmentId(5L);
        apply.setApplicantId(applicantId);
        apply.setStatus((short) 1);
        apply.setFinalStatus((short) 0);
        apply.setArchiveStatus((short) 0);
        apply.setTitle("教室卫生");
        apply.setScoreType((short) 1);
        apply.setScore(new BigDecimal("1.0"));

        return apply;
    }

    /**
     * 构造一条正式规则：同部门、同名称、指定类型与分值
     */
    private ScoreRule rule(
            Long id,
            Short scoreType,
            String score) {

        ScoreRule rule =
                new ScoreRule();

        rule.setId(id);
        rule.setDepartmentId(5L);
        rule.setName("教室卫生");
        rule.setScoreType(scoreType);
        rule.setScore(new BigDecimal(score));
        rule.setStatus((short) 1);

        return rule;
    }

    /**
     * 对 scoreRuleMapper.selectList 的 stub，避免 Wrapper / IPage 重载歧义
     */
    private void stubFormalRules(
            List<ScoreRule> rules) {

        Wrapper<ScoreRule> anyWrapper =
                any();

        when(scoreRuleMapper.selectList(anyWrapper))
                .thenReturn(rules);
    }

    private DepartmentScoreApply captureUpdate() {

        ArgumentCaptor<DepartmentScoreApply> captor =
                ArgumentCaptor.forClass(DepartmentScoreApply.class);

        verify(applyService).updateById(captor.capture());

        return captor.getValue();
    }
}
