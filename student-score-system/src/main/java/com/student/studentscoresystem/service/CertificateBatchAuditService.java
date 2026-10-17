package com.student.studentscoresystem.service;


import com.student.studentscoresystem.dto.BatchPreliminaryAuditDTO;
import com.student.studentscoresystem.entity.ScoreApply;
import com.student.studentscoresystem.mapper.ScoreApplyMapper;
import com.student.studentscoresystem.vo.BatchAuditResultVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * =========================================================
 * 个人证书批量审核（初审 / 终审）
 *
 * 设计要点：
 *
 * 1. 批量操作「逐条独立事务」处理，而不是整批一个大事务。
 *    任何一条失败（例如已经被其它人审核、并发重复生成成绩）
 *    都不会回滚其它已经成功的记录，同时返回失败原因给前端。
 *
 * 2. 真正的写操作分别在
 *    {@link CertificatePreliminaryAuditService#approveOne}
 *    和 {@link CertificateScoreFinalizeService#finalizeApproval}
 *    中完成，各自带有 @Transactional 边界，本类只做编排与容错。
 *
 * 3. 终审通过会真实生成正式成绩记录，
 *    并发场景由数据库唯一索引 uk_score_record_source 兜底，
 *    这里捕获 DuplicateKeyException 做幂等处理，避免产生重复数据。
 * =========================================================
 */
@Service
public class CertificateBatchAuditService {

    private final CertificatePreliminaryAuditService preliminaryAuditService;

    private final CertificateScoreFinalizeService certificateScoreFinalizeService;

    private final ScoreApplyMapper scoreApplyMapper;

    public CertificateBatchAuditService(
            CertificatePreliminaryAuditService preliminaryAuditService,
            CertificateScoreFinalizeService certificateScoreFinalizeService,
            ScoreApplyMapper scoreApplyMapper
    ) {

        this.preliminaryAuditService =
                preliminaryAuditService;

        this.certificateScoreFinalizeService =
                certificateScoreFinalizeService;

        this.scoreApplyMapper =
                scoreApplyMapper;
    }

    /**
     * =========================================================
     * 批量初审通过
     * =========================================================
     */
    public BatchAuditResultVO batchPreliminary(
            List<BatchPreliminaryAuditDTO.Item> items,
            Long reviewerId
    ) {

        BatchAuditResultVO result =
                new BatchAuditResultVO();

        if (items == null || items.isEmpty()) {

            return result;
        }

        result.setTotal(
                items.size()
        );

        for (
                BatchPreliminaryAuditDTO.Item item :
                items
        ) {

            if (item == null || item.getId() == null) {

                result.addFailure(
                        "存在无效的申请记录（ID为空）"
                );

                continue;
            }

            String message;

            try {

                message =
                        preliminaryAuditService.approveOne(
                                item.getId(),
                                item.getFinalScore(),
                                reviewerId
                        );

            } catch (Exception e) {

                message =
                        "初审异常：" + e.getMessage();
            }

            if (message == null) {

                result.addSuccess();

            } else {

                result.addFailure(
                        "申请[" + item.getId() + "]：" + message
                );
            }
        }

        return result;
    }


    /**
     * =========================================================
     * 批量终审通过
     * =========================================================
     */
    public BatchAuditResultVO batchFinal(
            List<Long> ids,
            Long reviewerId
    ) {

        BatchAuditResultVO result =
                new BatchAuditResultVO();

        if (ids == null || ids.isEmpty()) {

            return result;
        }

        /*
         * 去重，避免同一个申请在同一批次里被处理两次。
         */
        Set<Long> distinctIds =
                new LinkedHashSet<>(
                        ids
                );

        result.setTotal(
                distinctIds.size()
        );

        for (
                Long id :
                distinctIds
        ) {

            if (id == null) {

                result.addFailure(
                        "存在无效的申请记录（ID为空）"
                );

                continue;
            }

            ScoreApply apply =
                    scoreApplyMapper.selectById(
                            id
                    );

            if (apply == null) {

                result.addFailure(
                        "申请[" + id + "]：申请不存在"
                );

                continue;
            }

            if (
                    !"CERTIFICATE".equals(
                            apply.getApplyType()
                    )
            ) {

                result.addFailure(
                        "申请[" + id + "]：该申请不是个人证书申请"
                );

                continue;
            }

            if (
                    !Short.valueOf((short) 1)
                            .equals(
                                    apply.getPreliminaryStatus()
                            )
            ) {

                result.addFailure(
                        "申请[" + id + "]：该申请尚未通过初审，不能进行终审"
                );

                continue;
            }

            if (
                    !Short.valueOf((short) 0)
                            .equals(
                                    apply.getFinalStatus()
                            )
            ) {

                result.addFailure(
                        "申请[" + id + "]：该申请已经完成终审，不能重复审核"
                );

                continue;
            }

            if (
                    apply.getApplyScore() == null
                            ||
                            apply.getApplyScore()
                                    .compareTo(
                                            BigDecimal.ZERO
                                    ) <= 0
            ) {

                result.addFailure(
                        "申请[" + id + "]：该申请没有合法认定分值，无法终审"
                );

                continue;
            }

            String message;

            try {

                message =
                        certificateScoreFinalizeService
                                .finalizeApproval(
                                        apply,
                                        reviewerId
                                );

            } catch (DuplicateKeyException e) {

                /*
                 * 并发场景：
                 *
                 * 另一个请求已经为同一申请生成成绩记录，
                 * 唯一索引拦截了重复写入，
                 * 这里做幂等提示，不视为系统错误。
                 */
                message =
                        "该证书已经生成正式成绩记录，不能重复审批";

            } catch (Exception e) {

                message =
                        "终审异常：" + e.getMessage();
            }

            if (message == null) {

                result.addSuccess();

            } else {

                result.addFailure(
                        "申请[" + id + "]：" + message
                );
            }
        }

        return result;
    }


}
