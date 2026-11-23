package com.student.studentscoresystem;

import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.utils.DepartmentScoreReviewRules;
import com.student.studentscoresystem.utils.OrgPositionLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 部门 / 社团加减分审核阶段规则测试。
 *
 * 覆盖：
 *
 * 1. 申报创建时的初始审核状态；
 * 2. 组织初审权限（小社团仅正职，部门正副职均可）；
 * 3. 大社团审核权限（必须父子关系匹配）；
 * 4. 进入档案部审核的前置条件。
 */
class DepartmentScoreReviewRulesTest {

    private static final Long BIG_CLUB_ID = 2L;

    private static final Long SMALL_CLUB_ID = 3L;

    private static final Long OTHER_BIG_CLUB_ID = 5L;

    /* =====================================================
     * 初始状态
     * ===================================================== */

    @Test
    void departmentApplyWaitsForOrganizationAudit() {

        Department department =
                org(1L, OrgPositionLevel.TYPE_DEPARTMENT, null);

        assertEquals(
                0,
                DepartmentScoreReviewRules.initialStatus(
                        department,
                        OrgPositionLevel.LEVEL_LEADER
                )
        );

        assertEquals(
                0,
                DepartmentScoreReviewRules.initialStatus(
                        department,
                        OrgPositionLevel.LEVEL_DEPUTY
                )
        );

        assertNull(
                DepartmentScoreReviewRules.initialBigClubStatus(
                        department
                )
        );
    }

    @Test
    void bigClubApplySkipsInternalAudit() {

        Department bigClub =
                org(BIG_CLUB_ID, OrgPositionLevel.TYPE_BIG_CLUB, null);

        /* 主席 / 副主席 / 理事长 / 副理事长 都应直接进入档案部 */
        assertEquals(
                1,
                DepartmentScoreReviewRules.initialStatus(
                        bigClub,
                        OrgPositionLevel.LEVEL_LEADER
                )
        );

        assertEquals(
                1,
                DepartmentScoreReviewRules.initialStatus(
                        bigClub,
                        OrgPositionLevel.LEVEL_DEPUTY
                )
        );

        /* 大社团负责人自己提交，不经过大社团审核 */
        assertNull(
                DepartmentScoreReviewRules.initialBigClubStatus(
                        bigClub
                )
        );
    }

    @Test
    void smallClubApplyDependsOnApplicantLevel() {

        Department smallClub =
                org(SMALL_CLUB_ID, OrgPositionLevel.TYPE_SMALL_CLUB, BIG_CLUB_ID);

        /* 小社团正职提交：初审视为通过，但必须经大社团审核 */
        assertEquals(
                1,
                DepartmentScoreReviewRules.initialStatus(
                        smallClub,
                        OrgPositionLevel.LEVEL_LEADER
                )
        );

        /* 小社团副职提交：先由本小社团正职初审 */
        assertEquals(
                0,
                DepartmentScoreReviewRules.initialStatus(
                        smallClub,
                        OrgPositionLevel.LEVEL_DEPUTY
                )
        );

        assertEquals(
                DepartmentScoreReviewRules.BIG_CLUB_PENDING,
                DepartmentScoreReviewRules.initialBigClubStatus(
                        smallClub
                )
        );
    }

    /* =====================================================
     * 组织初审权限
     * ===================================================== */

    @Test
    void smallClubOnlyLeaderCanAudit() {

        Department smallClub =
                org(SMALL_CLUB_ID, OrgPositionLevel.TYPE_SMALL_CLUB, BIG_CLUB_ID);

        assertTrue(
                DepartmentScoreReviewRules.canAuditFirstStage(
                        smallClub,
                        OrgPositionLevel.LEVEL_LEADER
                )
        );

        /* 副部长 / 副主任只能提交，不能审核 */
        assertFalse(
                DepartmentScoreReviewRules.canAuditFirstStage(
                        smallClub,
                        OrgPositionLevel.LEVEL_DEPUTY
                )
        );
    }

    @Test
    void departmentKeepsOriginalAuditPermission() {

        Department department =
                org(1L, OrgPositionLevel.TYPE_DEPARTMENT, null);

        assertTrue(
                DepartmentScoreReviewRules.canAuditFirstStage(
                        department,
                        OrgPositionLevel.LEVEL_LEADER
                )
        );

        assertTrue(
                DepartmentScoreReviewRules.canAuditFirstStage(
                        department,
                        OrgPositionLevel.LEVEL_DEPUTY
                )
        );
    }

    /* =====================================================
     * 大社团审核权限
     * ===================================================== */

    @Test
    void bigClubReviewRequiresParentMatch() {

        Department smallClub =
                org(SMALL_CLUB_ID, OrgPositionLevel.TYPE_SMALL_CLUB, BIG_CLUB_ID);

        Department parent =
                org(BIG_CLUB_ID, OrgPositionLevel.TYPE_BIG_CLUB, null);

        Department otherParent =
                org(OTHER_BIG_CLUB_ID, OrgPositionLevel.TYPE_BIG_CLUB, null);

        assertTrue(
                DepartmentScoreReviewRules.canAuditBigClubStage(
                        smallClub,
                        parent,
                        OrgPositionLevel.LEVEL_LEADER
                )
        );

        assertTrue(
                DepartmentScoreReviewRules.canAuditBigClubStage(
                        smallClub,
                        parent,
                        OrgPositionLevel.LEVEL_DEPUTY
                )
        );

        /* 不是该小社团的上级大社团 → 拒绝 */
        assertFalse(
                DepartmentScoreReviewRules.canAuditBigClubStage(
                        smallClub,
                        otherParent,
                        OrgPositionLevel.LEVEL_LEADER
                )
        );

        /* 普通成员 → 拒绝 */
        assertFalse(
                DepartmentScoreReviewRules.canAuditBigClubStage(
                        smallClub,
                        parent,
                        OrgPositionLevel.LEVEL_MEMBER
                )
        );

        /* 申报不属于小社团 → 拒绝 */
        assertFalse(
                DepartmentScoreReviewRules.canAuditBigClubStage(
                        parent,
                        null,
                        OrgPositionLevel.LEVEL_LEADER
                )
        );
    }

    /* =====================================================
     * 审核阶段流转
     * ===================================================== */

    @Test
    void bigClubStageWaitsForParentReview() {

        DepartmentScoreApply apply = new DepartmentScoreApply();

        apply.setStatus((short) 1);
        apply.setBigClubStatus(
                DepartmentScoreReviewRules.BIG_CLUB_PENDING
        );

        assertTrue(
                DepartmentScoreReviewRules.waitingBigClubReview(apply)
        );

        assertFalse(
                DepartmentScoreReviewRules.readyForArchive(apply)
        );
    }

    @Test
    void archiveRequiresBigClubApproval() {

        DepartmentScoreApply approved = new DepartmentScoreApply();

        approved.setStatus((short) 1);
        approved.setBigClubStatus(
                DepartmentScoreReviewRules.BIG_CLUB_APPROVED
        );

        assertTrue(
                DepartmentScoreReviewRules.readyForArchive(approved)
        );

        DepartmentScoreApply rejected = new DepartmentScoreApply();

        rejected.setStatus((short) 1);
        rejected.setBigClubStatus(
                DepartmentScoreReviewRules.BIG_CLUB_REJECTED
        );

        assertFalse(
                DepartmentScoreReviewRules.readyForArchive(rejected)
        );
    }

    @Test
    void departmentApplyIsReadyForArchiveDirectly() {

        DepartmentScoreApply apply = new DepartmentScoreApply();

        apply.setStatus((short) 1);
        apply.setBigClubStatus(null);

        assertFalse(
                DepartmentScoreReviewRules.waitingBigClubReview(apply)
        );

        assertTrue(
                DepartmentScoreReviewRules.readyForArchive(apply)
        );

        /* 尚未通过组织初审 */
        apply.setStatus((short) 0);

        assertFalse(
                DepartmentScoreReviewRules.readyForArchive(apply)
        );
    }

    private Department org(
            Long id,
            short type,
            Long parentId
    ) {

        Department department =
                new Department();

        department.setId(id);
        department.setDepartmentType(type);
        department.setParentId(parentId);

        return department;
    }
}
