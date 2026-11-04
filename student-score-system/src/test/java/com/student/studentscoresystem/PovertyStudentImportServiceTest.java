package com.student.studentscoresystem;

import com.alibaba.excel.EasyExcel;
import com.student.studentscoresystem.dto.PovertyStudentExcelRow;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.PovertyImportPermissionService;
import com.student.studentscoresystem.service.PovertyStudentImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * =========================================================
 * 贫困生名单导入单元测试
 * =========================================================
 *
 * 重点验证：
 *
 * 1. 名单中的学号会被标记为贫困生；
 * 2. 学号不存在 / 为空只记录失败，不影响其他行；
 * 3. 文件内重复学号只处理一次；
 * 4. 无权限、空文件直接阻断。
 * =========================================================
 */
class PovertyStudentImportServiceTest {

    private static final long OPERATOR_ID = 1L;

    private SysUserMapper sysUserMapper;

    private PovertyImportPermissionService permissionService;

    private PovertyStudentImportService service;

    @BeforeEach
    void setUp() {

        sysUserMapper = mock(SysUserMapper.class);

        permissionService = mock(PovertyImportPermissionService.class);

        service = new PovertyStudentImportService(
                sysUserMapper,
                permissionService
        );

        when(permissionService.canImportPovertyList(any()))
                .thenReturn(true);
    }

    private MockMultipartFile buildFile(
            List<PovertyStudentExcelRow> rows
    ) {

        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        EasyExcel.write(out, PovertyStudentExcelRow.class)
                .sheet("贫困生名单")
                .doWrite(rows);

        return new MockMultipartFile(
                "file",
                "poverty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
        );
    }

    private PovertyStudentExcelRow row(String studentNo, String realName) {

        PovertyStudentExcelRow row =
                new PovertyStudentExcelRow();

        row.setStudentNo(studentNo);
        row.setRealName(realName);

        return row;
    }

    @Test
    void existingStudentIsMarkedAsPoverty() {

        SysUser user = new SysUser();
        user.setId(100L);
        user.setStudentNo("2024001");
        user.setRealName("张三");
        user.setPovertyStatus((short) 0);

        when(sysUserMapper.selectList(any()))
                .thenReturn(List.of(user));

        Map<String, Object> result =
                service.importPovertyStudents(
                        OPERATOR_ID,
                        buildFile(List.of(row("2024001", "张三")))
                );

        assertEquals(1, result.get("totalCount"));
        assertEquals(1, result.get("successCount"));
        assertEquals(0, result.get("failCount"));

        ArgumentCaptor<SysUser> captor =
                ArgumentCaptor.forClass(SysUser.class);

        verify(sysUserMapper).updateById(captor.capture());

        assertEquals(
                Short.valueOf((short) 1),
                captor.getValue().getPovertyStatus()
        );
    }

    @Test
    void unknownStudentNoIsRecordedAsFailure() {

        when(sysUserMapper.selectList(any()))
                .thenReturn(Collections.emptyList());

        Map<String, Object> result =
                service.importPovertyStudents(
                        OPERATOR_ID,
                        buildFile(List.of(row("9999999", "查无此人")))
                );

        assertEquals(1, result.get("totalCount"));
        assertEquals(0, result.get("successCount"));
        assertEquals(1, result.get("failCount"));

        assertTrue(
                result.get("errors") instanceof List
                        && !((List<?>) result.get("errors")).isEmpty()
        );

        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void duplicatedStudentNoIsHandledOnce() {

        SysUser user = new SysUser();
        user.setId(100L);
        user.setStudentNo("2024002");
        user.setRealName("李四");
        user.setPovertyStatus((short) 0);

        when(sysUserMapper.selectList(any()))
                .thenReturn(List.of(user));

        Map<String, Object> result =
                service.importPovertyStudents(
                        OPERATOR_ID,
                        buildFile(
                                List.of(
                                        row("2024002", "李四"),
                                        row("2024002", "李四")
                                )
                        )
                );

        assertEquals(2, result.get("totalCount"));
        assertEquals(1, result.get("successCount"));
        assertEquals(1, result.get("failCount"));
    }

    @Test
    void alreadyPovertyStudentIsNotUpdatedAgain() {

        SysUser user = new SysUser();
        user.setId(100L);
        user.setStudentNo("2024003");
        user.setRealName("王五");
        user.setPovertyStatus((short) 1);

        when(sysUserMapper.selectList(any()))
                .thenReturn(List.of(user));

        Map<String, Object> result =
                service.importPovertyStudents(
                        OPERATOR_ID,
                        buildFile(List.of(row("2024003", "王五")))
                );

        assertEquals(1, result.get("successCount"));

        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void mismatchedRealNameIsRejected() {

        SysUser user = new SysUser();
        user.setId(100L);
        user.setStudentNo("2024004");
        user.setRealName("张三");
        user.setPovertyStatus((short) 0);

        when(sysUserMapper.selectList(any()))
                .thenReturn(List.of(user));

        Map<String, Object> result =
                service.importPovertyStudents(
                        OPERATOR_ID,
                        buildFile(List.of(row("2024004", "李四")))
                );

        assertEquals(1, result.get("totalCount"));
        assertEquals(0, result.get("successCount"));
        assertEquals(1, result.get("failCount"));

        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void missingRealNameIsRejected() {

        SysUser user = new SysUser();
        user.setId(100L);
        user.setStudentNo("2024005");
        user.setRealName("张三");
        user.setPovertyStatus((short) 0);

        when(sysUserMapper.selectList(any()))
                .thenReturn(List.of(user));

        Map<String, Object> result =
                service.importPovertyStudents(
                        OPERATOR_ID,
                        buildFile(List.of(row("2024005", null)))
                );

        assertEquals(1, result.get("totalCount"));
        assertEquals(0, result.get("successCount"));
        assertEquals(1, result.get("failCount"));

        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void emptyFileIsRejected() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "empty.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        new byte[0]
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.importPovertyStudents(OPERATOR_ID, file)
        );
    }

    @Test
    void withoutPermissionIsRejected() {

        when(permissionService.canImportPovertyList(any()))
                .thenReturn(false);

        assertThrows(
                SecurityException.class,
                () -> service.buildTemplateWorkbook(OPERATOR_ID)
        );
    }
}