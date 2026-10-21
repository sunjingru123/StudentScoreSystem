package com.student.studentscoresystem.service;

import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.ScoreRecordOperationLog;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.ScoreRecordOperationLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * =========================================================
 * 学生成绩记录维护（删除 / 恢复）
 * =========================================================
 *
 * 为什么是「逻辑删除」而不是物理 DELETE：
 *
 * 1. score_record 被以下表外键引用：
 *      score_modify_log
 *      score_record_admin_log
 *      score_record_operation_log
 *    物理删除会直接破坏审计链路（并触发外键错误）。
 *
 * 2. 项目既有设计里已经用 status 表达「有效 / 作废」：
 *      status = 1 有效
 *      status = 0 作废
 *
 * 因此删除动作执行：
 *
 *      status       -> 0   （作废，不再参与任何成绩统计）
 *      admin_hidden -> 1   （管理员 / 辅导员 / 学生视图全部不再返回）
 *
 * 并写入 score_record_operation_log（operation = DELETE）。
 *
 * 效果与「删除这一条加减分」一致：
 *
 * 学生端成绩、辅导员端成绩、统计、导出都不再包含该记录。
 * =========================================================
 */
@Service
public class ScoreRecordManageService {

    /**
     * 操作日志：删除
     */
    public static final String OPERATION_DELETE = "DELETE";

    /**
     * 操作日志：恢复
     */
    public static final String OPERATION_RESTORE = "RESTORE";


    private final ScoreRecordMapper scoreRecordMapper;

    private final ScoreRecordOperationLogMapper operationLogMapper;

    private final ScoreManagerPermissionService permissionService;


    public ScoreRecordManageService(
            ScoreRecordMapper scoreRecordMapper,
            ScoreRecordOperationLogMapper operationLogMapper,
            ScoreManagerPermissionService permissionService
    ) {

        this.scoreRecordMapper = scoreRecordMapper;

        this.operationLogMapper = operationLogMapper;

        this.permissionService = permissionService;

    }


    /**
     * =====================================================
     * 删除一条学生成绩记录
     * =====================================================
     *
     * 权限：
     *   管理员 / 档案部部长 / 档案部副部长
     *
     * 事务：
     *   状态更新与审计日志必须同时成功或同时回滚。
     *
     * 抛出：
     *   SecurityException      没有权限
     *   IllegalArgumentException 参数 / 数据校验失败
     *   IllegalStateException  数据库写入失败
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(
            Long recordId,
            Long operatorId,
            String reason
    ) {

        ScoreRecord record =
                requireManagedRecord(
                        recordId,
                        operatorId
                );


        if (isDeleted(record)) {

            throw new IllegalArgumentException(
                    "该成绩记录已经删除"
            );
        }


        /*
         * 逻辑删除
         */
        record.setStatus((short) 0);

        record.setAdminHidden((short) 1);


        if (scoreRecordMapper.updateById(record) != 1) {

            throw new IllegalStateException(
                    "删除成绩记录失败"
            );
        }


        writeOperationLog(
                recordId,
                operatorId,
                OPERATION_DELETE,
                reason
        );
    }


    /**
     * =====================================================
     * 恢复一条被删除 / 隐藏的学生成绩记录
     * =====================================================
     *
     * 只恢复「作废」或「隐藏」的记录，
     * 正常记录直接返回业务错误，避免无意义的写操作。
     */
    @Transactional(rollbackFor = Exception.class)
    public void restoreRecord(
            Long recordId,
            Long operatorId,
            String reason
    ) {

        ScoreRecord record =
                requireManagedRecord(
                        recordId,
                        operatorId
                );


        boolean deleted = isDeleted(record);

        boolean hidden =
                record.getAdminHidden() != null
                        && record.getAdminHidden() == 1;


        if (!deleted && !hidden) {

            throw new IllegalArgumentException(
                    "该成绩记录状态正常，无需恢复"
            );
        }


        record.setStatus((short) 1);

        record.setAdminHidden((short) 0);


        if (scoreRecordMapper.updateById(record) != 1) {

            throw new IllegalStateException(
                    "恢复成绩记录失败"
            );
        }


        writeOperationLog(
                recordId,
                operatorId,
                OPERATION_RESTORE,
                reason
        );
    }


    /**
     * 校验登录态、权限与记录存在性
     */
    private ScoreRecord requireManagedRecord(
            Long recordId,
            Long operatorId
    ) {

        if (recordId == null) {

            throw new IllegalArgumentException(
                    "成绩记录ID不能为空"
            );
        }


        if (operatorId == null) {

            throw new IllegalArgumentException(
                    "请先登录"
            );
        }


        if (!permissionService.canManageStudentScores(operatorId)) {

            throw new SecurityException(
                    "没有权限维护学生成绩"
            );
        }


        ScoreRecord record =
                scoreRecordMapper.selectById(recordId);


        if (record == null) {

            throw new IllegalArgumentException(
                    "成绩记录不存在"
            );
        }


        return record;
    }


    /**
     * status = 0 视为已删除 / 已作废
     */
    private boolean isDeleted(ScoreRecord record) {

        return record.getStatus() != null
                && record.getStatus() == 0;
    }


    /**
     * 写入成绩操作审计日志
     */
    private void writeOperationLog(
            Long recordId,
            Long operatorId,
            String operation,
            String reason
    ) {

        ScoreRecordOperationLog log =
                new ScoreRecordOperationLog();


        log.setScoreRecordId(recordId);

        log.setOperatorId(operatorId);

        log.setOperation(operation);

        log.setReason(
                reason == null || reason.isBlank()
                        ? null
                        : reason.trim()
        );

        log.setCreateTime(LocalDateTime.now());


        if (operationLogMapper.insert(log) != 1) {

            throw new IllegalStateException(
                    "成绩操作日志写入失败"
            );
        }
    }

}
