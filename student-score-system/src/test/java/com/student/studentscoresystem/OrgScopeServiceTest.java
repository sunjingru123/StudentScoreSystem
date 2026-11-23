package com.student.studentscoresystem;

import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.service.OrgScopeService;
import com.student.studentscoresystem.utils.OrgPositionLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 组织管理范围（数据隔离）测试。
 *
 * 规则：
 *
 * 1. 部门负责人只管本部门；
 * 2. 小社团负责人只管本小社团；
 * 3. 大社团负责人管本大社团 + 下属小社团；
 * 4. 不能越权到其它组织。
 */
class OrgScopeServiceTest {

    private static final Long DEPARTMENT_ID = 1L;

    private static final Long BIG_CLUB_ID = 2L;

    private static final Long SMALL_CLUB_A = 3L;

    private static final Long SMALL_CLUB_B = 4L;

    private static final Long OTHER_BIG_CLUB = 5L;

    private static final Long OTHER_SMALL_CLUB = 6L;

    private DepartmentMapper departmentMapper;

    private OrgScopeService service;

    @BeforeEach
    void setUp() {

        departmentMapper = mock(DepartmentMapper.class);

        service = new OrgScopeService(departmentMapper);

        when(departmentMapper.selectList(any()))
                .thenReturn(
                        Arrays.asList(
                                org(DEPARTMENT_ID, "学习部",
                                        OrgPositionLevel.TYPE_DEPARTMENT, null),
                                org(BIG_CLUB_ID, "计算机协会",
                                        OrgPositionLevel.TYPE_BIG_CLUB, null),
                                org(SMALL_CLUB_A, "前端小组",
                                        OrgPositionLevel.TYPE_SMALL_CLUB, BIG_CLUB_ID),
                                org(SMALL_CLUB_B, "后端小组",
                                        OrgPositionLevel.TYPE_SMALL_CLUB, BIG_CLUB_ID),
                                org(OTHER_BIG_CLUB, "摄影协会",
                                        OrgPositionLevel.TYPE_BIG_CLUB, null),
                                org(OTHER_SMALL_CLUB, "风光组",
                                        OrgPositionLevel.TYPE_SMALL_CLUB, OTHER_BIG_CLUB)
                        )
                );
    }

    @Test
    void departmentLeaderOnlyManagesOwnDepartment() {

        List<Long> scope =
                service.expandManagedOrganizationIds(
                        Collections.singletonList(DEPARTMENT_ID)
                );

        assertEquals(
                Collections.singletonList(DEPARTMENT_ID),
                scope
        );
    }

    @Test
    void bigClubLeaderManagesItsOwnSmallClubs() {

        List<Long> scope =
                service.expandManagedOrganizationIds(
                        Collections.singletonList(BIG_CLUB_ID)
                );

        assertTrue(scope.contains(BIG_CLUB_ID));
        assertTrue(scope.contains(SMALL_CLUB_A));
        assertTrue(scope.contains(SMALL_CLUB_B));

        /* 不能越权到其它大社团 */
        assertFalse(scope.contains(OTHER_BIG_CLUB));
        assertFalse(scope.contains(OTHER_SMALL_CLUB));
        assertFalse(scope.contains(DEPARTMENT_ID));
    }

    @Test
    void smallClubLeaderOnlyManagesOwnClub() {

        List<Long> scope =
                service.expandManagedOrganizationIds(
                        Collections.singletonList(SMALL_CLUB_A)
                );

        assertEquals(
                Collections.singletonList(SMALL_CLUB_A),
                scope
        );
    }

    @Test
    void coversRespectsHierarchy() {

        List<Long> bigClubLeader =
                Collections.singletonList(BIG_CLUB_ID);

        assertTrue(
                service.covers(bigClubLeader, SMALL_CLUB_A)
        );

        assertFalse(
                service.covers(bigClubLeader, OTHER_SMALL_CLUB)
        );

        List<Long> smallClubLeader =
                Collections.singletonList(SMALL_CLUB_A);

        assertFalse(
                service.covers(smallClubLeader, BIG_CLUB_ID)
        );

        assertFalse(
                service.covers(smallClubLeader, SMALL_CLUB_B)
        );
    }

    @Test
    void emptyScopeCoversNothing() {

        assertFalse(
                service.covers(Collections.emptyList(), BIG_CLUB_ID)
        );

        assertTrue(
                service.expandManagedOrganizationIds(
                        Collections.emptyList()
                ).isEmpty()
        );
    }

    private Department org(
            Long id,
            String name,
            short type,
            Long parentId
    ) {

        Department department =
                new Department();

        department.setId(id);
        department.setName(name);
        department.setDepartmentType(type);
        department.setParentId(parentId);
        department.setStatus((short) 1);

        return department;
    }
}
