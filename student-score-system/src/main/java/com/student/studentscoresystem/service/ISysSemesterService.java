package com.student.studentscoresystem.service;

import com.student.studentscoresystem.entity.SysSemester;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 学期信息表 服务类
 * </p>
 *
 * @author 茹茹宝贝
 * @since 2026-08-05
 */
public interface ISysSemesterService extends IService<SysSemester> {

    /**
     * =========================================================
     * 获取当前生效的学期
     *
     * 查询顺序：
     *
     * 1. status = 1 且当前时间落在 start_date ~ end_date 内；
     * 2. 否则回退到状态为启用的最近学期；
     * 3. 再否则回退到最新创建的学期。
     *
     * @return 未配置任何学期时返回 null
     * =========================================================
     */
    SysSemester getCurrentSemester();
}
