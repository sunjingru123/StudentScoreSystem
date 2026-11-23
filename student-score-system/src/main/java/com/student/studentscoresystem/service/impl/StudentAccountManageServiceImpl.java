package com.student.studentscoresystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.student.studentscoresystem.entity.OperationLog;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.OperationLogMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.StudentAccountManageService;
import com.student.studentscoresystem.vo.RecycledStudentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.student.studentscoresystem.utils.ClassNameUtils;

/**
 * 学生账号软删除（回收站）实现。
 *
 * 说明：
 *
 * 1. 物理上只操作 sys_user 的 deleted / deleted_time / deleted_by /
 *    delete_reason 四列，绝不删除学生本人及其任何业务数据；
 * 2. 删除时顺带清空 active_session_id，
 *    让该账号已签发的 Token 立即失效（复用现有单账号登录机制）；
 * 3. 每次删除 / 恢复都会写一条 operation_log，复用现有日志表。
 */
@Service
public class StudentAccountManageServiceImpl
        implements StudentAccountManageService {

    /**
     * 学生岗位名称（与 SysUserController 的学生列表口径一致）
     */
    private static final String STUDENT_POSITION_NAME = "学生";

    private final SysUserMapper userMapper;

    private final SysUserPositionMapper userPositionMapper;

    private final SysPositionMapper positionMapper;

    private final OperationLogMapper operationLogMapper;

    public StudentAccountManageServiceImpl(
            SysUserMapper userMapper,
            SysUserPositionMapper userPositionMapper,
            SysPositionMapper positionMapper,
            OperationLogMapper operationLogMapper
    ) {

        this.userMapper =
                userMapper;

        this.userPositionMapper =
                userPositionMapper;

        this.positionMapper =
                positionMapper;

        this.operationLogMapper =
                operationLogMapper;
    }

    @Override
    public Page<RecycledStudentVO> pageRecycledStudents(
            long pageNum,
            long pageSize,
            String keyword,
            String className
    ) {

        if (pageNum < 1) {

            pageNum = 1;
        }

        if (pageSize < 1) {

            pageSize = 10;
        }

        if (pageSize > 100) {

            pageSize = 100;
        }

        LambdaQueryWrapper<SysUser> wrapper =
                new LambdaQueryWrapper<>();

        wrapper.eq(
                SysUser::getDeleted,
                (short) 1
        );

        String key =
                trimToNull(
                        keyword
                );

        if (key != null) {

            wrapper.and(
                    condition ->
                            condition
                                    .like(
                                            SysUser::getStudentNo,
                                            key
                                    )
                                    .or()
                                    .like(
                                            SysUser::getRealName,
                                            key
                                    )
                                    .or()
                                    .like(
                                            SysUser::getUsername,
                                            key
                                    )
                                    .or()
                                    .like(
                                            SysUser::getClassName,
                                            key
                                    )
            );
        }

        String classKey =
                trimToNull(
                        className
                );

        if (classKey != null) {

            wrapper.eq(
                    SysUser::getClassName,
                    classKey
            );
        }

        /*
         * 最近删除的排在前面，方便管理员找回。
         */
        wrapper.orderByDesc(
                SysUser::getDeletedTime
        );

        wrapper.orderByAsc(
                SysUser::getStudentNo
        );

        Page<SysUser> resultPage =
                userMapper.selectPage(
                        new Page<>(
                                pageNum,
                                pageSize
                        ),
                        wrapper
                );

        Page<RecycledStudentVO> voPage =
                new Page<>(
                        resultPage.getCurrent(),
                        resultPage.getSize(),
                        resultPage.getTotal()
                );

        List<SysUser> records =
                resultPage.getRecords();

        if (records == null
                || records.isEmpty()) {

            voPage.setRecords(
                    new ArrayList<>()
            );

            return voPage;
        }

        /*
         * 批量预取操作管理员姓名，避免逐条查询（N+1）。
         */
        Set<Long> operatorIds =
                new HashSet<>();

        for (SysUser user : records) {

            if (user == null
                    || user.getDeletedBy() == null) {

                continue;
            }

            operatorIds.add(
                    user.getDeletedBy()
            );
        }

        Map<Long, String> operatorNames =
                loadOperatorNames(
                        operatorIds
                );

        List<RecycledStudentVO> voList =
                new ArrayList<>();

        for (SysUser user : records) {

            if (user == null) {

                continue;
            }

            RecycledStudentVO vo =
                    new RecycledStudentVO();

            vo.setId(
                    user.getId()
            );

            vo.setStudentNo(
                    user.getStudentNo()
            );

            vo.setUsername(
                    user.getUsername()
            );

            vo.setRealName(
                    user.getRealName()
            );

            vo.setClassName(
                    ClassNameUtils.normalize(
                            user.getClassName()
                    )
            );

            vo.setPhone(
                    user.getPhone()
            );

            vo.setStatus(
                    user.getStatus()
            );

            vo.setDeletedTime(
                    user.getDeletedTime()
            );

            vo.setDeletedBy(
                    user.getDeletedBy()
            );

            vo.setDeleteReason(
                    user.getDeleteReason()
            );

            if (user.getDeletedBy() != null) {

                vo.setDeletedByName(
                        operatorNames.get(
                                user.getDeletedBy()
                        )
                );
            }

            voList.add(
                    vo
            );
        }

        voPage.setRecords(
                voList
        );

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDeleteStudent(
            Long studentId,
            String reason,
            Long operatorId
    ) {

        if (studentId == null) {

            throw new IllegalArgumentException(
                    "请选择要删除的学生"
            );
        }

        String deleteReason =
                trimToNull(
                        reason
                );

        if (deleteReason == null) {

            throw new IllegalArgumentException(
                    "请填写删除原因"
            );
        }

        if (deleteReason.length() > 500) {

            throw new IllegalArgumentException(
                    "删除原因不能超过 500 个字符"
            );
        }

        if (operatorId == null) {

            throw new IllegalArgumentException(
                    "无法识别当前操作管理员，请重新登录"
            );
        }

        SysUser user =
                userMapper.selectById(
                        studentId
                );

        if (user == null) {

            throw new IllegalArgumentException(
                    "学生不存在"
            );
        }

        if (isDeleted(user)) {

            throw new IllegalArgumentException(
                    "该学生已在回收站中，无需重复删除"
            );
        }

        /*
         * 只能删除学生账号：
         * 管理员、辅导员及其他组织人员一律拒绝。
         */
        if (!isStudentUser(studentId)) {

            throw new IllegalArgumentException(
                    "只能删除学生账号"
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        LambdaUpdateWrapper<SysUser> wrapper =
                new LambdaUpdateWrapper<>();

        wrapper.eq(
                        SysUser::getId,
                        studentId
                )
                .set(
                        SysUser::getDeleted,
                        (short) 1
                )
                .set(
                        SysUser::getDeletedTime,
                        now
                )
                .set(
                        SysUser::getDeletedBy,
                        operatorId
                )
                .set(
                        SysUser::getDeleteReason,
                        deleteReason
                )
                /*
                 * 清空会话，令该账号已签发的 Token 立即失效，
                 * 已删除的学生无法继续访问任何业务接口。
                 */
                .set(
                        SysUser::getActiveSessionId,
                        null
                );

        int updated =
                userMapper.update(
                        null,
                        wrapper
                );

        if (updated <= 0) {

            throw new IllegalStateException(
                    "删除学生失败，请稍后重试"
            );
        }

        writeOperationLog(
                operatorId,
                "删除学生",
                "将学生 ["
                        + safeText(user.getStudentNo())
                        + " / "
                        + safeText(user.getRealName())
                        + "] 放入回收站，原因："
                        + deleteReason
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreStudent(
            Long studentId,
            Long operatorId
    ) {

        if (studentId == null) {

            throw new IllegalArgumentException(
                    "请选择要恢复的学生"
            );
        }

        if (operatorId == null) {

            throw new IllegalArgumentException(
                    "无法识别当前操作管理员，请重新登录"
            );
        }

        SysUser user =
                userMapper.selectById(
                        studentId
                );

        if (user == null) {

            throw new IllegalArgumentException(
                    "学生不存在"
            );
        }

        if (!isDeleted(user)) {

            throw new IllegalArgumentException(
                    "该学生未被删除，无需恢复"
            );
        }

        LambdaUpdateWrapper<SysUser> wrapper =
                new LambdaUpdateWrapper<>();

        wrapper.eq(
                        SysUser::getId,
                        studentId
                )
                .set(
                        SysUser::getDeleted,
                        (short) 0
                )
                .set(
                        SysUser::getDeletedTime,
                        null
                )
                .set(
                        SysUser::getDeletedBy,
                        null
                )
                .set(
                        SysUser::getDeleteReason,
                        null
                );

        int updated =
                userMapper.update(
                        null,
                        wrapper
                );

        if (updated <= 0) {

            throw new IllegalStateException(
                    "恢复学生失败，请稍后重试"
            );
        }

        writeOperationLog(
                operatorId,
                "恢复学生",
                "将学生 ["
                        + safeText(user.getStudentNo())
                        + " / "
                        + safeText(user.getRealName())
                        + "] 从回收站恢复"
        );
    }

    /**
     * 判断是否已软删除
     */
    private boolean isDeleted(
            SysUser user
    ) {

        return user.getDeleted() != null
                && user.getDeleted() == 1;
    }

    /**
     * 判断目标用户是否拥有「学生」岗位
     */
    private boolean isStudentUser(
            Long userId
    ) {

        List<SysUserPosition> relations =
                userPositionMapper.selectList(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .eq(
                                        SysUserPosition::getUserId,
                                        userId
                                )
                );

        if (relations == null
                || relations.isEmpty()) {

            return false;
        }

        for (SysUserPosition relation : relations) {

            if (relation == null
                    || relation.getPositionId() == null) {

                continue;
            }

            SysPosition position =
                    positionMapper.selectById(
                            relation.getPositionId()
                    );

            if (position == null
                    || position.getName() == null) {

                continue;
            }

            if (STUDENT_POSITION_NAME.equals(
                    position.getName()
            )) {

                return true;
            }
        }

        return false;
    }

    /**
     * 批量查询操作管理员姓名
     */
    private Map<Long, String> loadOperatorNames(
            Set<Long> operatorIds
    ) {

        Map<Long, String> names =
                new HashMap<>();

        if (operatorIds == null
                || operatorIds.isEmpty()) {

            return names;
        }

        List<SysUser> operators =
                userMapper.selectBatchIds(
                        operatorIds
                );

        if (operators == null) {

            return names;
        }

        for (SysUser operator : operators) {

            if (operator == null
                    || operator.getId() == null) {

                continue;
            }

            names.put(
                    operator.getId(),
                    operator.getRealName()
            );
        }

        return names;
    }

    /**
     * 写入操作日志（复用现有 operation_log 表）
     */
    private void writeOperationLog(
            Long operatorId,
            String operation,
            String description
    ) {

        OperationLog log =
                new OperationLog();

        log.setUserId(
                operatorId
        );

        log.setOperation(
                operation
        );

        log.setDescription(
                description
        );

        operationLogMapper.insert(
                log
        );
    }

    /**
     * 去除首尾空白，空白字符串返回 null
     */
    private String trimToNull(
            String value
    ) {

        if (value == null) {

            return null;
        }

        String trimmed =
                value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }

    /**
     * 日志内文本兜底
     */
    private String safeText(
            String value
    ) {

        return value == null
                ? "-"
                : value;
    }

}
