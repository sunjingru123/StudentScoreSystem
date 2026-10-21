package com.student.studentscoresystem;

import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.entity.DepartmentScoreTemplate;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.DepartmentScoreApplyImportService;
import com.student.studentscoresystem.service.IDepartmentScoreApplyService;
import com.student.studentscoresystem.service.IDepartmentScoreTemplateService;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 部门加减分申报 Excel 导入测试。
 *
 * 覆盖：
 *
 * 1. 模板「项目」列只取该部门已启用项目，并带下拉有效性；
 * 2. 正常行入库（分值/类型取自项目，日期写入 activity_date）；
 * 3. 学号不存在、姓名不匹配、项目不在列表、分值不一致、重复申报
 *    都必须按行失败，不能静默写入；
 * 4. 同名不同分值项目按分值精确匹配，避免分值漂移；
 * 5. 非本部门干部不能导入。
 */
@ExtendWith(MockitoExtension.class)
class DepartmentScoreApplyImportServiceTest {

    private static final Long DEPARTMENT_ID = 5L;

    private static final Long APPLICANT_ID = 100L;

    private static final Long STUDENT_ID = 200L;

    private static final String STUDENT_NO = "20240101";

    private static final String STUDENT_NAME = "张三";

    private static final String PROJECT_NAME = "教室卫生检查：卫生优良";

    private static final String[] HEADERS = {
            "学号", "姓名", "日期", "项目", "分值", "备注"
    };

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysUserDepartmentMapper userDepartmentMapper;

    @Mock
    private IDepartmentScoreTemplateService templateService;

    @Mock
    private IDepartmentScoreApplyService applyService;

    private DepartmentScoreApplyImportService service;

    private List<DepartmentScoreTemplate> templates;

    private final List<DepartmentScoreApply> saved = new ArrayList<>();

    private DepartmentScoreTemplate bonusOne;

    private DepartmentScoreTemplate bonusHalf;

    @BeforeEach
    void setUp() {

        service = new DepartmentScoreApplyImportService(
                departmentMapper,
                sysUserMapper,
                userDepartmentMapper,
                templateService,
                applyService
        );

        saved.clear();

        bonusOne = template(1L, PROJECT_NAME, (short) 1, "1.00");

        bonusHalf = template(2L, PROJECT_NAME, (short) 1, "0.50");

        templates = new ArrayList<>();

        templates.add(bonusOne);

        lenient()
                .when(departmentMapper.selectById(any()))
                .thenReturn(department());

        lenient()
                .when(userDepartmentMapper.selectOne(any()))
                .thenReturn(relation("部长"));

        lenient()
                .when(sysUserMapper.selectOne(any()))
                .thenReturn(student());

        lenient()
                .when(templateService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenAnswer(invocation -> new ArrayList<>(templates));

        lenient()
                .when(applyService.count(any()))
                .thenReturn(0L);

        AtomicLong nextId = new AtomicLong(1000L);

        lenient()
                .when(applyService.save(any(DepartmentScoreApply.class)))
                .thenAnswer(invocation -> {

                    DepartmentScoreApply apply =
                            invocation.getArgument(0);

                    apply.setId(nextId.getAndIncrement());

                    saved.add(apply);

                    return true;
                });
    }

    @Test
    void templateOnlyContainsDepartmentEnabledProjects() throws Exception {

        byte[] bytes = service.buildTemplateWorkbook(
                DEPARTMENT_ID,
                APPLICANT_ID
        );

        assertNotNull(bytes);

        try (XSSFWorkbook workbook =
                     new XSSFWorkbook(new ByteArrayInputStream(bytes))) {

            Sheet sheet = workbook.getSheetAt(0);

            assertEquals("申报导入模板", sheet.getSheetName());

            assertEquals(6, sheet.getRow(0).getLastCellNum());

            for (int i = 0; i < HEADERS.length; i++) {

                assertEquals(
                        HEADERS[i],
                        sheet.getRow(0).getCell(i).getStringCellValue()
                );
            }

            /*
             * 「项目」列必须有下拉数据有效性
             */
            List<? extends DataValidation> validations =
                    sheet.getDataValidations();

            assertEquals(1, validations.size());

            assertEquals(
                    3,
                    validations.get(0)
                            .getRegions()
                            .getCellRangeAddress(0)
                            .getFirstColumn()
            );

            /*
             * 下拉选项来自隐藏 sheet，且只包含本部门项目
             */
            assertTrue(workbook.isSheetHidden(1));

            Sheet optionSheet = workbook.getSheetAt(1);

            assertEquals("项目选项", optionSheet.getSheetName());

            assertEquals(
                    PROJECT_NAME + "（+1分）",
                    optionSheet.getRow(0).getCell(0).getStringCellValue()
            );

            assertFalse(
                    workbook.getSheetAt(2).getSheetName().isEmpty()
            );
        }
    }

    @Test
    void importUsesTemplateScoreAndActivityDate() throws Exception {

        Map<String, Object> result = importRows(
                new String[][]{
                        {STUDENT_NO, STUDENT_NAME, "2024-05-01", PROJECT_NAME, "1", "表现优秀"}
                }
        );

        assertEquals(1, result.get("successCount"));
        assertEquals(0, result.get("failCount"));

        assertEquals(1, saved.size());

        DepartmentScoreApply apply = saved.get(0);

        assertEquals(STUDENT_ID, apply.getStudentId());
        assertEquals(APPLICANT_ID, apply.getApplicantId());
        assertEquals(DEPARTMENT_ID, apply.getDepartmentId());
        assertEquals(bonusOne.getId(), apply.getTemplateId());
        assertEquals((short) 1, apply.getScoreType());
        assertEquals(0, apply.getScore().compareTo(new BigDecimal("1.00")));
        assertEquals(PROJECT_NAME, apply.getTitle());
        assertEquals("表现优秀", apply.getDescription());
        assertEquals(LocalDate.of(2024, 5, 1), apply.getActivityDate());
        assertEquals((short) 0, apply.getStatus());
        assertEquals((short) 0, apply.getFinalStatus());
    }

    @Test
    void unknownStudentNoFailsWithReason() throws Exception {

        lenient()
                .when(sysUserMapper.selectOne(any()))
                .thenReturn(null);

        Map<String, Object> result = importRows(
                new String[][]{
                        {"99999999", STUDENT_NAME, "", PROJECT_NAME, "", ""}
                }
        );

        assertEquals(0, result.get("successCount"));
        assertEquals(1, result.get("failCount"));
        assertEquals(0, saved.size());

        assertTrue(
                firstErrorMessage(result).contains("学号不存在")
        );
    }

    @Test
    void nameMismatchFails() throws Exception {

        Map<String, Object> result = importRows(
                new String[][]{
                        {STUDENT_NO, "李四", "", PROJECT_NAME, "", ""}
                }
        );

        assertEquals(0, result.get("successCount"));
        assertEquals(1, result.get("failCount"));

        assertTrue(
                firstErrorMessage(result).contains("姓名与学号不匹配")
        );
    }

    @Test
    void projectOutsideDepartmentFails() throws Exception {

        Map<String, Object> result = importRows(
                new String[][]{
                        {STUDENT_NO, STUDENT_NAME, "", "不存在的项目", "", ""}
                }
        );

        assertEquals(0, result.get("successCount"));
        assertEquals(1, result.get("failCount"));

        assertTrue(
                firstErrorMessage(result).contains("不在本部门可用项目中")
        );
    }

    @Test
    void scoreMismatchFails() throws Exception {

        Map<String, Object> result = importRows(
                new String[][]{
                        {STUDENT_NO, STUDENT_NAME, "", PROJECT_NAME, "2", ""}
                }
        );

        assertEquals(0, result.get("successCount"));
        assertEquals(1, result.get("failCount"));

        assertTrue(
                firstErrorMessage(result).contains("分值与该项目的分值不一致")
        );
    }

    @Test
    void duplicateApplyFails() throws Exception {

        lenient()
                .when(applyService.count(any()))
                .thenReturn(1L);

        Map<String, Object> result = importRows(
                new String[][]{
                        {STUDENT_NO, STUDENT_NAME, "", PROJECT_NAME, "", ""}
                }
        );

        assertEquals(0, result.get("successCount"));
        assertEquals(1, result.get("failCount"));

        assertTrue(
                firstErrorMessage(result).contains("已经申报")
        );

        verify(applyService, never())
                .save(any(DepartmentScoreApply.class));
    }

    @Test
    void sameNameDifferentScoreUsesScoreColumn() throws Exception {

        templates.add(bonusHalf);

        Map<String, Object> result = importRows(
                new String[][]{
                        {STUDENT_NO, STUDENT_NAME, "", PROJECT_NAME, "0.5", ""}
                }
        );

        assertEquals(1, result.get("successCount"));
        assertEquals(1, saved.size());

        assertEquals(
                bonusHalf.getId(),
                saved.get(0).getTemplateId()
        );

        assertEquals(
                0,
                saved.get(0).getScore()
                        .compareTo(new BigDecimal("0.50"))
        );
    }

    @Test
    void ambiguousSameNameProjectFails() throws Exception {

        templates.add(bonusHalf);

        Map<String, Object> result = importRows(
                new String[][]{
                        {STUDENT_NO, STUDENT_NAME, "", PROJECT_NAME, "", ""}
                }
        );

        assertEquals(0, result.get("successCount"));
        assertEquals(1, result.get("failCount"));

        assertTrue(
                firstErrorMessage(result).contains("同名多项")
        );
    }

    @Test
    void nonDepartmentLeaderCannotImport() throws Exception {

        lenient()
                .when(userDepartmentMapper.selectOne(any()))
                .thenReturn(relation("学生"));

        assertThrows(
                IllegalArgumentException.class,
                () -> importRows(
                        new String[][]{
                                {STUDENT_NO, STUDENT_NAME, "", PROJECT_NAME, "", ""}
                        }
                )
        );

        assertEquals(0, saved.size());
    }

    @Test
    void invalidDateFails() throws Exception {

        Map<String, Object> result = importRows(
                new String[][]{
                        {STUDENT_NO, STUDENT_NAME, "2024年5月1日", PROJECT_NAME, "", ""}
                }
        );

        assertEquals(0, result.get("successCount"));
        assertEquals(1, result.get("failCount"));

        assertTrue(
                firstErrorMessage(result).contains("日期格式不正确")
        );
    }

    @Test
    void reorderedHeaderIsRejected() throws Exception {

        String[][] values = {
                {STUDENT_NO, STUDENT_NAME, "", PROJECT_NAME, "", ""}
        };

        MockMultipartFile file = excelWithHeaders(
                new String[]{
                        "姓名", "学号", "日期", "项目", "分值", "备注"
                },
                values
        );

        IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.importApplies(
                                DEPARTMENT_ID,
                                APPLICANT_ID,
                                file
                        )
                );

        assertTrue(
                error.getMessage().contains("模板表头不正确")
        );

        assertEquals(0, saved.size());
    }

    /* ======================================================
     * 辅助
     * ====================================================== */

    private Map<String, Object> importRows(
            String[][] rows) throws Exception {

        return service.importApplies(
                DEPARTMENT_ID,
                APPLICANT_ID,
                excel(rows)
        );
    }

    @SuppressWarnings("unchecked")
    private String firstErrorMessage(
            Map<String, Object> result) {

        List<Map<String, Object>> errors =
                (List<Map<String, Object>>) result.get("errors");

        assertNotNull(errors);

        assertFalse(errors.isEmpty());

        Object message = errors.get(0).get("message");

        return message == null
                ? ""
                : message.toString();
    }

    private MockMultipartFile excel(
            String[][] rows) throws Exception {

        return excelWithHeaders(HEADERS, rows);
    }

    private MockMultipartFile excelWithHeaders(
            String[] headers,
            String[][] rows) throws Exception {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("申报");

            Row header = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {

                header.createCell(i).setCellValue(headers[i]);
            }

            for (int r = 0; r < rows.length; r++) {

                Row row = sheet.createRow(r + 1);

                for (int c = 0; c < rows[r].length; c++) {

                    if (rows[r][c] != null) {

                        row.createCell(c).setCellValue(rows[r][c]);
                    }
                }
            }

            workbook.write(out);

            return new MockMultipartFile(
                    "file",
                    "apply.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray()
            );
        }
    }

    private DepartmentScoreTemplate template(
            Long id,
            String name,
            Short scoreType,
            String score) {

        DepartmentScoreTemplate template =
                new DepartmentScoreTemplate();

        template.setId(id);
        template.setDepartmentId(DEPARTMENT_ID);
        template.setName(name);
        template.setScoreType(scoreType);
        template.setScore(new BigDecimal(score));
        template.setStatus((short) 1);
        template.setDescription("项目说明");

        return template;
    }

    private Department department() {

        Department department =
                new Department();

        department.setId(DEPARTMENT_ID);
        department.setName("学习部");
        department.setStatus((short) 1);

        return department;
    }

    private SysUser student() {

        SysUser student =
                new SysUser();

        student.setId(STUDENT_ID);
        student.setStudentNo(STUDENT_NO);
        student.setRealName(STUDENT_NAME);
        student.setStatus((short) 1);

        return student;
    }

    private SysUserDepartment relation(
            String position) {

        SysUserDepartment relation =
                new SysUserDepartment();

        relation.setUserId(APPLICANT_ID);
        relation.setDepartmentId(DEPARTMENT_ID);
        relation.setPosition(position);
        relation.setStatus((short) 1);

        return relation;
    }
}
