package com.student.studentscoresystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.SysSemester;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.SysSemesterMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.IDepartmentScoreApplyService;
import com.student.studentscoresystem.service.IScoreExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScoreExportServiceImpl
        implements IScoreExportService {

    private final SysUserMapper sysUserMapper;

    private final ScoreRecordMapper scoreRecordMapper;

    private final SysSemesterMapper sysSemesterMapper;

    private final IDepartmentScoreApplyService
            departmentScoreApplyService;


    public ScoreExportServiceImpl(
            SysUserMapper sysUserMapper,
            ScoreRecordMapper scoreRecordMapper,
            SysSemesterMapper sysSemesterMapper,
            IDepartmentScoreApplyService
                    departmentScoreApplyService) {

        this.sysUserMapper =
                sysUserMapper;

        this.scoreRecordMapper =
                scoreRecordMapper;

        this.sysSemesterMapper =
                sysSemesterMapper;

        this.departmentScoreApplyService =
                departmentScoreApplyService;
    }


    /* =========================================================
       导出
       ========================================================= */

    @Override
    public void export(
            Long semesterId,
            String className,
            HttpServletResponse response) {

        if (semesterId == null) {

            throw new IllegalArgumentException(
                    "请选择学期"
            );
        }


        if (className == null
                || className.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "请输入班级"
            );
        }


        className =
                className.trim();


        /* =====================================================
           查询学期
           ===================================================== */

        SysSemester semester =
                sysSemesterMapper.selectById(
                        semesterId
                );


        if (semester == null) {

            throw new IllegalArgumentException(
                    "学期不存在"
            );
        }


        /* =====================================================
           查询指定班级学生
           ===================================================== */

        List<SysUser> students =
                sysUserMapper.selectList(

                        new LambdaQueryWrapper<SysUser>()

                                .eq(
                                        SysUser::getClassName,
                                        className
                                )

                                .eq(
                                        SysUser::getStatus,
                                        (short) 1
                                )

                                .orderByAsc(
                                        SysUser::getStudentNo
                                )
                );


        /* =====================================================
           创建 Excel
           ===================================================== */

        try (
                Workbook workbook =
                        new XSSFWorkbook()
        ) {

            Sheet sheet =
                    workbook.createSheet(
                            "加减分汇总"
                    );


            /* =================================================
               样式
               ================================================= */

            CellStyle headerStyle =
                    createHeaderStyle(
                            workbook
                    );


            CellStyle normalStyle =
                    createNormalStyle(
                            workbook
                    );


            CellStyle scoreStyle =
                    createScoreStyle(
                            workbook
                    );


            /* =================================================
               表头
               ================================================= */

            Row header =
                    sheet.createRow(0);


            String[] headers = {

                    "姓名",

                    "学号",

                    "加分",

                    "减分",

                    "加减分具体情况"

            };


            for (
                    int i = 0;
                    i < headers.length;
                    i++
            ) {

                Cell cell =
                        header.createCell(i);

                cell.setCellValue(
                        headers[i]
                );

                cell.setCellStyle(
                        headerStyle
                );
            }


            /* =================================================
               获取所有学生ID
               ================================================= */

            List<Long> studentIds =
                    students.stream()

                            .map(
                                    SysUser::getId
                            )

                            .filter(
                                    Objects::nonNull
                            )

                            .collect(
                                    Collectors.toList()
                            );


            /*
             * key：
             * studentId
             *
             * value：
             * 该学生所有部门加减分记录
             */

            Map<Long, List<ScoreRecord>>
                    recordMap =
                    new HashMap<>();


            /*
             * 所有正式成绩记录
             */

            if (!studentIds.isEmpty()) {

                List<ScoreRecord> records =
                        scoreRecordMapper.selectList(

                                new LambdaQueryWrapper<ScoreRecord>()

                                        .in(
                                                ScoreRecord::getStudentId,
                                                studentIds
                                        )

                                        .eq(
                                                ScoreRecord::getSemesterId,
                                                semesterId
                                        )

                                        .eq(
                                                ScoreRecord::getStatus,
                                                (short) 1
                                        )

                                        .eq(
                                                ScoreRecord::getAdminHidden,
                                                (short) 0
                                        )

                                        /*
                                         * 只统计部门申报产生的成绩
                                         */
                                        .eq(
                                                ScoreRecord::getSourceType,
                                                "DEPARTMENT"
                                        )

                                        .orderByAsc(
                                                ScoreRecord::getStudentId
                                        )

                                        .orderByAsc(
                                                ScoreRecord::getCreateTime
                                        )
                        );


                recordMap =
                        records.stream()

                                .collect(
                                        Collectors.groupingBy(
                                                ScoreRecord::getStudentId
                                        )
                                );
            }


            /* =================================================
               获取所有部门申报ID
               ================================================= */

            Set<Long> applyIds =
                    recordMap.values()
                            .stream()
                            .flatMap(
                                    Collection::stream
                            )
                            .map(
                                    ScoreRecord::getSourceId
                            )
                            .filter(
                                    Objects::nonNull
                            )
                            .collect(
                                    Collectors.toSet()
                            );


            /*
             * 根据 sourceId 查询原始部门申报
             *
             * sourceId =
             * department_score_apply.id
             */

            Map<Long, DepartmentScoreApply>
                    applyMap =
                    new HashMap<>();


            if (!applyIds.isEmpty()) {

                List<DepartmentScoreApply>
                        applies =
                        departmentScoreApplyService.list(

                                new LambdaQueryWrapper<DepartmentScoreApply>()

                                        .in(
                                                DepartmentScoreApply::getId,
                                                applyIds
                                        )
                        );


                applyMap =
                        applies.stream()

                                .collect(
                                        Collectors.toMap(
                                                DepartmentScoreApply::getId,
                                                item -> item,
                                                (a, b) -> a
                                        )
                                );
            }


            /* =================================================
               开始写入学生数据
               ================================================= */

            int rowIndex = 1;


            for (SysUser student :
                    students) {

                Row row =
                        sheet.createRow(
                                rowIndex++
                        );


                /*
                 * 学生姓名
                 */

                Cell nameCell =
                        row.createCell(0);

                nameCell.setCellValue(
                        safeString(
                                student.getRealName()
                        )
                );

                nameCell.setCellStyle(
                        normalStyle
                );


                /*
                 * 学号
                 */

                Cell studentNoCell =
                        row.createCell(1);

                studentNoCell.setCellValue(
                        safeString(
                                student.getStudentNo()
                        )
                );

                studentNoCell.setCellStyle(
                        normalStyle
                );


                /*
                 * 获取学生成绩
                 */

                List<ScoreRecord> records =
                        recordMap.getOrDefault(
                                student.getId(),
                                Collections.emptyList()
                        );


                BigDecimal addScore =
                        BigDecimal.ZERO;


                BigDecimal deductScore =
                        BigDecimal.ZERO;


                List<String> details =
                        new ArrayList<>();


                /* =================================================
                   汇总该学生所有加减分
                   ================================================= */

                for (
                        ScoreRecord record :
                        records
                ) {

                    if (record == null) {
                        continue;
                    }


                    BigDecimal score =
                            record.getScore();


                    if (score == null) {
                        continue;
                    }


                    /*
                     * 加分
                     */

                    if (
                            score.compareTo(
                                    BigDecimal.ZERO
                            ) > 0
                    ) {

                        addScore =
                                addScore.add(
                                        score
                                );
                    }


                    /*
                     * 减分
                     */

                    else if (
                            score.compareTo(
                                    BigDecimal.ZERO
                            ) < 0
                    ) {

                        deductScore =
                                deductScore.add(
                                        score.abs()
                                );
                    }


                    /* =================================================
                       具体情况
                       ================================================= */

                    DepartmentScoreApply apply =
                            applyMap.get(
                                    record.getSourceId()
                            );


                    if (apply != null) {

                        String title =
                                safeString(
                                        apply.getTitle()
                                );


                        if (
                                !title.isEmpty()
                        ) {

                            /*
                             * 加分显示：
                             *
                             * 优秀志愿服务(+2)
                             *
                             * 减分显示：
                             *
                             * 无故缺席(-1)
                             */

                            String detail;


                            if (
                                    score.compareTo(
                                            BigDecimal.ZERO
                                    ) > 0
                            ) {

                                detail =
                                        title
                                                + "(+"
                                                + formatScore(
                                                score
                                        )
                                                + ")";

                            }

                            else {

                                detail =
                                        title
                                                + "(-"
                                                + formatScore(
                                                score.abs()
                                        )
                                                + ")";
                            }


                            details.add(
                                    detail
                            );
                        }
                    }
                }


                /*
                 * 加分
                 */

                Cell addCell =
                        row.createCell(2);


                addCell.setCellValue(
                        formatScore(
                                addScore
                        )
                );


                addCell.setCellStyle(
                        scoreStyle
                );


                /*
                 * 减分
                 */

                Cell deductCell =
                        row.createCell(3);


                deductCell.setCellValue(
                        formatScore(
                                deductScore
                        )
                );


                deductCell.setCellStyle(
                        scoreStyle
                );


                /*
                 * 加减分具体情况
                 *
                 * ★★★ 这里就是你要求的格式
                 *
                 * 每条之间用：
                 *
                 * ；
                 *
                 * 不换行。
                 */

                Cell detailCell =
                        row.createCell(4);


                detailCell.setCellValue(
                        String.join(
                                "；",
                                details
                        )
                );


                detailCell.setCellStyle(
                        normalStyle
                );
            }


            /* =================================================
               设置列宽
               ================================================= */

            /*
             * 姓名
             */

            sheet.setColumnWidth(
                    0,
                    15 * 256
            );


            /*
             * 学号
             */

            sheet.setColumnWidth(
                    1,
                    20 * 256
            );


            /*
             * 加分
             */

            sheet.setColumnWidth(
                    2,
                    12 * 256
            );


            /*
             * 减分
             */

            sheet.setColumnWidth(
                    3,
                    12 * 256
            );


            /*
             * 加减分具体情况
             *
             * 不自动换行
             */

            sheet.setColumnWidth(
                    4,
                    80 * 256
            );


            /*
             * 冻结第一行
             */

            sheet.createFreezePane(
                    0,
                    1
            );


            /* =================================================
               自动筛选
               ================================================= */

            if (rowIndex > 1) {

                sheet.setAutoFilter(
                        new org.apache.poi.ss.util.CellRangeAddress(
                                0,
                                rowIndex - 1,
                                0,
                                4
                        )
                );
            }


            /* =================================================
               设置文件名
               ================================================= */

            String fileName =
                    className
                            + "-"
                            + semester.getName()
                            + "-加减分汇总.xlsx";


            String encodedFileName =
                    URLEncoder.encode(
                            fileName,
                            StandardCharsets.UTF_8
                    ).replace(
                            "+",
                            "%20"
                    );


            response.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );


            response.setCharacterEncoding(
                    StandardCharsets.UTF_8.name()
            );


            response.setHeader(
                    "Content-Disposition",
                    "attachment;filename*=UTF-8''"
                            + encodedFileName
            );


            /* =================================================
               输出
               ================================================= */

            workbook.write(
                    response.getOutputStream()
            );


            response.flushBuffer();


        } catch (IOException e) {

            throw new RuntimeException(
                    "Excel 导出失败",
                    e
            );
        }
    }


    /* =========================================================
       表头样式
       ========================================================= */

    private CellStyle createHeaderStyle(
            Workbook workbook) {

        CellStyle style =
                workbook.createCellStyle();


        Font font =
                workbook.createFont();


        font.setBold(true);


        font.setFontHeightInPoints(
                (short) 11
        );


        style.setFont(font);


        style.setAlignment(
                HorizontalAlignment.CENTER
        );


        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );


        style.setBorderTop(
                BorderStyle.THIN
        );


        style.setBorderBottom(
                BorderStyle.THIN
        );


        style.setBorderLeft(
                BorderStyle.THIN
        );


        style.setBorderRight(
                BorderStyle.THIN
        );


        return style;
    }


    /* =========================================================
       普通样式
       ========================================================= */

    private CellStyle createNormalStyle(
            Workbook workbook) {

        CellStyle style =
                workbook.createCellStyle();


        style.setAlignment(
                HorizontalAlignment.LEFT
        );


        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );


        /*
         * ★★★ 最重要
         *
         * 禁止自动换行
         */

        style.setWrapText(false);


        style.setBorderTop(
                BorderStyle.THIN
        );


        style.setBorderBottom(
                BorderStyle.THIN
        );


        style.setBorderLeft(
                BorderStyle.THIN
        );


        style.setBorderRight(
                BorderStyle.THIN
        );


        return style;
    }


    /* =========================================================
       分数样式
       ========================================================= */

    private CellStyle createScoreStyle(
            Workbook workbook) {

        CellStyle style =
                createNormalStyle(
                        workbook
                );


        style.setAlignment(
                HorizontalAlignment.CENTER
        );


        return style;
    }


    /* =========================================================
       安全字符串
       ========================================================= */

    private String safeString(
            String value) {

        if (value == null) {

            return "";
        }

        return value.trim();
    }


    /* =========================================================
       分数格式
       ========================================================= */

    private String formatScore(
            BigDecimal score) {

        if (score == null) {

            return "0";
        }


        /*
         * 去掉多余的 0
         *
         * 2.00 → 2
         * 2.50 → 2.5
         */

        return score
                .stripTrailingZeros()
                .toPlainString();
    }
}