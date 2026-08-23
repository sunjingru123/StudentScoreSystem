package com.student.studentscoresystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.student.studentscoresystem.entity.ScoreAdminAdjustment;

public interface IScoreAdminAdjustmentService
        extends IService<ScoreAdminAdjustment> {

    /**
     * 管理员成绩调整
     *
     * adjustType：
     * 1  = 加分
     * -1 = 减分
     */
    void createAdjustment(
            Long adminId,
            Long studentId,
            Short adjustType,
            java.math.BigDecimal score,
            String reason
    );
}