package com.student.studentscoresystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.ScoreRecordDeleteApply;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.ScoreRecordDeleteApplyMapper;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.vo.ScoreDeleteApplyVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * =========================================================
 * 成绩删除申请（提交 / 审核）
 * =========================================================
 *
 * 业务规则（保持既有权限与状态流转不变，只是新增一层审核）：
 *
 * 1. 提交人：档案部部长 / 副部长（干事不能提交）；
 * 2. 审核人：档案部负责辅导员（department.teacher_id）或高级辅导员；
 * 3. 审核通过后，复用 {@link ScoreRecordManageService#deleteRecord}
 *    执行原有逻辑删除（status = 0、admin_hidden = 1），
 *    并沿用 score_record_operation_log 审计链路；
 * 4. 管理员 / 高级辅导员 / 档案部负责辅导员本身可以直接删除，
 *    不需要走申请流程。
 * =========================================================
 */
@Service
public class ScoreDeleteApplyService {

    /**
     * 待审核
     */
    public static final short STATUS_PENDING = 0;

    /**
     * 已通过
     */
    public static final short STATUS_APPROVED = 1;

    /**
     * 已驳回
     */
    public static final short STATUS_REJECTED = 2;


    private final ScoreRecordDeleteApplyMapper applyMapper;

    private final ScoreRecordMapper scoreRecordMapper;

    private final SysUserMapper sysUserMapper;

    private final ScoreManagerPermissionService permissionService;

    private final ScoreRecordManageService scoreRecordManageService;

    private final ScoreProjectNameResolver scoreProjectNameResolver;


    public ScoreDeleteApplyService(
            ScoreRecordDeleteApplyMapper applyMapper,
            ScoreRecordMapper scoreRecordMapper,
            SysUserMapper sysUserMapper,
            ScoreManagerPermissionService permissionService,
            ScoreRecordManageService scoreRecordManageService,
            ScoreProjectNameResolver scoreProjectNameResolver
    ) {

        this.applyMapper = applyMapper;

        this.scoreRecordMapper = scoreRecordMapper;

        this.sysUserMapper = sysUserMapper;

        this.permissionService = permissionService;

        this.scoreRecordManageService = scoreRecordManageService;

        this.scoreProjectNameResolver = scoreProjectNameResolver;
    }


    /**
     * =====================================================
     * 提交删除申请
     * =====================================================
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submitApply(
            Long recordId,
            Long applicantId,
            String reason
    ) {

        if (recordId == null) {

            throw new IllegalArgumentException("成绩记录ID不能为空");
        }


        if (applicantId == null) {

            throw new IllegalArgumentException("请先登录");
        }


        if (!permissionService.canSubmitScoreDeleteApply(applicantId)) {

            throw new SecurityException(
                    "只有档案部部长 / 副部长可以提交成绩删除申请"
            );
        }


        ScoreRecord record =
                scoreRecordMapper.selectById(recordId);


        if (record == null) {

            throw new IllegalArgumentException("成绩记录不存在");
        }


        if (record.getStatus() != null && record.getStatus() == 0) {

            throw new IllegalArgumentException("该成绩记录已经删除");
        }


        /*
         * 同一条成绩只允许存在一条待审核申请。
         *
         * 这里先做业务预检，真正的并发兜底靠
         * uk_score_delete_apply_pending 局部唯一索引，
         * 冲突时抛出 DuplicateKeyException 由调用方友好提示。
         */
        Long pendingCount =
                applyMapper.selectCount(
                        new LambdaQueryWrapper<ScoreRecordDeleteApply>()
                                .eq(
                                        ScoreRecordDeleteApply::getScoreRecordId,
                                        recordId
                                )
                                .eq(
                                        ScoreRecordDeleteApply::getStatus,
                                        STATUS_PENDING
                                )
                );


        if (pendingCount != null && pendingCount > 0) {

            throw new IllegalArgumentException(
                    "该成绩已有一条待审核的删除申请"
            );
        }


        ScoreRecordDeleteApply apply =
                new ScoreRecordDeleteApply();

        apply.setScoreRecordId(recordId);

        apply.setStudentId(record.getStudentId());

        apply.setApplicantId(applicantId);

        apply.setReason(normalize(reason));

        apply.setStatus(STATUS_PENDING);

        apply.setCreateTime(LocalDateTime.now());

        apply.setUpdateTime(LocalDateTime.now());


        if (applyMapper.insert(apply) != 1) {

            throw new IllegalStateException("删除申请提交失败");
        }


        return apply.getId();
    }


    /**
     * =====================================================
     * 我的删除申请（档案部成员）
     * =====================================================
     */
    public List<ScoreDeleteApplyVO> listMine(Long applicantId) {

        if (applicantId == null) {

            throw new IllegalArgumentException("请先登录");
        }


        List<ScoreRecordDeleteApply> applies =
                applyMapper.selectList(
                        new LambdaQueryWrapper<ScoreRecordDeleteApply>()
                                .eq(
                                        ScoreRecordDeleteApply::getApplicantId,
                                        applicantId
                                )
                                .orderByDesc(
                                        ScoreRecordDeleteApply::getId
                                )
                );


        return toVoList(applies);
    }


    /**
     * =====================================================
     * 待我审核（辅导员）
     * =====================================================
     */
    public List<ScoreDeleteApplyVO> listPending(Long reviewerId) {

        requireReviewer(reviewerId);


        List<ScoreRecordDeleteApply> applies =
                applyMapper.selectList(
                        new LambdaQueryWrapper<ScoreRecordDeleteApply>()
                                .eq(
                                        ScoreRecordDeleteApply::getStatus,
                                        STATUS_PENDING
                                )
                                .orderByAsc(
                                        ScoreRecordDeleteApply::getId
                                )
                );


        return toVoList(applies);
    }


    /**
     * =====================================================
     * 我已处理（辅导员）
     * =====================================================
     */
    public List<ScoreDeleteApplyVO> listProcessed(Long reviewerId) {

        requireReviewer(reviewerId);


        List<ScoreRecordDeleteApply> applies =
                applyMapper.selectList(
                        new LambdaQueryWrapper<ScoreRecordDeleteApply>()
                                .eq(
                                        ScoreRecordDeleteApply::getReviewerId,
                                        reviewerId
                                )
                                .in(
                                        ScoreRecordDeleteApply::getStatus,
                                        List.of(STATUS_APPROVED, STATUS_REJECTED)
                                )
                                .orderByDesc(
                                        ScoreRecordDeleteApply::getId
                                )
                );


        return toVoList(applies);
    }


    /**
     * =====================================================
     * 审核通过
     * =====================================================
     *
     * 通过后立即执行原有逻辑删除；
     * 任一环节失败（例如成绩已被他人删除）整体回滚，
     * 申请保持待审核状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(
            Long applyId,
            Long reviewerId,
            String remark
    ) {

        ScoreRecordDeleteApply apply =
                requirePendingApply(applyId, reviewerId);


        ScoreRecord record =
                scoreRecordMapper.selectById(
                        apply.getScoreRecordId()
                );


        if (record == null) {

            throw new IllegalArgumentException("成绩记录不存在");
        }


        if (record.getStatus() != null && record.getStatus() == 0) {

            throw new IllegalArgumentException(
                    "该成绩记录已经被删除，无需再次审核"
            );
        }


        /*
         * 复用既有删除逻辑：逻辑删除 + 操作日志。
         */
        scoreRecordManageService.deleteRecord(
                apply.getScoreRecordId(),
                reviewerId,
                buildDeleteReason(apply, remark)
        );


        apply.setStatus(STATUS_APPROVED);

        apply.setReviewerId(reviewerId);

        apply.setReviewRemark(normalize(remark));

        apply.setReviewTime(LocalDateTime.now());

        apply.setUpdateTime(LocalDateTime.now());


        if (applyMapper.updateById(apply) != 1) {

            throw new IllegalStateException("删除申请审核失败");
        }
    }


    /**
     * =====================================================
     * 审核驳回
     * =====================================================
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(
            Long applyId,
            Long reviewerId,
            String remark
    ) {

        ScoreRecordDeleteApply apply =
                requirePendingApply(applyId, reviewerId);


        apply.setStatus(STATUS_REJECTED);

        apply.setReviewerId(reviewerId);

        apply.setReviewRemark(normalize(remark));

        apply.setReviewTime(LocalDateTime.now());

        apply.setUpdateTime(LocalDateTime.now());


        if (applyMapper.updateById(apply) != 1) {

            throw new IllegalStateException("删除申请驳回失败");
        }
    }


    /**
     * 校验审核权限并返回一条待审核申请。
     */
    private ScoreRecordDeleteApply requirePendingApply(
            Long applyId,
            Long reviewerId
    ) {

        if (applyId == null) {

            throw new IllegalArgumentException("申请ID不能为空");
        }


        requireReviewer(reviewerId);


        ScoreRecordDeleteApply apply =
                applyMapper.selectById(applyId);


        if (apply == null) {

            throw new IllegalArgumentException("删除申请不存在");
        }


        if (apply.getStatus() == null
                || apply.getStatus() != STATUS_PENDING) {

            throw new IllegalArgumentException("该申请已经处理过了");
        }


        return apply;
    }


    private void requireReviewer(Long reviewerId) {

        if (reviewerId == null) {

            throw new IllegalArgumentException("请先登录");
        }


        if (!permissionService.canReviewScoreDeleteApply(reviewerId)) {

            throw new SecurityException(
                    "只有档案部负责辅导员或高级辅导员可以审核删除申请"
            );
        }
    }


    private String buildDeleteReason(
            ScoreRecordDeleteApply apply,
            String remark
    ) {

        StringBuilder builder = new StringBuilder();

        builder.append("删除申请审核通过");


        if (apply.getReason() != null
                && !apply.getReason().isBlank()) {

            builder.append("，申请理由：")
                    .append(apply.getReason().trim());
        }


        if (remark != null && !remark.isBlank()) {

            builder.append("，审核意见：")
                    .append(remark.trim());
        }


        return builder.toString();
    }


    private String normalize(String value) {

        return value == null || value.isBlank()
                ? null
                : value.trim();
    }


    /**
     * =====================================================
     * 批量组装 VO（一次查询学生与项目名称，避免 N+1）
     * =====================================================
     */
    private List<ScoreDeleteApplyVO> toVoList(
            List<ScoreRecordDeleteApply> applies
    ) {

        if (applies == null || applies.isEmpty()) {

            return new ArrayList<>();
        }


        Set<Long> recordIds = new HashSet<>();

        Set<Long> userIds = new HashSet<>();


        for (ScoreRecordDeleteApply apply : applies) {

            if (apply == null) {

                continue;
            }


            if (apply.getScoreRecordId() != null) {

                recordIds.add(apply.getScoreRecordId());
            }


            if (apply.getStudentId() != null) {

                userIds.add(apply.getStudentId());
            }


            if (apply.getApplicantId() != null) {

                userIds.add(apply.getApplicantId());
            }


            if (apply.getReviewerId() != null) {

                userIds.add(apply.getReviewerId());
            }
        }


        Map<Long, ScoreRecord> recordMap = new HashMap<>();


        if (!recordIds.isEmpty()) {

            List<ScoreRecord> records =
                    scoreRecordMapper.selectBatchIds(recordIds);


            if (records != null) {

                for (ScoreRecord record : records) {

                    if (record != null && record.getId() != null) {

                        recordMap.put(record.getId(), record);
                    }
                }
            }
        }


        Map<Long, SysUser> userMap = new HashMap<>();


        if (!userIds.isEmpty()) {

            List<SysUser> users =
                    sysUserMapper.selectBatchIds(userIds);


            if (users != null) {

                for (SysUser user : users) {

                    if (user != null && user.getId() != null) {

                        userMap.put(user.getId(), user);
                    }
                }
            }
        }


        ScoreProjectNameResolver.PreloadedNames names =
                scoreProjectNameResolver.preload(recordMap.values());


        List<ScoreDeleteApplyVO> result = new ArrayList<>();


        for (ScoreRecordDeleteApply apply : applies) {

            if (apply == null) {

                continue;
            }


            ScoreDeleteApplyVO vo = new ScoreDeleteApplyVO();

            vo.setId(apply.getId());

            vo.setScoreRecordId(apply.getScoreRecordId());

            vo.setStudentId(apply.getStudentId());

            vo.setApplicantId(apply.getApplicantId());

            vo.setReason(apply.getReason());

            vo.setStatus(apply.getStatus());

            vo.setStatusText(statusText(apply.getStatus()));

            vo.setReviewerId(apply.getReviewerId());

            vo.setReviewRemark(apply.getReviewRemark());

            vo.setReviewTime(apply.getReviewTime());

            vo.setCreateTime(apply.getCreateTime());


            SysUser student = userMap.get(apply.getStudentId());


            if (student != null) {

                vo.setStudentName(student.getRealName());

                vo.setStudentNo(student.getStudentNo());

                vo.setClassName(student.getClassName());
            }


            SysUser applicant = userMap.get(apply.getApplicantId());


            if (applicant != null) {

                vo.setApplicantName(
                        applicant.getRealName() != null
                                ? applicant.getRealName()
                                : applicant.getUsername()
                );
            }


            SysUser reviewer = userMap.get(apply.getReviewerId());


            if (reviewer != null) {

                vo.setReviewerName(
                        reviewer.getRealName() != null
                                ? reviewer.getRealName()
                                : reviewer.getUsername()
                );
            }


            ScoreRecord record = recordMap.get(apply.getScoreRecordId());


            if (record != null) {

                vo.setScore(record.getScore());

                vo.setSourceType(record.getSourceType());

                vo.setSourceName(names.resolveSourceLabel(record));

                vo.setProjectName(names.resolve(record, true));

                vo.setRecordCreateTime(record.getCreateTime());

                vo.setRecordStatus(record.getStatus());

                vo.setRecordAdminHidden(record.getAdminHidden());


                if (vo.getStudentId() == null) {

                    vo.setStudentId(record.getStudentId());
                }
            }


            result.add(vo);
        }


        return result;
    }


    private String statusText(Short status) {

        if (status == null) {

            return "未知";
        }


        return switch (status) {

            case STATUS_PENDING -> "待审核";

            case STATUS_APPROVED -> "已通过";

            case STATUS_REJECTED -> "已驳回";

            default -> "未知";
        };
    }
}
