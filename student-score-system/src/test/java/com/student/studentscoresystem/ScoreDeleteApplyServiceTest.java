package com.student.studentscoresystem;

import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.ScoreRecordDeleteApply;
import com.student.studentscoresystem.mapper.ScoreRecordDeleteApplyMapper;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.ScoreDeleteApplyService;
import com.student.studentscoresystem.service.ScoreManagerPermissionService;
import com.student.studentscoresystem.service.ScoreProjectNameResolver;
import com.student.studentscoresystem.service.ScoreRecordManageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * =========================================================
 * 成绩删除申请流程回归测试
 * =========================================================
 *
 * 覆盖：
 *   1. 只有档案部部长 / 副部长可以提交删除申请；
 *   2. 同一条成绩不能重复提交待审核申请；
 *   3. 只有审核人可以审核；
 *   4. 审核通过复用既有删除逻辑并标记为已通过；
 *   5. 驳回不触发删除。
 * =========================================================
 */
class ScoreDeleteApplyServiceTest {

    private static final long APPLICANT_ID = 10L;

    private static final long REVIEWER_ID = 20L;

    private static final long STUDENT_ID = 30L;

    private static final long RECORD_ID = 300L;

    private static final long APPLY_ID = 900L;

    private ScoreRecordDeleteApplyMapper applyMapper;

    private ScoreRecordMapper scoreRecordMapper;

    private SysUserMapper sysUserMapper;

    private ScoreManagerPermissionService permissionService;

    private ScoreRecordManageService scoreRecordManageService;

    private ScoreDeleteApplyService service;

    @BeforeEach
    void setUp() {

        applyMapper = mock(ScoreRecordDeleteApplyMapper.class);

        scoreRecordMapper = mock(ScoreRecordMapper.class);

        sysUserMapper = mock(SysUserMapper.class);

        permissionService = mock(ScoreManagerPermissionService.class);

        scoreRecordManageService = mock(ScoreRecordManageService.class);

        ScoreProjectNameResolver resolver =
                mock(ScoreProjectNameResolver.class);

        service = new ScoreDeleteApplyService(
                applyMapper,
                scoreRecordMapper,
                sysUserMapper,
                permissionService,
                scoreRecordManageService,
                resolver
        );
    }

    private ScoreRecord record(long id) {

        ScoreRecord record = new ScoreRecord();

        record.setId(id);
        record.setStudentId(STUDENT_ID);
        record.setScore(new BigDecimal("2"));
        record.setStatus((short) 1);

        return record;
    }

    private ScoreRecordDeleteApply pendingApply() {

        ScoreRecordDeleteApply apply =
                new ScoreRecordDeleteApply();

        apply.setId(APPLY_ID);
        apply.setScoreRecordId(RECORD_ID);
        apply.setStudentId(STUDENT_ID);
        apply.setApplicantId(APPLICANT_ID);
        apply.setStatus(ScoreDeleteApplyService.STATUS_PENDING);

        return apply;
    }

    @Test
    void nonArchiveLeaderCannotSubmit() {

        when(permissionService.canSubmitScoreDeleteApply(APPLICANT_ID))
                .thenReturn(false);

        assertThrows(
                SecurityException.class,
                () -> service.submitApply(RECORD_ID, APPLICANT_ID, "误加分")
        );

        verify(applyMapper, never()).insert(any(ScoreRecordDeleteApply.class));
    }

    @Test
    void archiveLeaderCanSubmitPendingApply() {

        when(permissionService.canSubmitScoreDeleteApply(APPLICANT_ID))
                .thenReturn(true);

        when(scoreRecordMapper.selectById(RECORD_ID))
                .thenReturn(record(RECORD_ID));

        when(applyMapper.selectCount(any()))
                .thenReturn(0L);

        when(applyMapper.insert(any(ScoreRecordDeleteApply.class)))
                .thenAnswer(invocation -> {

                    ScoreRecordDeleteApply inserted =
                            invocation.getArgument(0);

                    inserted.setId(APPLY_ID);

                    return 1;

                });

        assertEquals(
                APPLY_ID,
                service.submitApply(RECORD_ID, APPLICANT_ID, "误加分")
        );

        verify(applyMapper).insert(any(ScoreRecordDeleteApply.class));
    }

    @Test
    void duplicatePendingApplyIsRejected() {

        when(permissionService.canSubmitScoreDeleteApply(APPLICANT_ID))
                .thenReturn(true);

        when(scoreRecordMapper.selectById(RECORD_ID))
                .thenReturn(record(RECORD_ID));

        when(applyMapper.selectCount(any()))
                .thenReturn(1L);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.submitApply(RECORD_ID, APPLICANT_ID, null)
        );

        verify(applyMapper, never()).insert(any(ScoreRecordDeleteApply.class));
    }

    @Test
    void reviewerApprovesAndDeletesRecord() {

        when(permissionService.canReviewScoreDeleteApply(REVIEWER_ID))
                .thenReturn(true);

        when(applyMapper.selectById(APPLY_ID))
                .thenReturn(pendingApply());

        when(scoreRecordMapper.selectById(RECORD_ID))
                .thenReturn(record(RECORD_ID));

        when(applyMapper.updateById(any(ScoreRecordDeleteApply.class)))
                .thenReturn(1);

        service.approve(APPLY_ID, REVIEWER_ID, "同意删除");

        verify(scoreRecordManageService)
                .deleteRecord(eq(RECORD_ID), eq(REVIEWER_ID), any());

        verify(applyMapper).updateById(
                org.mockito.ArgumentMatchers
                        .<ScoreRecordDeleteApply>argThat(
                                apply -> apply.getStatus()
                                        == ScoreDeleteApplyService.STATUS_APPROVED
                        )
        );
    }

    @Test
    void nonReviewerCannotApprove() {

        when(permissionService.canReviewScoreDeleteApply(anyLong()))
                .thenReturn(false);

        assertThrows(
                SecurityException.class,
                () -> service.approve(APPLY_ID, REVIEWER_ID, null)
        );

        verify(scoreRecordManageService, never())
                .deleteRecord(anyLong(), anyLong(), any());
    }

    @Test
    void rejectDoesNotDeleteRecord() {

        when(permissionService.canReviewScoreDeleteApply(REVIEWER_ID))
                .thenReturn(true);

        when(applyMapper.selectById(APPLY_ID))
                .thenReturn(pendingApply());

        when(applyMapper.updateById(any(ScoreRecordDeleteApply.class)))
                .thenReturn(1);

        service.reject(APPLY_ID, REVIEWER_ID, "证据不足");

        verify(scoreRecordManageService, never())
                .deleteRecord(anyLong(), anyLong(), any());

        verify(applyMapper).updateById(
                org.mockito.ArgumentMatchers
                        .<ScoreRecordDeleteApply>argThat(
                                apply -> apply.getStatus()
                                        == ScoreDeleteApplyService.STATUS_REJECTED
                        )
        );
    }

    @Test
    void alreadyProcessedApplyCannotBeReviewedAgain() {

        when(permissionService.canReviewScoreDeleteApply(REVIEWER_ID))
                .thenReturn(true);

        ScoreRecordDeleteApply processed = pendingApply();

        processed.setStatus(ScoreDeleteApplyService.STATUS_APPROVED);

        when(applyMapper.selectById(APPLY_ID))
                .thenReturn(processed);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.approve(APPLY_ID, REVIEWER_ID, null)
        );

        verify(scoreRecordManageService, never())
                .deleteRecord(anyLong(), anyLong(), any());
    }
}
