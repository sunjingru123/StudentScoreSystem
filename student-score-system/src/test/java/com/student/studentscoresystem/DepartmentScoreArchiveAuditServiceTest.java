package com.student.studentscoresystem;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.service.DepartmentScoreArchiveAuditService;
import com.student.studentscoresystem.service.IDepartmentScoreApplyService;
import com.student.studentscoresystem.service.ScoreManagerPermissionService;
import com.student.studentscoresystem.vo.BatchAuditResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
 * 5. 一键通过会逐条复用单条审核逻辑。
 */
@ExtendWith(MockitoExtension.class)
class DepartmentScoreArchiveAuditServiceTest {

    @Mock
    private IDepartmentScoreApplyService applyService;

    @Mock
    private ScoreManagerPermissionService scoreManagerPermissionService;

    private DepartmentScoreArchiveAuditService service;

    @BeforeEach
    void setUp() {

        service = new DepartmentScoreArchiveAuditService(
                applyService,
                scoreManagerPermissionService
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

        return apply;
    }

    private DepartmentScoreApply captureUpdate() {

        ArgumentCaptor<DepartmentScoreApply> captor =
                ArgumentCaptor.forClass(DepartmentScoreApply.class);

        verify(applyService).updateById(captor.capture());

        return captor.getValue();
    }
}
