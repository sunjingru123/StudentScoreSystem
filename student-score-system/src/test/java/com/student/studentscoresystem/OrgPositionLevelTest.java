package com.student.studentscoresystem;

import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.utils.OrgPositionLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组织职位 -> 权限层级映射测试。
 *
 * 关键约定：
 *
 * 不同组织可以使用不同职位名称，
 * 但同一层级必须拥有完全一致的系统权限。
 */
class OrgPositionLevelTest {

    @Test
    void leaderTitlesMapToLeaderLevel() {

        assertEquals(
                OrgPositionLevel.LEVEL_LEADER,
                OrgPositionLevel.resolveLevel("部长")
        );

        assertEquals(
                OrgPositionLevel.LEVEL_LEADER,
                OrgPositionLevel.resolveLevel("站长")
        );

        assertEquals(
                OrgPositionLevel.LEVEL_LEADER,
                OrgPositionLevel.resolveLevel("理事长")
        );

        assertEquals(
                OrgPositionLevel.LEVEL_LEADER,
                OrgPositionLevel.resolveLevel("主席")
        );

        assertEquals(
                OrgPositionLevel.LEVEL_LEADER,
                OrgPositionLevel.resolveLevel("主任")
        );
    }

    @Test
    void deputyTitlesMapToDeputyLevel() {

        assertEquals(
                OrgPositionLevel.LEVEL_DEPUTY,
                OrgPositionLevel.resolveLevel("副部长")
        );

        assertEquals(
                OrgPositionLevel.LEVEL_DEPUTY,
                OrgPositionLevel.resolveLevel("副站长")
        );

        assertEquals(
                OrgPositionLevel.LEVEL_DEPUTY,
                OrgPositionLevel.resolveLevel("副理事长")
        );

        assertEquals(
                OrgPositionLevel.LEVEL_DEPUTY,
                OrgPositionLevel.resolveLevel("副主席")
        );

        assertEquals(
                OrgPositionLevel.LEVEL_DEPUTY,
                OrgPositionLevel.resolveLevel("副主任")
        );
    }

    @Test
    void unknownTitleIsRejected() {

        assertNull(
                OrgPositionLevel.resolveLevel("普通成员")
        );

        assertFalse(
                OrgPositionLevel.isValidTitle("普通成员")
        );

        assertEquals(
                OrgPositionLevel.LEVEL_MEMBER,
                OrgPositionLevel.resolveLevelOrDefault("普通成员")
        );
    }

    @Test
    void titleValidationDependsOnOrgType() {

        /* 部门：允许站长，不允许理事长 */
        assertTrue(
                OrgPositionLevel.isValidTitleForOrgType(
                        "站长",
                        (int) OrgPositionLevel.TYPE_DEPARTMENT
                )
        );

        assertFalse(
                OrgPositionLevel.isValidTitleForOrgType(
                        "理事长",
                        (int) OrgPositionLevel.TYPE_DEPARTMENT
                )
        );

        /* 大社团：允许理事长，不允许站长 */
        assertTrue(
                OrgPositionLevel.isValidTitleForOrgType(
                        "理事长",
                        (int) OrgPositionLevel.TYPE_BIG_CLUB
                )
        );

        assertFalse(
                OrgPositionLevel.isValidTitleForOrgType(
                        "站长",
                        (int) OrgPositionLevel.TYPE_BIG_CLUB
                )
        );

        /* 小社团：允许主任，不允许理事长 */
        assertTrue(
                OrgPositionLevel.isValidTitleForOrgType(
                        "主任",
                        (int) OrgPositionLevel.TYPE_SMALL_CLUB
                )
        );

        assertFalse(
                OrgPositionLevel.isValidTitleForOrgType(
                        "理事长",
                        (int) OrgPositionLevel.TYPE_SMALL_CLUB
                )
        );
    }

    @Test
    void leaderOrDeputyMatchesBothLevels() {

        assertTrue(
                OrgPositionLevel.isLeaderOrDeputy(
                        OrgPositionLevel.LEVEL_LEADER
                )
        );

        assertTrue(
                OrgPositionLevel.isLeaderOrDeputy(
                        OrgPositionLevel.LEVEL_DEPUTY
                )
        );

        assertFalse(
                OrgPositionLevel.isLeaderOrDeputy(
                        OrgPositionLevel.LEVEL_MEMBER
                )
        );
    }

    @Test
    void levelOfFallsBackToPositionTitle() {

        SysUserDepartment relation =
                new SysUserDepartment();

        relation.setPosition("站长");

        /* position_level 为空的历史数据 */
        assertEquals(
                OrgPositionLevel.LEVEL_LEADER,
                OrgPositionLevel.levelOf(relation)
        );

        /* 已有层级时以层级为准 */
        relation.setPositionLevel(
                OrgPositionLevel.LEVEL_DEPUTY
        );

        assertEquals(
                OrgPositionLevel.LEVEL_DEPUTY,
                OrgPositionLevel.levelOf(relation)
        );
    }
}
