package com.student.studentscoresystem.utils;

import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.DepartmentScoreApply;

/**
 * 部门 / 社团加减分申报的审核阶段规则。
 *
 * 组织分三类：
 *
 *   部门（type = 1）：正职 / 副职均可审核本部门申报（原有流程）。
 *   大社团（type = 2）：主席 / 副主席 / 理事长 / 副理事长，
 *                      负责审核旗下所有小社团的申报。
 *   小社团（type = 3）：只有正职（部长 / 主任）审核本小社团申报；
 *                      副职（副部长 / 副主任）只能提交，不能审核。
 *
 * 审核阶段：
 *
 *   status          0 = 待组织初审，1 = 组织初审通过，2 = 组织初审驳回
 *   bigClubStatus   null = 不经过大社团审核，
 *                   0 = 待大社团审核，1 = 大社团审核通过，2 = 大社团驳回
 *
 * 完整链路：
 *
 *   小社团副职 / 成员提交  → 小社团正职初审 → 大社团审核 → 档案部 → 辅导员终审
 *   小社团正职提交         → 大社团审核 → 档案部 → 辅导员终审
 *   大社团负责人提交       → 档案部 → 辅导员终审
 *   部门人员提交           → 部门初审 → 档案部 → 辅导员终审
 */
public final class DepartmentScoreReviewRules {

    /** 大社团审核：待审核 */
    public static final short BIG_CLUB_PENDING = 0;

    /** 大社团审核：通过 */
    public static final short BIG_CLUB_APPROVED = 1;

    /** 大社团审核：驳回 */
    public static final short BIG_CLUB_REJECTED = 2;

    private DepartmentScoreReviewRules() {
    }

    /**
     * 是否大社团
     */
    public static boolean isBigClub(Department org) {

        return org != null
                && org.getDepartmentType() != null
                && org.getDepartmentType() == OrgPositionLevel.TYPE_BIG_CLUB;
    }

    /**
     * 是否小社团
     */
    public static boolean isSmallClub(Department org) {

        return org != null
                && org.getDepartmentType() != null
                && org.getDepartmentType() == OrgPositionLevel.TYPE_SMALL_CLUB;
    }

    /**
     * 申报创建后的组织初审状态。
     *
     * 大社团负责人、小社团正职提交的申报无需组织初审，
     * 直接视为初审通过（小社团正职的申报后面仍需大社团审核）。
     */
    public static short initialStatus(
            Department org,
            String positionLevel
    ) {

        if (isBigClub(org)) {

            return 1;
        }

        if (isSmallClub(org)) {

            return OrgPositionLevel.isLeaderLevel(positionLevel)
                    ? (short) 1
                    : (short) 0;
        }

        /* 部门 / 未知组织：保持原有部门初审流程 */
        return 0;
    }

    /**
     * 申报创建后的大社团审核状态。
     *
     * 只有小社团的申报才需要大社团审核。
     */
    public static Short initialBigClubStatus(
            Department org
    ) {

        return isSmallClub(org)
                ? BIG_CLUB_PENDING
                : null;
    }

    /**
     * 是否具备「组织初审」权限。
     *
     * - 小社团：仅正职（部长 / 主任）
     * - 部门 / 未知组织：正职或副职（兼容原有部门流程）
     */
    public static boolean canAuditFirstStage(
            Department org,
            String positionLevel
    ) {

        if (isSmallClub(org)) {

            return OrgPositionLevel.isLeaderLevel(
                    positionLevel
            );
        }

        return OrgPositionLevel.isLeaderOrDeputy(
                positionLevel
        );
    }

    /**
     * 是否具备「大社团审核」权限。
     *
     * 要求：申报属于小社团，且用户是该小社团所属大社团的正职 / 副职。
     *
     * @param applyOrg   申报所属组织（必须是小社团）
     * @param parentOrg  申报组织所属大社团
     * @param positionLevel 用户在父组织中的权限层级
     */
    public static boolean canAuditBigClubStage(
            Department applyOrg,
            Department parentOrg,
            String positionLevel
    ) {

        if (!isSmallClub(applyOrg)) {

            return false;
        }

        if (applyOrg.getParentId() == null) {

            return false;
        }

        if (parentOrg == null
                || parentOrg.getId() == null
                || !isBigClub(parentOrg)) {

            return false;
        }

        if (!applyOrg.getParentId().equals(
                parentOrg.getId()
        )) {

            return false;
        }

        return OrgPositionLevel.isLeaderOrDeputy(
                positionLevel
        );
    }

    /**
     * 是否处于「待大社团审核」阶段
     */
    public static boolean waitingBigClubReview(
            DepartmentScoreApply apply
    ) {

        return apply != null
                && Short.valueOf((short) 1).equals(apply.getStatus())
                && apply.getBigClubStatus() != null
                && apply.getBigClubStatus() == BIG_CLUB_PENDING;
    }

    /**
     * 是否已经可以进入档案部审核
     *
     * 条件：组织初审通过，且（不需要大社团审核 或 大社团已通过）
     */
    public static boolean readyForArchive(
            DepartmentScoreApply apply
    ) {

        if (apply == null
                || !Short.valueOf((short) 1).equals(apply.getStatus())) {

            return false;
        }

        Short bigClubStatus =
                apply.getBigClubStatus();

        return bigClubStatus == null
                || bigClubStatus == BIG_CLUB_APPROVED;
    }
}
