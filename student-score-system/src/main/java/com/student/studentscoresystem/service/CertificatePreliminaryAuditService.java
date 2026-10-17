package com.student.studentscoresystem.service;


import com.student.studentscoresystem.entity.ScoreApply;
import com.student.studentscoresystem.mapper.ScoreApplyMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * =========================================================
 * 个人证书初审
 *
 * 把「初审通过」的写操作从 Controller 下沉到 Service，
 * 与 ScoreApplyController 单条初审保持完全一致的业务规则：
 *
 * 1. 必须是个人证书（CERTIFICATE）申请；
 * 2. 只有未初审（preliminaryStatus = 0）的申请可以初审；
 * 3. 初审通过必须填写大于 0 的认定分值，否则阻断。
 *
 * 单条初审使用独立事务，任何一步异常都会整体回滚。
 * =========================================================
 */
@Service
public class CertificatePreliminaryAuditService {

    private final ScoreApplyMapper scoreApplyMapper;

    public CertificatePreliminaryAuditService(
            ScoreApplyMapper scoreApplyMapper
    ) {

        this.scoreApplyMapper =
                scoreApplyMapper;
    }

    /**
     * =========================================================
     * 初审通过单个申请
     *
     * @return null 表示成功，否则为业务失败提示
     * =========================================================
     */
    @Transactional(rollbackFor = Exception.class)
    public String approveOne(
            Long id,
            BigDecimal finalScore,
            Long reviewerId
    ) {

        if (id == null) {

            return "申请ID不能为空";
        }

        /*
         * 初审通过必须有合法认定分值，严禁默认值兜底。
         */
        if (finalScore == null) {

            return "初审通过时必须填写认定分值";
        }

        if (
                finalScore.compareTo(
                        BigDecimal.ZERO
                ) <= 0
        ) {

            return "认定分值必须大于0";
        }

        ScoreApply apply =
                scoreApplyMapper.selectById(
                        id
                );

        if (apply == null) {

            return "申请不存在";
        }

        if (
                !"CERTIFICATE".equals(
                        apply.getApplyType()
                )
        ) {

            return "该申请不是个人证书申请";
        }

        /*
         * 防止重复初审
         */
        if (
                !Short.valueOf((short) 0)
                        .equals(
                                apply.getPreliminaryStatus()
                        )
        ) {

            return "该申请已经完成初审，不能重复审核";
        }

        apply.setApplyScore(
                finalScore
        );

        /*
         * 初审通过，进入终审
         */
        apply.setPreliminaryStatus(
                (short) 1
        );

        apply.setFinalStatus(
                (short) 0
        );

        apply.setStatus(
                (short) 0
        );

        apply.setPreliminaryReviewerId(
                reviewerId
        );

        apply.setPreliminaryReviewTime(
                LocalDateTime.now()
        );

        apply.setUpdateTime(
                LocalDateTime.now()
        );

        int result =
                scoreApplyMapper.updateById(
                        apply
                );

        if (result <= 0) {

            return "初审操作失败";
        }

        return null;
    }


}
