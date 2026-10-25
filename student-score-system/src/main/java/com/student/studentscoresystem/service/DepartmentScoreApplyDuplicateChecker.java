package com.student.studentscoresystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * =========================================================
 * 部门加减分申报的窄口径判重
 *
 * 判定口径：学生 + 部门 + 项目 + 活动日期 + 备注 完全相同，
 * 并且活动日期非空，才算重复。
 *
 * 因此：
 *
 * 1. 同一个学生的同一个项目可以在不同日期多次申报
 *    （例如一周内多次旷课），不会被判重；
 * 2. 日期留空的申报不做判重，没有日期就没有区分
 *    「同一天重复录入」的依据；
 * 3. 已经被驳回的申报不参与判重，允许修正后重新提交。
 *
 * 用途：拦截「同一个导入文件重复上传」「同一条记录重复提交」，
 * Excel 导入与单条申报共用同一套判定。
 * =========================================================
 */
@Service
public class DepartmentScoreApplyDuplicateChecker {

    private final IDepartmentScoreApplyService applyService;

    public DepartmentScoreApplyDuplicateChecker(
            IDepartmentScoreApplyService applyService) {

        this.applyService = applyService;
    }

    /**
     * 是否已经存在完全相同、且未被驳回的申报
     */
    public boolean isDuplicate(
            Long studentId,
            Long departmentId,
            Long templateId,
            LocalDate activityDate,
            String description) {

        if (studentId == null
                || departmentId == null
                || templateId == null
                || activityDate == null) {

            return false;
        }

        List<DepartmentScoreApply> candidates =
                applyService.list(
                        new LambdaQueryWrapper<DepartmentScoreApply>()
                                .eq(
                                        DepartmentScoreApply::getStudentId,
                                        studentId
                                )
                                .eq(
                                        DepartmentScoreApply::getDepartmentId,
                                        departmentId
                                )
                                .eq(
                                        DepartmentScoreApply::getTemplateId,
                                        templateId
                                )
                                .eq(
                                        DepartmentScoreApply::getActivityDate,
                                        activityDate
                                )
                                /*
                                 * 只和未被驳回的申报判重：
                                 * 0 = 待部门审核
                                 * 1 + finalStatus 0 + archiveStatus != 2 = 待档案部 / 待辅导员终审
                                 * 1 + finalStatus 1 = 已通过
                                 *
                                 * 档案部已驳回（archiveStatus = 2）的申报
                                 * 属于可修正后重新提交，不再参与判重；
                                 * archive_status 在历史数据上可能为 NULL，
                                 * 因此用「<> 2 或 IS NULL」保证 NULL 行仍参与判重。
                                 */
                                .and(w -> w
                                        .eq(
                                                DepartmentScoreApply::getStatus,
                                                (short) 0
                                        )
                                        .or(inner -> inner
                                                .eq(
                                                        DepartmentScoreApply::getStatus,
                                                        (short) 1
                                                )
                                                .eq(
                                                        DepartmentScoreApply::getFinalStatus,
                                                        (short) 0
                                                )
                                                .and(sub -> sub
                                                        .ne(
                                                                DepartmentScoreApply::getArchiveStatus,
                                                                (short) 2
                                                        )
                                                        .or()
                                                        .isNull(
                                                                DepartmentScoreApply::getArchiveStatus
                                                        )))
                                        .or(inner -> inner
                                                .eq(
                                                        DepartmentScoreApply::getStatus,
                                                        (short) 1
                                                )
                                                .eq(
                                                        DepartmentScoreApply::getFinalStatus,
                                                        (short) 1
                                                )))
                );

        if (candidates == null || candidates.isEmpty()) {

            return false;
        }

        /*
         * 备注直接交给 SQL 比较会漏掉 null 与空字符串，
         * 这里统一归一化之后再逐条比较。
         */
        String expected =
                normalizeDescription(description);

        for (DepartmentScoreApply candidate : candidates) {

            if (Objects.equals(
                    expected,
                    normalizeDescription(
                            candidate.getDescription()
                    ))) {

                return true;
            }
        }

        return false;
    }

    /**
     * 备注归一化：去掉首尾空白，空字符串与 null 视为同一种情况
     */
    private String normalizeDescription(
            String description) {

        if (description == null) {

            return null;
        }

        String trimmed =
                description.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}
