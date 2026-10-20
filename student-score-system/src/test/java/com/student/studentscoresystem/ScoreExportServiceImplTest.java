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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 加减分汇总导出测试。
 *
 * 重点验证：导出表最后一列「加减分具体情况」的单元格
 * 必须开启 Excel 自动换行（wrapText），
 * 否则内容较长时不会自动折行。
 */
@ExtendWith(MockitoExtension.class)
class ScoreExportServiceImplTest {

    private static final long SEMESTER_ID = 1L;

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

                        /* 测试无需处理
                         */
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

        stubExportData();

        service.export(
                SEMESTER_ID,
                "25级1班",
                response
        );

        try (Workbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(
                        output.toByteArray()
                )
        )) {

            Sheet sheet =
                    workbook.getSheetAt(0);

            Row header =
                    sheet.getRow(0);

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

            Row dataRow =
                    sheet.getRow(1);

            assertEquals(
                    "志愿服务+2",
                    dataRow.getCell(4)
                            .getStringCellValue()
            );

            assertTrue(
                    dataRow.getCell(4)
                            .getCellStyle()
                            .getWrapText(),
                    "加减分具体情况单元格必须开启自动换行"
            );
        }
    }

    private void stubExportData() {

        SysSemester semester =
                new SysSemester();

        semester.setId(SEMESTER_ID);
        semester.setName("2025-2026学年第一学期");

        lenient()
                .when(sysSemesterMapper.selectById(SEMESTER_ID))
                .thenReturn(semester);

        SysUser student =
                new SysUser();

        student.setId(100L);
        student.setRealName("张三");
        student.setStudentNo("2025001");

        lenient()
                .when(sysUserMapper.selectList(any()))
                .thenReturn(List.of(student));

        ScoreRecord record =
                new ScoreRecord();

        record.setId(1L);
        record.setStudentId(100L);
        record.setScore(new BigDecimal("2"));
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
