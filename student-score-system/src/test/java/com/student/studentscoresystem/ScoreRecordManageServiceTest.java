package com.student.studentscoresystem;

import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.ScoreRecordOperationLog;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.ScoreRecordOperationLogMapper;
import com.student.studentscoresystem.service.ScoreManagerPermissionService;
import com.student.studentscoresystem.service.ScoreRecordManageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * =========================================================
 * 学生成绩删除 / 恢复回归测试
 * =========================================================
 *
 * 删除采用逻辑删除：
 *
 *   status       -> 0
 *   admin_hidden -> 1
 *
 * 并写入审计日志（operation = DELETE）。
 * =========================================================
 */
class ScoreRecordManageServiceTest {

    private static final long RECORD_ID = 500L;
    private static final long OPERATOR_ID = 3L;

    private ScoreRecordMapper scoreRecordMapper;
    private ScoreRecordOperationLogMapper operationLogMapper;
    private ScoreManagerPermissionService permissionService;

    private ScoreRecordManageService service;

    @BeforeEach
    void setUp() {

        scoreRecordMapper = mock(ScoreRecordMapper.class);
        operationLogMapper = mock(ScoreRecordOperationLogMapper.class);
        permissionService = mock(ScoreManagerPermissionService.class);

        service = new ScoreRecordManageService(
                scoreRecordMapper,
                operationLogMapper,
                permissionService
        );
    }

    private ScoreRecord record(Short status, Short adminHidden) {

        ScoreRecord record = new ScoreRecord();
        record.setId(RECORD_ID);
        record.setStudentId(100L);
        record.setScore(new BigDecimal("1.5"));
        record.setStatus(status);
        record.setAdminHidden(adminHidden);

        return record;
    }

    @Test
    void managerCanDeleteRecord() {

        when(permissionService.canManageStudentScores(OPERATOR_ID)).thenReturn(true);
        when(scoreRecordMapper.selectById(RECORD_ID)).thenReturn(record((short) 1, (short) 0));
        when(scoreRecordMapper.updateById(any(ScoreRecord.class))).thenReturn(1);
        when(operationLogMapper.insert(any(ScoreRecordOperationLog.class))).thenReturn(1);

        service.deleteRecord(RECORD_ID, OPERATOR_ID, "重复上报");

        ArgumentCaptor<ScoreRecord> recordCaptor =
                ArgumentCaptor.forClass(ScoreRecord.class);

        verify(scoreRecordMapper).updateById(recordCaptor.capture());

        assertEquals(0, recordCaptor.getValue().getStatus().intValue());
        assertEquals(1, recordCaptor.getValue().getAdminHidden().intValue());

        ArgumentCaptor<ScoreRecordOperationLog> logCaptor =
                ArgumentCaptor.forClass(ScoreRecordOperationLog.class);

        verify(operationLogMapper).insert(logCaptor.capture());

        assertEquals(
                ScoreRecordManageService.OPERATION_DELETE,
                logCaptor.getValue().getOperation()
        );
        assertEquals(RECORD_ID, logCaptor.getValue().getScoreRecordId().longValue());
        assertEquals(OPERATOR_ID, logCaptor.getValue().getOperatorId().longValue());
    }

    @Test
    void userWithoutPermissionCannotDelete() {

        when(permissionService.canManageStudentScores(OPERATOR_ID)).thenReturn(false);

        assertThrows(
                SecurityException.class,
                () -> service.deleteRecord(RECORD_ID, OPERATOR_ID, null)
        );

        verify(scoreRecordMapper, never()).selectById(any());
        verify(scoreRecordMapper, never()).updateById(any(ScoreRecord.class));
        verify(operationLogMapper, never()).insert(any(ScoreRecordOperationLog.class));
    }

    @Test
    void anonymousCannotDelete() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.deleteRecord(RECORD_ID, null, null)
        );

        verify(scoreRecordMapper, never()).updateById(any(ScoreRecord.class));
    }

    @Test
    void missingRecordCannotBeDeleted() {

        when(permissionService.canManageStudentScores(OPERATOR_ID)).thenReturn(true);
        when(scoreRecordMapper.selectById(RECORD_ID)).thenReturn(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.deleteRecord(RECORD_ID, OPERATOR_ID, null)
        );

        verify(scoreRecordMapper, never()).updateById(any(ScoreRecord.class));
    }

    @Test
    void alreadyDeletedRecordIsRejected() {

        when(permissionService.canManageStudentScores(OPERATOR_ID)).thenReturn(true);
        when(scoreRecordMapper.selectById(RECORD_ID)).thenReturn(record((short) 0, (short) 1));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.deleteRecord(RECORD_ID, OPERATOR_ID, null)
        );

        verify(scoreRecordMapper, never()).updateById(any(ScoreRecord.class));
    }

    @Test
    void failedUpdateFailsWholeOperation() {

        when(permissionService.canManageStudentScores(OPERATOR_ID)).thenReturn(true);
        when(scoreRecordMapper.selectById(RECORD_ID)).thenReturn(record((short) 1, (short) 0));
        when(scoreRecordMapper.updateById(any(ScoreRecord.class))).thenReturn(0);

        assertThrows(
                IllegalStateException.class,
                () -> service.deleteRecord(RECORD_ID, OPERATOR_ID, null)
        );

        /*
         * 状态更新失败时不允许写入审计日志，
         * 保证两端一致（事务回滚）。
         */
        verify(operationLogMapper, never()).insert(any(ScoreRecordOperationLog.class));
    }

    @Test
    void failedLogInsertFailsWholeOperation() {

        when(permissionService.canManageStudentScores(OPERATOR_ID)).thenReturn(true);
        when(scoreRecordMapper.selectById(RECORD_ID)).thenReturn(record((short) 1, (short) 0));
        when(scoreRecordMapper.updateById(any(ScoreRecord.class))).thenReturn(1);
        when(operationLogMapper.insert(any(ScoreRecordOperationLog.class))).thenReturn(0);

        /*
         * 抛异常后由事务统一回滚，不会留下“已删除但没有日志”的数据。
         */
        assertThrows(
                IllegalStateException.class,
                () -> service.deleteRecord(RECORD_ID, OPERATOR_ID, null)
        );
    }

    @Test
    void hiddenRecordCanBeRestored() {

        when(permissionService.canManageStudentScores(OPERATOR_ID)).thenReturn(true);
        when(scoreRecordMapper.selectById(RECORD_ID)).thenReturn(record((short) 1, (short) 1));
        when(scoreRecordMapper.updateById(any(ScoreRecord.class))).thenReturn(1);
        when(operationLogMapper.insert(any(ScoreRecordOperationLog.class))).thenReturn(1);

        service.restoreRecord(RECORD_ID, OPERATOR_ID, null);

        ArgumentCaptor<ScoreRecord> recordCaptor =
                ArgumentCaptor.forClass(ScoreRecord.class);

        verify(scoreRecordMapper).updateById(recordCaptor.capture());

        assertEquals(1, recordCaptor.getValue().getStatus().intValue());
        assertEquals(0, recordCaptor.getValue().getAdminHidden().intValue());
    }

    @Test
    void deletedRecordCanBeRestored() {

        when(permissionService.canManageStudentScores(OPERATOR_ID)).thenReturn(true);
        when(scoreRecordMapper.selectById(RECORD_ID)).thenReturn(record((short) 0, (short) 1));
        when(scoreRecordMapper.updateById(any(ScoreRecord.class))).thenReturn(1);
        when(operationLogMapper.insert(any(ScoreRecordOperationLog.class))).thenReturn(1);

        service.restoreRecord(RECORD_ID, OPERATOR_ID, null);

        verify(scoreRecordMapper).updateById(any(ScoreRecord.class));
    }

    @Test
    void normalRecordCannotBeRestored() {

        when(permissionService.canManageStudentScores(OPERATOR_ID)).thenReturn(true);
        when(scoreRecordMapper.selectById(RECORD_ID)).thenReturn(record((short) 1, (short) 0));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.restoreRecord(RECORD_ID, OPERATOR_ID, null)
        );

        verify(scoreRecordMapper, never()).updateById(any(ScoreRecord.class));
    }
}
