package com.student.studentscoresystem;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.student.studentscoresystem.dto.ClubMemberExcelRow;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysDepartment;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.ScoreRuleMapper;
import com.student.studentscoresystem.mapper.SysDepartmentMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.DepartmentMemberManageService;
import com.student.studentscoresystem.service.DepartmentTemplateSyncService;
import com.student.studentscoresystem.service.impl.ExcelImportServiceImpl;
import com.student.studentscoresystem.utils.OrgPositionLevel;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 社团成员 Excel 导入测试。
 *
 * 重点验证：
 *
 * 1. 大社团 / 小社团自动创建，并建立父子关系；
 * 2. 职位名称写入 position，权限层级写入 positionLevel；
 * 3. 覆盖模式：以 Excel 名单为准调用 syncRoster；
 * 4. 同一组织存在失败行时，跳过该组织的覆盖，避免误移出成员。
 */
class ExcelImportClubMembersTest {

    private static final long STUDENT_ID = 100L;

    private static final long BIG_CLUB_ID = 900L;

    private static final long SMALL_CLUB_ID = 901L;

    private long nextDepartmentId = BIG_CLUB_ID;

    private SysUserMapper sysUserMapper;

    private DepartmentMapper departmentMapper;

    private SysDepartmentMapper sysDepartmentMapper;

    private SysUserDepartmentMapper userDepartmentMapper;

    private SysUserPositionMapper sysUserPositionMapper;

    private SysPositionMapper sysPositionMapper;

    private DepartmentMemberManageService departmentMemberManageService;

    private ExcelImportServiceImpl service;

    @BeforeAll
    static void initTableInfo() {

        /*
         * LambdaQueryWrapper 解析列名依赖 TableInfo 缓存，
         * 单测中没有 Spring 容器，手动初始化。
         */
        if (TableInfoHelper.getTableInfo(Department.class) != null) {

            return;
        }

        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(
                        new MybatisConfiguration(),
                        ""
                );

        assistant.setCurrentNamespace(
                DepartmentMapper.class.getName()
        );

        TableInfoHelper.initTableInfo(
                assistant,
                Department.class
        );
    }

    @BeforeEach
    void setUp() {

        sysUserMapper = mock(SysUserMapper.class);
        departmentMapper = mock(DepartmentMapper.class);
        sysDepartmentMapper = mock(SysDepartmentMapper.class);
        userDepartmentMapper = mock(SysUserDepartmentMapper.class);
        sysUserPositionMapper = mock(SysUserPositionMapper.class);
        sysPositionMapper = mock(SysPositionMapper.class);

        departmentMemberManageService =
                mock(DepartmentMemberManageService.class);

        service = new ExcelImportServiceImpl(
                sysUserMapper,
                departmentMapper,
                sysDepartmentMapper,
                userDepartmentMapper,
                sysUserPositionMapper,
                sysPositionMapper,
                mock(ScoreRuleMapper.class),
                mock(DepartmentTemplateSyncService.class),
                departmentMemberManageService
        );

        when(sysUserMapper.selectOne(any()))
                .thenReturn(student());

        when(sysPositionMapper.selectOne(any()))
                .thenReturn(position());

        when(userDepartmentMapper.insert(any(SysUserDepartment.class)))
                .thenReturn(1);

        when(sysUserPositionMapper.insert(any(SysUserPosition.class)))
                .thenReturn(1);

        when(sysDepartmentMapper.insert(any(SysDepartment.class)))
                .thenReturn(1);

        /*
         * 自动创建组织时回填 ID，并模拟数据库按名称查重
         */
        List<Department> store =
                new ArrayList<>();

        when(departmentMapper.selectOne(any()))
                .thenAnswer(invocation -> {

                    String name =
                            firstNameOf(
                                    invocation.getArgument(0)
                            );

                    if (name == null) {

                        return null;
                    }

                    return store.stream()
                            .filter(item -> name.equals(item.getName()))
                            .findFirst()
                            .orElse(null);
                });

        when(departmentMapper.insert(any(Department.class)))
                .thenAnswer(invocation -> {

                    Department department =
                            invocation.getArgument(0);

                    department.setId(
                            nextDepartmentId++
                    );

                    store.add(
                            department
                    );

                    return 1;
                });
    }

    /**
     * 从 LambdaQueryWrapper 中取出查询条件里的字符串参数，
     * 用于模拟按名称查重。
     */
    @SuppressWarnings("unchecked")
    private String firstNameOf(
            Object wrapper
    ) {

        if (!(wrapper instanceof AbstractWrapper)) {

            return null;
        }

        AbstractWrapper<?, ?, ?> condition =
                (AbstractWrapper<?, ?, ?>) wrapper;

        /*
         * MyBatis-Plus 的查询参数是在渲染 SQL 时才写入
         * paramNameValuePairs 的，这里先渲染一次。
         */
        try {

            condition.getSqlSegment();

        } catch (Exception ignored) {

            // 渲染失败时按“查不到”处理
        }

        Map<String, Object> params =
                condition.getParamNameValuePairs();


        for (Object value : params.values()) {

            if (value instanceof String) {

                return (String) value;
            }
        }

        return null;
    }

    @Test
    void clubImportCreatesHierarchyAndSyncsRoster() throws Exception {

        Map<String, Object> result =
                service.importClubMembers(
                        workbook(
                                row("计算机协会", "", "张一", "20240001", "理事长"),
                                row("计算机协会", "前端小组", "张一", "20240001", "部长")
                        )
                );

        assertEquals(2, result.get("successCount"));
        assertEquals(0, result.get("failCount"));

        /* 两个组织：大社团 + 小社团，且建立父子关系 */
        ArgumentCaptor<Department> departmentCaptor =
                ArgumentCaptor.forClass(Department.class);

        verify(departmentMapper, atLeastOnce())
                .insert(departmentCaptor.capture());

        List<Department> created =
                departmentCaptor.getAllValues();

        assertEquals(2, created.size());

        Department bigClub =
                created.stream()
                        .filter(item -> item.getDepartmentType()
                                == OrgPositionLevel.TYPE_BIG_CLUB)
                        .findFirst()
                        .orElse(null);

        Department smallClub =
                created.stream()
                        .filter(item -> item.getDepartmentType()
                                == OrgPositionLevel.TYPE_SMALL_CLUB)
                        .findFirst()
                        .orElse(null);

        assertNotNull(bigClub);
        assertNotNull(smallClub);
        assertEquals(
                bigClub.getId(),
                smallClub.getParentId()
        );

        /* 职位名称保留，权限层级归一 */
        ArgumentCaptor<SysUserDepartment> relationCaptor =
                ArgumentCaptor.forClass(SysUserDepartment.class);

        verify(userDepartmentMapper, atLeastOnce())
                .insert(relationCaptor.capture());

        List<SysUserDepartment> relations =
                relationCaptor.getAllValues();

        assertEquals(2, relations.size());

        for (SysUserDepartment relation : relations) {

            assertEquals(
                    OrgPositionLevel.LEVEL_LEADER,
                    relation.getPositionLevel()
            );
        }

        /* 覆盖：两个组织各同步一次 */
        verify(departmentMemberManageService)
                .syncRoster(
                        eq(BIG_CLUB_ID),
                        any()
                );

        verify(departmentMemberManageService)
                .syncRoster(
                        eq(SMALL_CLUB_ID),
                        any()
                );
    }

    @Test
    void clubImportSkipsRosterSyncWhenOrgHasFailedRow() throws Exception {

        Map<String, Object> result =
                service.importClubMembers(
                        workbook(
                                row("计算机协会", "前端小组", "张一", "20240001", "部长"),
                                row("", "", "张一", "", "干事")
                        )
                );

        assertEquals(1, result.get("successCount"));
        assertEquals(1, result.get("failCount"));

        /*
         * 该小社团存在失败行 → 不执行覆盖
         */
        verify(departmentMemberManageService, never())
                .syncRoster(anyLong(), any());

        assertEquals(
                0,
                result.get("removedCount")
        );

        List<?> skipped =
                (List<?>) result.get("skippedDepartments");

        assertEquals(1, skipped.size());
        assertEquals(
                "前端小组",
                skipped.get(0)
        );
    }

    /* =====================================================
     * 辅助
     * ===================================================== */

    private ClubMemberExcelRow row(
            String bigClubName,
            String smallClubName,
            String realName,
            String studentNo,
            String position
    ) {

        ClubMemberExcelRow row =
                new ClubMemberExcelRow();

        row.setBigClubName(bigClubName);
        row.setSmallClubName(smallClubName);
        row.setRealName(realName);
        row.setStudentNo(studentNo);
        row.setPosition(position);

        return row;
    }

    private MockMultipartFile workbook(
            ClubMemberExcelRow... rows
    ) {

        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        List<ClubMemberExcelRow> data =
                new ArrayList<>(List.of(rows));

        EasyExcel.write(out, ClubMemberExcelRow.class)
                .sheet("社团成员")
                .doWrite(data);

        return new MockMultipartFile(
                "file",
                "club-members.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
        );
    }

    private SysUser student() {

        SysUser user =
                new SysUser();

        user.setId(STUDENT_ID);
        user.setStudentNo("20240001");
        user.setRealName("张一");

        return user;
    }

    private SysPosition position() {

        SysPosition position =
                new SysPosition();

        position.setId(8L);
        position.setName("理事长");
        position.setLevel(OrgPositionLevel.LEVEL_LEADER);
        position.setStatus((short) 1);

        return position;
    }
}
