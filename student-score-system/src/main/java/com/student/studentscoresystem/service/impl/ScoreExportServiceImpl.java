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
       Excel 导出
       ========================================================= */

    @Override
    public void export(
            Long semesterId,
            String className,
            HttpServletResponse response) {

        /*
         * =====================================================
         * 1. 参数检查
         * =====================================================
         */

        if (semesterId == null) {

            throw new IllegalArgumentException(
                    "请选择学期"
            );
        }


        if (
                className == null
                        || className.trim().isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "请输入班级"
            );
        }


        /*
         * 去掉用户输入的前后空格
         */

        className =
                className.trim();


        /*
         * =====================================================
         * 2. 查询学期
         * =====================================================
         */

        SysSemester semester =
                sysSemesterMapper.selectById(
                        semesterId
                );


        if (semester == null) {

            throw new IllegalArgumentException(
                    "学期不存在"
            );
        }


        /*
         * =====================================================
         * 3. 查询指定班级的所有学生
         *
         * ★★★ 这里是这次最重要的修改
         *
         * 以前：
         *
         *     class_name = '25级1班'
         *
         * 现在：
         *
         *     TRIM(class_name) LIKE '%25级1班%'
         *
         * 这样可以处理：
         *
         * 25级1班
         * 25级1班
         * 25级1班软件工程
         *
         * 等情况。
         *
         * =====================================================
         */

        List<SysUser> students =
                sysUserMapper.selectList(

                        new LambdaQueryWrapper<SysUser>()

                                /*
                                 * 去掉数据库 class_name
                                 * 两边可能存在的空格，
                                 * 然后进行模糊匹配。
                                 *
                                 * PostgreSQL：
                                 *
                                 * TRIM(class_name)
                                 * LIKE '%xxx%'
                                 */
                                .apply(
                                        "TRIM(class_name) LIKE CONCAT('%', {0}, '%')",
                                        className
                                )

                                /*
                                 * 按学号排序
                                 */
                                .orderByAsc(
                                        SysUser::getStudentNo
                                )

                                /*
                                 * 再按照用户ID排序，
                                 * 保证排序稳定。
                                 */
                                .orderByAsc(
                                        SysUser::getId
                                )
                );


        /*
         * =====================================================
         * ★★★ 不在这里因为“没有成绩”过滤学生
         *
         * students 里面查到多少学生，
         * Excel 就必须生成多少学生。
         *
         * 即使这个学生完全没有 score_record，
         * 也必须出现：
         *
         * 加分 = 0
         * 减分 = 0
         * 具体情况 = 空
         *
         * =====================================================
         */


        /*
         * =====================================================
         * 4. 创建 Excel
         * =====================================================
         */

        try (
                Workbook workbook =
                        new XSSFWorkbook()
        ) {

            Sheet sheet =
                    workbook.createSheet(
                            "加减分汇总"
                    );


            /*
             * =================================================
             * 5. 创建样式
             * =================================================
             */

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


            /*
             * =================================================
             * 6. 创建表头
             * =================================================
             */

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


            /*
             * =================================================
             * 7. 获取所有学生ID
             * =================================================
             */

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
             * =================================================
             * 8. 创建成绩 Map
             *
             * key：
             *     studentId
             *
             * value：
             *     这个学生在当前学期的所有正式成绩
             *
             * =================================================
             */

            Map<Long, List<ScoreRecord>>
                    recordMap =
                    new HashMap<>();


            /*
             * =================================================
             * 9. 查询正式成绩
             *
             * ★★★ 不再限制：
             *
             *     sourceType = DEPARTMENT
             *
             * 所有正式、未隐藏的成绩都参与导出。
             *
             * 包括：
             *
             * DEPARTMENT
             * CERTIFICATE
             * 以及以后其他正式成绩来源。
             *
             * =================================================
             */

            if (!studentIds.isEmpty()) {

                List<ScoreRecord> records =
                        scoreRecordMapper.selectList(

                                new LambdaQueryWrapper<ScoreRecord>()

                                        /*
                                         * 当前班级学生
                                         */
                                        .in(
                                                ScoreRecord::getStudentId,
                                                studentIds
                                        )

                                        /*
                                         * 当前学期
                                         */
                                        .eq(
                                                ScoreRecord::getSemesterId,
                                                semesterId
                                        )

                                        /*
                                         * 正式有效成绩
                                         */
                                        .eq(
                                                ScoreRecord::getStatus,
                                                (short) 1
                                        )

                                        /*
                                         * 管理员没有隐藏
                                         */
                                        .eq(
                                                ScoreRecord::getAdminHidden,
                                                (short) 0
                                        )

                                        /*
                                         * 按学生排序
                                         */
                                        .orderByAsc(
                                                ScoreRecord::getStudentId
                                        )

                                        /*
                                         * 同一个学生按照创建时间排序
                                         */
                                        .orderByAsc(
                                                ScoreRecord::getCreateTime
                                        )
                        );


                /*
                 * =================================================
                 * 分组：
                 *
                 * studentId
                 *     ↓
                 * List<ScoreRecord>
                 * =================================================
                 */

                if (
                        records != null
                                && !records.isEmpty()
                ) {

                    recordMap =
                            records.stream()
                                    .filter(
                                            Objects::nonNull
                                    )
                                    .collect(
                                            Collectors.groupingBy(
                                                    ScoreRecord::getStudentId
                                            )
                                    );
                }
            }


            /*
             * =================================================
             * 10. 获取部门申报ID
             *
             * 只有 DEPARTMENT 类型的成绩需要查询
             * department_score_apply。
             * =================================================
             */

            Set<Long> applyIds =
                    recordMap.values()
                            .stream()
                            .flatMap(
                                    Collection::stream
                            )
                            .filter(
                                    Objects::nonNull
                            )
                            .filter(
                                    record ->
                                            "DEPARTMENT".equals(
                                                    record.getSourceType()
                                            )
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
             * =================================================
             * 11. 查询部门申报
             * =================================================
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


                if (
                        applies != null
                                && !applies.isEmpty()
                ) {

                    applyMap =
                            applies.stream()
                                    .filter(
                                            Objects::nonNull
                                    )
                                    .filter(
                                            item ->
                                                    item.getId() != null
                                    )
                                    .collect(
                                            Collectors.toMap(
                                                    DepartmentScoreApply::getId,
                                                    item -> item,
                                                    (a, b) -> a
                                            )
                                    );
                }
            }


            /*
             * =================================================
             * 12. 开始生成学生数据
             *
             * ★★★ students 是“所有学生”
             *
             * 所以：
             *
             * 没成绩的学生也会进入这里。
             *
             * =================================================
             */

            int rowIndex = 1;


            for (
                    SysUser student
                    : students
            ) {

                if (student == null) {

                    continue;
                }


                /*
                 * 创建 Excel 行
                 */

                Row row =
                        sheet.createRow(
                                rowIndex++
                        );


                /*
                 * =================================================
                 * 13. 姓名
                 * =================================================
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
                 * =================================================
                 * 14. 学号
                 * =================================================
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
                 * =================================================
                 * 15. 获取这个学生的成绩
                 *
                 * ★★★ 如果没有成绩：
                 *
                 * Collections.emptyList()
                 *
                 * 不会跳过学生。
                 * =================================================
                 */

                List<ScoreRecord> records =
                        recordMap.getOrDefault(
                                student.getId(),
                                Collections.emptyList()
                        );


                /*
                 * =================================================
                 * 16. 初始化加分、减分
                 *
                 * ★★★ 默认就是 0
                 * =================================================
                 */

                BigDecimal addScore =
                        BigDecimal.ZERO;


                BigDecimal deductScore =
                        BigDecimal.ZERO;


                /*
                 * =================================================
                 * 17. 明细
                 * =================================================
                 */

                List<String> details =
                        new ArrayList<>();


                /*
                 * =================================================
                 * 18. 汇总成绩
                 * =================================================
                 */

                for (
                        ScoreRecord record
                        : records
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
                     * =================================================
                     * 加分
                     * =================================================
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
                     * =================================================
                     * 减分
                     * =================================================
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


                    /*
                     * =================================================
                     * 19. 生成具体情况
                     * =================================================
                     */

                    String title = "";


                    /*
                     * -------------------------------------------------
                     * 部门加减分
                     * -------------------------------------------------
                     */

                    if (
                            "DEPARTMENT".equals(
                                    record.getSourceType()
                            )
                    ) {

                        DepartmentScoreApply apply =
                                applyMap.get(
                                        record.getSourceId()
                                );


                        if (apply != null) {

                            title =
                                    safeString(
                                            apply.getTitle()
                                    );
                        }
                    }


                    /*
                     * -------------------------------------------------
                     * 个人证书加分
                     * -------------------------------------------------
                     *
                     * 这里暂时显示：
                     *
                     * 个人证书加分+2
                     *
                     * 后面如果你需要，我还可以把证书名称、
                     * 获奖级别等具体内容也解析出来。
                     *
                     * -------------------------------------------------
                     */

                    else if (
                            "CERTIFICATE".equals(
                                    record.getSourceType()
                            )
                    ) {

                        title =
                                "个人证书加分";
                    }


                    /*
                     * -------------------------------------------------
                     * 其他类型
                     * -------------------------------------------------
                     */

                    else if (
                            record.getSourceType() != null
                    ) {

                        title =
                                safeString(
                                        record.getSourceType()
                                );
                    }


                    /*
                     * =================================================
                     * 20. 生成明细字符串
                     *
                     * ★★★ 不要括号
                     *
                     * 加分：
                     *
                     * 志愿服务+2
                     *
                     * 减分：
                     *
                     * 无故缺席-1
                     * =================================================
                     */

                    if (
                            !title.isEmpty()
                    ) {

                        String detail;


                        if (
                                score.compareTo(
                                        BigDecimal.ZERO
                                ) > 0
                        ) {

                            detail =
                                    title
                                            + "+"
                                            + formatScore(
                                            score
                                    );

                        } else {

                            detail =
                                    title
                                            + "-"
                                            + formatScore(
                                            score.abs()
                                    );
                        }


                        details.add(
                                detail
                        );
                    }
                }


                /*
                 * =================================================
                 * 21. 写入加分
                 * =================================================
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
                 * =================================================
                 * 22. 写入减分
                 * =================================================
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
                 * =================================================
                 * 23. 写入加减分具体情况
                 *
                 * ★★★ 每条之间使用中文分号：
                 *
                 * ；
                 *
                 * 没有成绩：
                 *
                 * 空字符串
                 *
                 * =================================================
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


            /*
             * =================================================
             * 24. 设置列宽
             * =================================================
             */

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
             */

            sheet.setColumnWidth(
                    4,
                    80 * 256
            );


            /*
             * =================================================
             * 25. 冻结第一行
             * =================================================
             */

            sheet.createFreezePane(
                    0,
                    1
            );


            /*
             * =================================================
             * 26. 自动筛选
             * =================================================
             */

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


            /*
             * =================================================
             * 27. 文件名
             * =================================================
             */

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


            /*
             * =================================================
             * 28. 设置响应头
             * =================================================
             */

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


            /*
             * =================================================
             * 29. 输出 Excel
             * =================================================
             */

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


        style.setFont(
                font
        );


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
         * 不自动换行
         */

        style.setWrapText(
                false
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