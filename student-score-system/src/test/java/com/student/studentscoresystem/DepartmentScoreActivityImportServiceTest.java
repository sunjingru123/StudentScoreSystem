package com.student.studentscoresystem;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.DepartmentScoreActivity;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.DepartmentScoreActivityMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.DepartmentScoreActivityImportService;
import com.student.studentscoresystem.service.IDepartmentScoreApplyService;
import com.student.studentscoresystem.utils.OrgPositionLevel;
import com.student.studentscoresystem.vo.ActivityBatchPreviewVO;
import com.student.studentscoresystem.vo.ActivityBatchRowVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 活动批量加分申报测试。
 *
 * 覆盖：
 *
 * 1. 活动基本信息 + 多名学生的预览；
 * 2. 每名学生分值与凭证可以不同；
 * 3. 错误学号 / 非法分值 / 表内重复只影响当前行；
 * 4. 无申报权限时整表阻断；
 * 5. 导入落库时所有学生关联到同一个活动。
 */
class DepartmentScoreActivityImportServiceTest {

    private static final Long APPLICANT_ID = 100L;

    private static final Long DEPARTMENT_ID = 10L;

    private static final Long ACTIVITY_ID = 900L;

    private static final Long STUDENT_A_ID = 201L;

    private static final Long STUDENT_B_ID = 202L;

    private DepartmentMapper departmentMapper;

    private SysUserMapper sysUserMapper;

    private SysUserDepartmentMapper userDepartmentMapper;

    private DepartmentScoreActivityMapper activityMapper;

    private IDepartmentScoreApplyService applyService;

    private DepartmentScoreActivityImportService service;

    @BeforeAll
    static void initTableInfo() {

        initTableInfo(Department.class);
        initTableInfo(SysUser.class);
        initTableInfo(SysUserDepartment.class);
        initTableInfo(DepartmentScoreActivity.class);
        initTableInfo(DepartmentScoreApply.class);
    }

    private static void initTableInfo(Class<?> entity) {

        if (TableInfoHelper.getTableInfo(entity) != null) {
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

        TableInfoHelper.initTableInfo(assistant, entity);
    }

    @BeforeEach
    void setUp() {

        departmentMapper = mock(DepartmentMapper.class);
        sysUserMapper = mock(SysUserMapper.class);
        userDepartmentMapper = mock(SysUserDepartmentMapper.class);
        activityMapper = mock(DepartmentScoreActivityMapper.class);
        applyService = mock(IDepartmentScoreApplyService.class);

        service = new DepartmentScoreActivityImportService(
                departmentMapper,
                sysUserMapper,
                userDepartmentMapper,
                activityMapper,
                applyService
        );

        when(departmentMapper.selectOne(any()))
                .thenReturn(department());

        when(sysUserMapper.selectOne(any()))
                .thenAnswer(invocation -> {

                    String studentNo = firstNameOf(invocation.getArgument(0));

                    if ("20240001".equals(studentNo)) {
                        return student(STUDENT_A_ID, "20240001", "张一");
                    }

                    if ("20240002".equals(studentNo)) {
                        return student(STUDENT_B_ID, "20240002", "张二");
                    }

                    return null;
                });

        when(userDepartmentMapper.selectOne(any()))
                .thenReturn(relation(OrgPositionLevel.LEVEL_LEADER));

        /* 无历史活动 */
        when(activityMapper.selectOne(any())).thenReturn(null);

        when(activityMapper.insert(any(DepartmentScoreActivity.class)))
                .thenAnswer(invocation -> {

                    DepartmentScoreActivity activity =
                            invocation.getArgument(0);

                    activity.setId(ACTIVITY_ID);

                    return 1;
                });

        when(applyService.list(
                org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<DepartmentScoreApply>>any()
        )).thenReturn(new ArrayList<>());
        when(applyService.save(any(DepartmentScoreApply.class))).thenReturn(true);
    }

    @Test
    void previewSupportsTwoColumnBlocksLikeWordForm() {

        ActivityBatchPreviewVO vo =
                service.preview(
                        twoBlockWorkbook("2024-05-01"),
                        APPLICANT_ID
                );

        assertNull(vo.getMessage());
        assertEquals("校园歌手大赛", vo.getActivityName());
        assertEquals("学习部", vo.getDepartmentName());
        assertEquals("2024-05-01", vo.getActivityDate());
        assertEquals("大学生活动中心", vo.getLocation());
        assertEquals("张一、张二", vo.getOrganizers());

        /* 左右两栏各一名学生，共 2 行 */
        assertEquals(2, vo.getRows().size());
        assertEquals(2, vo.getValidCount());

        ActivityBatchRowVO left = vo.getRows().get(0);
        ActivityBatchRowVO right = vo.getRows().get(1);

        assertEquals("20240001", left.getStudentNo());
        assertEquals("证书A", left.getEvidence());
        assertEquals("2", left.getScore().stripTrailingZeros().toPlainString());

        assertEquals("20240002", right.getStudentNo());
        assertEquals("证书B", right.getEvidence());
        assertEquals("1.5", right.getScore().stripTrailingZeros().toPlainString());
    }

    @Test
    void previewShowsActivityAndStudentsWithDifferentScores() {

        ActivityBatchPreviewVO vo =
                service.preview(
                        workbook(
                                row("张一", "25级1班", "20240001", "2", "证书A"),
                                row("张二", "25级1班", "20240002", "1.5", "证书B")
                        ),
                        APPLICANT_ID
                );

        assertNull(vo.getMessage());

        assertEquals("校园歌手大赛", vo.getActivityName());
        assertEquals("学习部", vo.getDepartmentName());
        assertEquals("2024-05-01", vo.getActivityDate());
        assertEquals("大学生活动中心", vo.getLocation());
        assertEquals("张一、张二", vo.getOrganizers());

        assertEquals(2, vo.getRows().size());
        assertEquals(2, vo.getValidCount());
        assertEquals(0, vo.getInvalidCount());

        ActivityBatchRowVO first = vo.getRows().get(0);
        ActivityBatchRowVO second = vo.getRows().get(1);

        assertEquals("2", first.getScore().stripTrailingZeros().toPlainString());
        assertEquals("1.5", second.getScore().stripTrailingZeros().toPlainString());
        assertEquals("证书A", first.getEvidence());
        assertEquals("证书B", second.getEvidence());
    }

    @Test
    void invalidRowsDoNotAffectValidOnes() {

        ActivityBatchPreviewVO vo =
                service.preview(
                        workbook(
                                row("张一", "25级1班", "20240001", "2", ""),
                                row("查无此人", "25级1班", "99999999", "1", ""),
                                row("张二", "25级1班", "20240002", "0", "")
                        ),
                        APPLICANT_ID
                );

        assertEquals(3, vo.getRows().size());
        assertEquals(1, vo.getValidCount());
        assertEquals(2, vo.getInvalidCount());

        assertNotNull(vo.getRows().get(1).getError());
        assertTrue(vo.getRows().get(1).getError().contains("不存在该学号"));

        assertTrue(vo.getRows().get(2).getError().contains("大于 0"));
    }

    @Test
    void duplicateStudentInSameFileIsRejected() {

        ActivityBatchPreviewVO vo =
                service.preview(
                        workbook(
                                row("张一", "25级1班", "20240001", "2", ""),
                                row("张一", "25级1班", "20240001", "2", "")
                        ),
                        APPLICANT_ID
                );

        assertEquals(1, vo.getValidCount());
        assertEquals(1, vo.getInvalidCount());
        assertTrue(vo.getRows().get(1).getError().contains("重复"));
    }

    @Test
    void ordinaryMemberCannotImport() {

        when(userDepartmentMapper.selectOne(any()))
                .thenReturn(relation(OrgPositionLevel.LEVEL_MEMBER));

        ActivityBatchPreviewVO vo =
                service.preview(
                        workbook(row("张一", "25级1班", "20240001", "2", "")),
                        APPLICANT_ID
                );

        assertNotNull(vo.getMessage());
        assertTrue(vo.getMessage().contains("没有活动加分申报权限"));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.importBatch(
                        workbook(row("张一", "25级1班", "20240001", "2", "")),
                        APPLICANT_ID
                )
        );
    }

    @Test
    void importLinksStudentsToSameActivity() {

        Map<String, Object> result =
                service.importBatch(
                        workbook(
                                row("张一", "25级1班", "20240001", "2", "证书A"),
                                row("张二", "25级1班", "20240002", "3", "证书B")
                        ),
                        APPLICANT_ID
                );

        assertEquals(2, result.get("successCount"));
        assertEquals(0, result.get("failCount"));
        assertEquals(ACTIVITY_ID, result.get("activityId"));

        ArgumentCaptor<DepartmentScoreApply> captor =
                ArgumentCaptor.forClass(DepartmentScoreApply.class);

        org.mockito.Mockito.verify(applyService, org.mockito.Mockito.times(2))
                .save(captor.capture());

        List<DepartmentScoreApply> saved = captor.getAllValues();

        for (DepartmentScoreApply apply : saved) {

            assertEquals(ACTIVITY_ID, apply.getActivityId());
            assertEquals(DEPARTMENT_ID, apply.getDepartmentId());
            assertEquals(APPLICANT_ID, apply.getApplicantId());
            assertEquals((short) 1, apply.getScoreType());
            assertEquals("校园歌手大赛", apply.getTitle());
            assertNull(apply.getTemplateId());
        }

        assertEquals(STUDENT_A_ID, saved.get(0).getStudentId());
        assertEquals(STUDENT_B_ID, saved.get(1).getStudentId());
    }

    @Test
    void previewAcceptsDotDate() {

        ActivityBatchPreviewVO vo =
                service.preview(
                        twoBlockWorkbook("2026.10.22"),
                        APPLICANT_ID
                );

        assertNull(vo.getMessage());
        assertEquals("2026.10.22", vo.getActivityDate());
        assertEquals(2, vo.getValidCount());
    }

    @Test
    void previewAcceptsDateRange() {

        ActivityBatchPreviewVO vo =
                service.preview(
                        twoBlockWorkbook("2026.9.20-2026.9.24"),
                        APPLICANT_ID
                );

        assertNull(vo.getMessage());
        assertEquals("2026.9.20-2026.9.24", vo.getActivityDate());
        assertEquals(2, vo.getValidCount());
    }

    @Test
    void previewAcceptsChineseDateRange() {

        ActivityBatchPreviewVO vo =
                service.preview(
                        twoBlockWorkbook("2026年9月20日至2026年9月24日"),
                        APPLICANT_ID
                );

        assertNull(vo.getMessage());
        assertEquals(2, vo.getValidCount());
    }

    @Test
    void importStoresRangeStartDateAndKeepsRangeText() {

        Map<String, Object> result =
                service.importBatch(
                        twoBlockWorkbook("2026.9.20-2026.9.24"),
                        APPLICANT_ID
                );

        assertEquals(2, result.get("successCount"));

        ArgumentCaptor<DepartmentScoreActivity> activityCaptor =
                ArgumentCaptor.forClass(DepartmentScoreActivity.class);

        org.mockito.Mockito.verify(activityMapper)
                .insert(activityCaptor.capture());

        DepartmentScoreActivity activity = activityCaptor.getValue();

        assertEquals(LocalDate.of(2026, 9, 20), activity.getActivityDate());
        assertTrue(activity.getDescription().contains("2026.9.20-2026.9.24"));

        ArgumentCaptor<DepartmentScoreApply> applyCaptor =
                ArgumentCaptor.forClass(DepartmentScoreApply.class);

        org.mockito.Mockito.verify(applyService, org.mockito.Mockito.times(2))
                .save(applyCaptor.capture());

        for (DepartmentScoreApply apply : applyCaptor.getAllValues()) {
            assertEquals(LocalDate.of(2026, 9, 20), apply.getActivityDate());
        }
    }

    @Test
    void templateCanBeGenerated() {

        byte[] template = service.buildTemplateWorkbook();

        assertNotNull(template);
        assertTrue(template.length > 1000);
    }

    @Test
    void shippedWordTemplateIsServedAsIs() {

        byte[] template = service.buildTemplateDocument();

        assertNotNull(template);
        assertTrue(template.length > 1000);

        /* docx 实为 zip 包，文件头为 PK */
        assertEquals('P', (char) template[0]);
        assertEquals('K', (char) template[1]);

        ActivityBatchPreviewVO vo = service.preview(
                new MockMultipartFile(
                        "file",
                        "人工智能与计算机学院加分统计表.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        template
                ),
                APPLICANT_ID
        );

        /* 下发的就是原表，未填写内容：能解析出表格，但提示补全活动信息 */
        assertNotNull(vo);
        assertEquals("活动名称不能为空", vo.getMessage());
        assertTrue(vo.getRows().isEmpty());
    }

    @Test
    void realTemplateAcceptsFilledValues() throws Exception {

        byte[] filled;

        try (XWPFDocument doc = new XWPFDocument(
                new java.io.ByteArrayInputStream(
                        service.buildTemplateDocument()
                )
        );
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XWPFTable table = doc.getTables().get(0);

            table.getRow(0).getCell(1).setText("校园歌手大赛");
            table.getRow(1).getCell(1).setText("学习部");
            table.getRow(1).getCell(3).setText("2024-05-01");
            table.getRow(1).getCell(5).setText("大学生活动中心");
            table.getRow(2).getCell(1).setText("张一、张二");

            XWPFTableRow detail = table.getRow(4);

            String[] values = {
                    "张一", "25级1班", "20240001", "证书A", "2",
                    "张二", "25级1班", "20240002", "证书B", "1.5"
            };

            for (int c = 0; c < values.length; c++) {
                detail.getCell(c).setText(values[c]);
            }

            doc.write(out);

            filled = out.toByteArray();
        }

        ActivityBatchPreviewVO vo = service.preview(
                new MockMultipartFile(
                        "file",
                        "人工智能与计算机学院加分统计表.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        filled
                ),
                APPLICANT_ID
        );

        assertNull(vo.getMessage());

        assertEquals("校园歌手大赛", vo.getActivityName());
        assertEquals("学习部", vo.getDepartmentName());
        assertEquals("大学生活动中心", vo.getLocation());
        assertEquals("张一、张二", vo.getOrganizers());

        assertEquals(2, vo.getRows().size());
        assertEquals(2, vo.getValidCount());

        assertEquals("20240001", vo.getRows().get(0).getStudentNo());
        assertEquals("证书A", vo.getRows().get(0).getEvidence());
        assertEquals("20240002", vo.getRows().get(1).getStudentNo());
        assertEquals(
                "1.5",
                vo.getRows().get(1).getScore()
                        .stripTrailingZeros()
                        .toPlainString()
        );
    }

    @Test
    void previewParsesOriginalWordTemplate() {

        ActivityBatchPreviewVO vo =
                service.preview(
                        wordDocument(),
                        APPLICANT_ID
                );

        assertNull(vo.getMessage());

        assertEquals("校园歌手大赛", vo.getActivityName());
        assertEquals("学习部", vo.getDepartmentName());
        assertEquals("大学生活动中心", vo.getLocation());
        assertEquals("张一、张二", vo.getOrganizers());

        /* 中文日期（2024年5月1日）也应能识别 */
        assertEquals("2024年5月1日", vo.getActivityDate());

        assertEquals(2, vo.getRows().size());
        assertEquals(2, vo.getValidCount());
        assertEquals(0, vo.getInvalidCount());

        ActivityBatchRowVO left = vo.getRows().get(0);
        ActivityBatchRowVO right = vo.getRows().get(1);

        assertEquals("20240001", left.getStudentNo());
        assertEquals("证书A", left.getEvidence());
        assertEquals("2", left.getScore().stripTrailingZeros().toPlainString());

        assertEquals("20240002", right.getStudentNo());
        assertEquals("证书B", right.getEvidence());
        assertEquals("1.5", right.getScore().stripTrailingZeros().toPlainString());
    }

    @Test
    void wordTemplateReportsRowErrorsWithoutBlockingValidRows() {

        ActivityBatchPreviewVO vo =
                service.preview(
                        wordDocument(
                                "",
                                "25级1班",
                                "20249999",
                                "证书X",
                                "2"
                        ),
                        APPLICANT_ID
                );

        assertEquals(2, vo.getRows().size());
        assertEquals(1, vo.getValidCount());
        assertEquals(1, vo.getInvalidCount());
        assertEquals(
                "系统中不存在该学号：20249999",
                vo.getRows().get(0).getError()
        );
        assertNull(vo.getRows().get(1).getError());
    }

    /* =====================================================
     * 构造测试 Excel
     * ===================================================== */

    private String[] row(
            String realName,
            String className,
            String studentNo,
            String score,
            String evidence
    ) {

        return new String[]{realName, className, studentNo, score, evidence};
    }

    private MockMultipartFile workbook(String[]... rows) {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            org.apache.poi.ss.usermodel.Sheet sheet =
                    workbook.createSheet("活动批量加分申报");

            String[][] info = {
                    {"活动名称", "校园歌手大赛"},
                    {"举办部门", "学习部"},
                    {"举办时间", "2024-05-01"},
                    {"活动地点", "大学生活动中心"},
                    {"组织人员", "张一、张二"}
            };

            for (int i = 0; i < info.length; i++) {

                org.apache.poi.ss.usermodel.Row row = sheet.createRow(i);

                row.createCell(0).setCellValue(info[i][0]);
                row.createCell(1).setCellValue(info[i][1]);
            }

            org.apache.poi.ss.usermodel.Row header =
                    sheet.createRow(info.length + 1);

            String[] headers = {"姓名", "班级", "学号", "加分分值", "加分凭证"};

            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            for (int i = 0; i < rows.length; i++) {

                org.apache.poi.ss.usermodel.Row row =
                        sheet.createRow(info.length + 2 + i);

                for (int c = 0; c < rows[i].length; c++) {
                    row.createCell(c).setCellValue(rows[i][c]);
                }
            }

            workbook.write(out);

            return new MockMultipartFile(
                    "file",
                    "activity.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray()
            );

        } catch (Exception e) {

            throw new IllegalStateException(e);
        }
    }

    private MockMultipartFile twoBlockWorkbook(String dateText) {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            org.apache.poi.ss.usermodel.Sheet sheet =
                    workbook.createSheet("活动批量加分申报");

            sheet.createRow(0).createCell(0)
                    .setCellValue("人工智能与计算机学院加分统计表");

            org.apache.poi.ss.usermodel.Row name =
                    sheet.createRow(1);
            name.createCell(0).setCellValue("活动名称");
            name.createCell(1).setCellValue("校园歌手大赛");

            org.apache.poi.ss.usermodel.Row info =
                    sheet.createRow(2);
            info.createCell(0).setCellValue("举办部门");
            info.createCell(1).setCellValue("学习部");
            info.createCell(2).setCellValue("举办时间");
            info.createCell(3).setCellValue(dateText);
            info.createCell(4).setCellValue("活动地点");
            info.createCell(5).setCellValue("大学生活动中心");

            org.apache.poi.ss.usermodel.Row organizer =
                    sheet.createRow(3);
            organizer.createCell(0).setCellValue("组织人员");
            organizer.createCell(1).setCellValue("张一、张二");

            org.apache.poi.ss.usermodel.Row header =
                    sheet.createRow(4);

            String[] block = {"姓名", "班级", "学号", "加分凭证", "加分"};

            for (int b = 0; b < 2; b++) {
                for (int c = 0; c < block.length; c++) {
                    header.createCell(b * block.length + c)
                            .setCellValue(block[c]);
                }
            }

            org.apache.poi.ss.usermodel.Row data =
                    sheet.createRow(5);

            data.createCell(0).setCellValue("张一");
            data.createCell(1).setCellValue("25级1班");
            data.createCell(2).setCellValue("20240001");
            data.createCell(3).setCellValue("证书A");
            data.createCell(4).setCellValue("2");

            data.createCell(5).setCellValue("张二");
            data.createCell(6).setCellValue("25级1班");
            data.createCell(7).setCellValue("20240002");
            data.createCell(8).setCellValue("证书B");
            data.createCell(9).setCellValue("1.5");

            org.apache.poi.ss.usermodel.Row approval =
                    sheet.createRow(6);
            approval.createCell(0).setCellValue("活动组织者意见");
            approval.createCell(2).setCellValue("档案部主要负责人意见");
            approval.createCell(4).setCellValue("指导老师意见");
            approval.createCell(6).setCellValue("团委意见");
            approval.createCell(8).setCellValue("团委公章");

            workbook.write(out);

            return new MockMultipartFile(
                    "file",
                    "activity-word.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray()
            );

        } catch (Exception e) {

            throw new IllegalStateException(e);
        }
    }

    private MockMultipartFile wordDocument(String... leftCells) {

        String[] left = leftCells.length == 5
                ? leftCells
                : new String[]{"张一", "25级1班", "20240001", "证书A", "2"};

        String[] right = {"张二", "25级1班", "20240002", "证书B", "1.5"};

        String[] data = new String[10];

        System.arraycopy(left, 0, data, 0, 5);
        System.arraycopy(right, 0, data, 5, 5);

        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            /* 原表：4 行基本信息/表头 + 1 行明细 + 1 行空行 + 1 行审批栏 */
            XWPFTable table = doc.createTable(7, 10);

            setCells(table.getRow(0), new String[]{"活动名称", "校园歌手大赛"});

            setCells(table.getRow(1), new String[]{
                    "举办部门", "学习部",
                    "举办时间", "2024年5月1日",
                    "活动地点", "大学生活动中心"
            });

            setCells(table.getRow(2), new String[]{"组织人员", "张一、张二"});

            setCells(table.getRow(3), new String[]{
                    "姓名", "班级", "学号", "加分凭证", "加分",
                    "姓名", "班级", "学号", "加分凭证", "加分"
            });

            setCells(table.getRow(4), data);

            setCells(table.getRow(6), new String[]{
                    "活动组织者意见", "",
                    "档案部主要负责人意见", "",
                    "指导老师意见", "",
                    "团委意见", "团委公章"
            });

            doc.write(out);

            return new MockMultipartFile(
                    "file",
                    "人工智能与计算机学院加分统计表.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    out.toByteArray()
            );

        } catch (Exception e) {

            throw new IllegalStateException(e);
        }
    }

    private void setCells(XWPFTableRow row, String[] values) {

        for (int c = 0; c < values.length; c++) {
            row.getCell(c).setText(values[c]);
        }
    }

    private Department department() {

        Department department = new Department();

        department.setId(DEPARTMENT_ID);
        department.setName("学习部");
        department.setDepartmentType(OrgPositionLevel.TYPE_DEPARTMENT);
        department.setStatus((short) 1);

        return department;
    }

    private SysUser student(Long id, String studentNo, String realName) {

        SysUser user = new SysUser();

        user.setId(id);
        user.setStudentNo(studentNo);
        user.setRealName(realName);
        user.setClassName("2025级1班");
        user.setStatus((short) 1);

        return user;
    }

    private SysUserDepartment relation(String level) {

        SysUserDepartment relation = new SysUserDepartment();

        relation.setUserId(APPLICANT_ID);
        relation.setDepartmentId(DEPARTMENT_ID);
        relation.setPositionLevel(level);
        relation.setStatus((short) 1);

        return relation;
    }

    /**
     * 从查询条件中取出字符串参数（模拟按学号查询）
     */
    @SuppressWarnings("unchecked")
    private String firstNameOf(Object wrapper) {

        if (!(wrapper instanceof AbstractWrapper)) {
            return null;
        }

        AbstractWrapper<?, ?, ?> condition =
                (AbstractWrapper<?, ?, ?>) wrapper;

        try {
            condition.getSqlSegment();
        } catch (Exception ignored) {
            // 忽略
        }

        for (Object value : condition.getParamNameValuePairs().values()) {

            if (value instanceof String) {
                return (String) value;
            }
        }

        return null;
    }
}
