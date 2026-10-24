package com.student.studentscoresystem;

import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.SysSemester;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.SysSemesterMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.ScoreProjectNameResolver;
import com.student.studentscoresystem.service.impl.ScoreExportServiceImpl;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * 加减分汇总导出测试。
 *
 * 覆盖两件事：
 *
 * 1.「加减分具体情况」列开启 Excel 自动换行（wrapText）；
 * 2. 详情里的「第 X 周」按所选学期开始日期动态推算，
 *    学期日期一改，导出结果随之更新。
 */
@ExtendWith(MockitoExtension.class)
class ScoreExportServiceImplTest {

    private static final long SEMESTER_ID = 1L;

    private static final Long STUDENT_ID = 100L;

    /** 成绩记录时间：2025-09-15 */
    private static final LocalDateTime RECORD_TIME =
            LocalDateTime.of(2025, 9, 15, 10, 0);

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private ScoreRecordMapper scoreRecordMapper;

    @Mock
    private SysSemesterMapper sysSemesterMapper;

    @Mock
    private ScoreProjectNameResolver scoreProjectNameResolver;

    @Mock
    private HttpServletResponse response;

    private ScoreExportServiceImpl service;

    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() throws Exception {

        service = new ScoreExportServiceImpl(
                sysUserMapper,
                scoreRecordMapper,
                sysSemesterMapper,
                scoreProjectNameResolver
        );

        output = new ByteArrayOutputStream();

        ServletOutputStream servletOutputStream =
                new ServletOutputStream() {

                    @Override
                    public boolean isReady() {

                        return true;
                    }

                    @Override
                    public void setWriteListener(
                            WriteListener writeListener) {

                        /* 测试无需处理 */
                    }

                    @Override
                    public void write(int b) {

                        output.write(b);
                    }
                };

        lenient()
                .when(response.getOutputStream())
                .thenReturn(servletOutputStream);
    }

    @Test
    void detailColumnCellWrapsText() throws Exception {

        Sheet sheet = exportSheet(null, null);

        Row header = sheet.getRow(0);

        assertEquals(
                "加减分具体情况",
                header.getCell(4)
                        .getStringCellValue()
        );

        assertFalse(
                header.getCell(4)
                        .getCellStyle()
                        .getWrapText(),
                "表头不需要自动换行"
        );

        Row dataRow = sheet.getRow(1);

        assertTrue(
                dataRow.getCell(4)
                        .getCellStyle()
                        .getWrapText(),
                "加减分具体情况单元格必须开启自动换行"
        );
    }

    @Test
    void detailContainsWeekNumberComputedFromSemester()
            throws Exception {

        /*
         * 学期开始：2025-09-01
         * 成绩时间：2025-09-15
         *
         * 相差 14 天 → 第 3 周
         */

        Sheet sheet = exportSheet(
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2026, 1, 11)
        );

        assertEquals(
                "志愿服务+2（第3周）",
                sheet.getRow(1)
                        .getCell(4)
                        .getStringCellValue()
        );
    }

    @Test
    void weekNumberFollowsUpdatedSemesterStartDate()
            throws Exception {

        /*
         * 同一条成绩，学期开始日期改成 2025-09-15 后，
         * 周次应随学期配置变化：相差 0 天 → 第 1 周
         */

        Sheet sheet = exportSheet(
                LocalDate.of(2025, 9, 15),
                LocalDate.of(2026, 1, 11)
        );

        assertEquals(
                "志愿服务+2（第1周）",
                sheet.getRow(1)
                        .getCell(4)
                        .getStringCellValue()
        );
    }

    @Test
    void weekNumberDoesNotExceedSemesterLength()
            throws Exception {

        /*
         * 学期只有 1 周（2025-09-01 ~ 2025-09-07），
         * 成绩时间晚于学期结束 → 收敛到学期总周数
         */

        Sheet sheet = exportSheet(
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 9, 7)
        );

        assertEquals(
                "志愿服务+2（第1周）",
                sheet.getRow(1)
                        .getCell(4)
                        .getStringCellValue()
        );
    }

    /**
     * 累计加分超过综合测评上限（40）时，
     * 汇总表的「加分」列只写 40。
     */
    @Test
    void bonusColumnIsCappedAtMaxScore()
            throws Exception {

        stubExportData(
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2026, 1, 11)
        );

        /*
         * 覆盖成绩记录：30 + 29 = 59 分
         */
        lenient()
                .when(scoreRecordMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                record(
                                        1L,
                                        new BigDecimal("30")
                                ),
                                record(
                                        2L,
                                        new BigDecimal("29")
                                )
                        )
                );

        Sheet sheet = exportCurrent();

        assertEquals(
                "40",
                sheet.getRow(1)
                        .getCell(2)
                        .getStringCellValue(),
                "累计加分 59 分时只写 40"
        );
    }

    /**
     * 未超过上限时保持真实累计值，不能被误截断。
     */
    @Test
    void bonusColumnKeepsValueBelowMaxScore()
            throws Exception {

        stubExportData(
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2026, 1, 11)
        );

        lenient()
                .when(scoreRecordMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                record(
                                        1L,
                                        new BigDecimal("20.5")
                                ),
                                record(
                                        2L,
                                        new BigDecimal("18")
                                )
                        )
                );

        Sheet sheet = exportCurrent();

        assertEquals(
                "38.5",
                sheet.getRow(1)
                        .getCell(2)
                        .getStringCellValue()
        );
    }

    /**
     * 执行一次导出并返回解析后的工作表
     */
    private Sheet exportSheet(
            LocalDate semesterStart,
            LocalDate semesterEnd
    ) throws Exception {

        stubExportData(semesterStart, semesterEnd);

        return exportCurrent();
    }

    /**
     * 使用当前已准备好的 mock 数据执行一次导出，并解析工作表。
     */
    private Sheet exportCurrent() throws Exception {

        service.export(
                SEMESTER_ID,
                "25级1班",
                response
        );

        return readSheet();
    }

    /**
     * 解析已写入的 Excel 输出。
     */
    private Sheet readSheet() throws Exception {

        Workbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(
                        output.toByteArray()
                )
        );

        return workbook.getSheetAt(0);
    }

    /**
     * 高级辅导员按「全年级」（如 24级）导出时，
     * 列结构与班级导出一致。
     */
    @Test
    void exportByScopeGradeUsesSameFormat() throws Exception {

        stubExportData(
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2026, 1, 11)
        );

        service.exportByScope(
                SEMESTER_ID,
                "GRADE",
                "24级",
                null,
                response
        );

        Sheet sheet = readSheet();

        assertEquals(
                "姓名",
                sheet.getRow(0)
                        .getCell(0)
                        .getStringCellValue()
        );

        assertEquals(
                "加减分具体情况",
                sheet.getRow(0)
                        .getCell(4)
                        .getStringCellValue()
        );
    }

    private static ScoreRecord record(
            Long id,
            BigDecimal score
    ) {

        ScoreRecord record = new ScoreRecord();

        record.setId(id);
        record.setStudentId(STUDENT_ID);
        record.setScore(score);
        record.setCreateTime(RECORD_TIME);
        record.setSourceType(
                ScoreProjectNameResolver.TYPE_DEPARTMENT
        );

        return record;
    }

    private void stubExportData(
            LocalDate semesterStart,
            LocalDate semesterEnd
    ) {

        SysSemester semester = new SysSemester();

        semester.setId(SEMESTER_ID);
        semester.setName("2025-2026学年第一学期");
        semester.setStartDate(semesterStart);
        semester.setEndDate(semesterEnd);

        lenient()
                .when(sysSemesterMapper.selectById(SEMESTER_ID))
                .thenReturn(semester);

        SysUser student = new SysUser();

        student.setId(STUDENT_ID);
        student.setRealName("张三");
        student.setStudentNo("2025001");

        lenient()
                .when(sysUserMapper.selectList(any()))
                .thenReturn(List.of(student));

        ScoreRecord record = new ScoreRecord();

        record.setId(1L);
        record.setStudentId(STUDENT_ID);
        record.setScore(new BigDecimal("2"));
        record.setCreateTime(RECORD_TIME);
        record.setSourceType(
                ScoreProjectNameResolver.TYPE_DEPARTMENT
        );

        lenient()
                .when(scoreRecordMapper.selectList(any()))
                .thenReturn(List.of(record));

        ScoreProjectNameResolver.PreloadedNames preloadedNames =
                mock(
                        ScoreProjectNameResolver.PreloadedNames.class
                );

        lenient()
                .when(scoreProjectNameResolver.preload(anyList()))
                .thenReturn(preloadedNames);

        lenient()
                .when(preloadedNames.resolve(
                        any(ScoreRecord.class),
                        eq(true)
                ))
                .thenReturn("志愿服务");
    }
}
