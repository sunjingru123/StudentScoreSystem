package com.student.studentscoresystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.SysSemester;
import com.student.studentscoresystem.mapper.SysSemesterMapper;
import com.student.studentscoresystem.service.ISysSemesterService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * <p>
 * 学期信息表 服务实现类
 * </p>
 *
 * @author 茹茹宝贝
 * @since 2026-08-05
 */
@Service
public class SysSemesterServiceImpl extends ServiceImpl<SysSemesterMapper, SysSemester> implements ISysSemesterService {

    /**
     * =========================================================
     * 获取当前生效的学期
     *
     * 严禁在成绩生成逻辑中写死学期 ID，统一从这里取。
     * =========================================================
     */
    @Override
    public SysSemester getCurrentSemester() {

        LocalDate today =
                LocalDate.now();

        /*
         * 1. 当前时间落在学期区间内，且状态为启用
         */
        SysSemester active =
                baseMapper.selectOne(
                        new LambdaQueryWrapper<SysSemester>()
                                .le(
                                        SysSemester::getStartDate,
                                        today
                                )
                                .ge(
                                        SysSemester::getEndDate,
                                        today
                                )
                                .eq(
                                        SysSemester::getStatus,
                                        (short) 1
                                )
                                .orderByDesc(
                                        SysSemester::getStartDate
                                )
                                .orderByDesc(
                                        SysSemester::getId
                                )
                                .last(
                                        "LIMIT 1"
                                )
                );

        if (active != null) {

            return active;
        }

        /*
         * 2. 回退：状态为启用的最近学期
         */
        SysSemester enabled =
                baseMapper.selectOne(
                        new LambdaQueryWrapper<SysSemester>()
                                .eq(
                                        SysSemester::getStatus,
                                        (short) 1
                                )
                                .orderByDesc(
                                        SysSemester::getStartDate
                                )
                                .orderByDesc(
                                        SysSemester::getId
                                )
                                .last(
                                        "LIMIT 1"
                                )
                );

        if (enabled != null) {

            return enabled;
        }

        /*
         * 3. 再回退：最新创建的学期
         */
        return baseMapper.selectOne(
                new LambdaQueryWrapper<SysSemester>()
                        .orderByDesc(
                                SysSemester::getCreateTime
                        )
                        .orderByDesc(
                                SysSemester::getId
                        )
                        .last(
                                "LIMIT 1"
                        )
        );
    }
}
