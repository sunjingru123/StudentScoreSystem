package com.student.studentscoresystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.vo.BatchAuditResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


/**
 * =========================================================
 * 部门加减分申报「档案部审核」环节
 *
 * 审核链路（四段）：
 *   干事 / 副部长 / 部长 提交
 *     -> 部门副部长 / 部长 初审（status 0 -> 1 / 2）
 *     -> 档案部副部长 / 部长 审核（archiveStatus 0 -> 1 / 2）
 *     -> 辅导员 终审（finalStatus 0 -> 1 / 2，通过后才生成正式成绩）
 *
 * 设计要点：
 *
 * 1. 写操作全部下沉到本 Service，并带
 *    {@code @Transactional(rollbackFor = Exception.class)} 边界，
 *    Controller 只负责参数 / 权限的最外层校验与响应封装。
 *
 * 2. 通过 / 驳回都只改动 department_score_apply 的档案部审核字段，
 *    不会在档案部环节生成成绩；成绩生成点仍在
 *    {@link DepartmentScoreFinalizeService#finalizeApproval}（辅导员终审）。
 *
 * 3. 审核人必须是「档案部」的部长 / 副部长，
 *    且不能审核自己提交的申报，与部门初审保持一致。
 * =========================================================
 */
@Service
public class DepartmentScoreArchiveAuditService {

    /**
     * 一键通过时统一写入的审核意见
     */
    private static final String ARCHIVE_BATCH_PASS_REMARK =
            "一键审批通过";

    private final IDepartmentScoreApplyService applyService;

    private final ScoreManagerPermissionService scoreManagerPermissionService;


    public DepartmentScoreArchiveAuditService(
            IDepartmentScoreApplyService applyService,
            ScoreManagerPermissionService scoreManagerPermissionService) {

        this.applyService = applyService;

        this.scoreManagerPermissionService = scoreManagerPermissionService;
    }


    /**
     * =========================================================
     * 单条档案部审核
     *
     * @param applyId    申报记录 ID
     * @param reviewerId 当前登录用户
     * @param status     1 = 通过，2 = 驳回
     * @param remark     审核意见
     * @return 错误信息；返回 null 表示审核成功
     * =========================================================
     */
    @Transactional(rollbackFor = Exception.class)
    public String archiveAudit(
            Long applyId,
            Long reviewerId,
            Short status,
            String remark) {

        if (!scoreManagerPermissionService.isArchiveLeader(reviewerId)) {

            return "你不是档案部的部长或副部长，没有审核权限";
        }


        if (applyId == null) {

            return "申报记录ID不能为空";
        }


        if (status == null
                || (status != 1 && status != 2)) {

            return "审核状态无效";
        }


        DepartmentScoreApply apply =
                applyService.getById(applyId);


        if (apply == null) {

            return "申报记录不存在";
        }


        /*
         * 只有通过部门初审的申报才进入档案部审核
         */
        if (!Short.valueOf((short) 1)
                .equals(apply.getStatus())) {

            return "该申报尚未通过部门审核";
        }


        if (!Short.valueOf((short) 0)
                .equals(apply.getArchiveStatus())) {

            return "该申报已经完成档案部审核";
        }


        /*
         * 不能审核自己提交的申报
         */
        if (reviewerId != null
                && reviewerId.equals(apply.getApplicantId())) {

            return "不能审核自己提交的申报";
        }


        LocalDateTime now =
                LocalDateTime.now();


        apply.setArchiveStatus(
                status
        );


        apply.setArchiveReviewerId(
                reviewerId
        );


        apply.setArchiveReviewRemark(
                remark
        );


        apply.setArchiveReviewTime(
                now
        );


        apply.setUpdateTime(
                now
        );


        boolean updated =
                applyService.updateById(
                        apply
                );


        if (!updated) {

            return "档案部审核处理失败";
        }


        return null;
    }


    /**
     * =========================================================
     * 一键通过全部待档案部审核的申报
     *
     * 逐条复用 {@link #archiveAudit}，
     * 单条失败只记录原因，不影响其它记录。
     * =========================================================
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchAuditResultVO batchPass(
            Long reviewerId) {

        BatchAuditResultVO result =
                new BatchAuditResultVO();


        if (!scoreManagerPermissionService.isArchiveLeader(reviewerId)) {

            result.addFailure(
                    "你不是档案部的部长或副部长，没有审核权限"
            );

            return result;
        }


        List<DepartmentScoreApply> list =
                applyService.list(
                        new LambdaQueryWrapper<DepartmentScoreApply>()
                                .eq(
                                        DepartmentScoreApply::getStatus,
                                        (short) 1
                                )
                                .eq(
                                        DepartmentScoreApply::getArchiveStatus,
                                        (short) 0
                                )
                                /*
                                 * 自己的申报不能自己审批
                                 */
                                .ne(
                                        DepartmentScoreApply::getApplicantId,
                                        reviewerId
                                )
                                .orderByAsc(
                                        DepartmentScoreApply::getCreateTime
                                )
                );


        if (list == null || list.isEmpty()) {

            return result;
        }


        result.setTotal(
                list.size()
        );


        for (DepartmentScoreApply apply : list) {

            if (apply == null || apply.getId() == null) {

                result.addFailure(
                        "存在无效的申报记录（ID为空）"
                );

                continue;
            }


            String message =
                    archiveAudit(
                            apply.getId(),
                            reviewerId,
                            (short) 1,
                            ARCHIVE_BATCH_PASS_REMARK
                    );


            if (message == null) {

                result.addSuccess();

            } else {

                result.addFailure(
                        message
                );
            }
        }


        return result;
    }
}
