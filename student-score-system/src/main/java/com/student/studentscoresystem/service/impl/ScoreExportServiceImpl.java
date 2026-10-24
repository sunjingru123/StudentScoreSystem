package com.student.studentscoresystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.ScoreConstants;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.SysSemester;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.SysSemesterMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.IScoreExportService;
import com.student.studentscoresystem.service.ScoreProjectNameResolver;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScoreExportServiceImpl
        implements IScoreExportService {

    private final SysUserMapper sysUserMapper;

    private final ScoreRecordMapper scoreRecordMapper;

    private final SysSemesterMapper sysSemesterMapper;

    private final SysUserDepartmentMapper userDepartmentMapper;

    private final DepartmentMapper departmentMapper;

    private final ScoreProjectNameResolver
            scoreProjectNameResolver;


    public ScoreExportServiceImpl(
            SysUserMapper sysUserMapper,
            ScoreRecordMapper scoreRecordMapper,
            SysSemesterMapper sysSemesterMapper,
            SysUserDepartmentMapper userDepartmentMapper,
            DepartmentMapper departmentMapper,
            ScoreProjectNameResolver
                    scoreProjectNameResolver) {

        this.sysUserMapper =
                sysUserMapper;

        this.scoreRecordMapper =
                scoreRecordMapper;

        this.sysSemesterMapper =
                sysSemesterMapper;

        this.userDepartmentMapper =
                userDepartmentMapper;

        this.departmentMapper =
                departmentMapper;

        this.scoreProjectNameResolver =
                scoreProjectNameResolver;
    }


    /* =========================================================
       Excel 导出
       ========================================================= */

    @Override
    public void export(
            Long semesterId,
            String className,
            HttpServletResponse response) {

        if (
                className == null
                        || className.trim().isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "请输入班级"
            );
        }


        String trimmedClassName =
                className.trim();


        SysSemester semester =
                requireSemester(semesterId);


        List<SysUser> students =
                queryStudentsByClassNameLike(
                        trimmedClassName
                );


        writeWorkbook(
                semester,
                students,
                trimmedClassName,
                response
        );
    }


    /**
     * =========================================================
     * 按范围导出（全年级 / 各部门 / 各班）
     *
     * 与 {@link #export(Long, String, HttpServletResponse)}
     * 完全共用同一套 Excel 生成逻辑，
     * 保证导出的列结构、周次、40 分封顶、自动换行等格式一致。
     *
     * scope：
     *   GRADE      按年级关键字（如 24级）匹配 class_name
     *   DEPARTMENT 导出该部门全部在编成员
     *   CLASS      按班级关键字匹配 class_name
     * =========================================================
     */
    @Override
    public void exportByScope(
            Long semesterId,
            String scope,
            String grade,
            Long departmentId,
            String className,
            HttpServletResponse response) {

        SysSemester semester =
                requireSemester(semesterId);


        String normalizedScope =
                scope == null
                        ? ""
                        : scope.trim().toUpperCase();


        List<SysUser> students;

        String fileLabel;


        if ("GRADE".equals(normalizedScope)) {

            if (grade == null || grade.trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "请选择年级"
                );
            }


            fileLabel = grade.trim();

            students =
                    queryStudentsByClassNameLike(
                            fileLabel
                    );

        } else if ("DEPARTMENT".equals(normalizedScope)) {

            if (departmentId == null) {

                throw new IllegalArgumentException(
                        "请选择部门"
                );
            }


            Department department =
                    departmentMapper.selectById(
                            departmentId
                    );


            if (department == null) {

                throw new IllegalArgumentException(
                        "部门不存在"
                );
            }


            fileLabel = department.getName();

            students =
                    queryDepartmentMembers(
                            departmentId
                    );

        } else if ("CLASS".equals(normalizedScope)) {

            if (className == null || className.trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "请输入班级"
                );
            }


            fileLabel = className.trim();

            students =
                    queryStudentsByClassNameLike(
                            fileLabel
                    );

        } else {

            throw new IllegalArgumentException(
                    "不支持的导出范围"
            );
        }


        writeWorkbook(
                semester,
                students,
                fileLabel,
                response
        );
    }


    /**
     * 查询并校验学期。
     */
    private SysSemester requireSemester(
            Long semesterId) {

        if (semesterId == null) {

            throw new IllegalArgumentException(
                    "请选择学期"
            );
        }


        SysSemester semester =
                sysSemesterMapper.selectById(
                        semesterId
                );


        if (semester == null) {

            throw new IllegalArgumentException(
                    "学期不存在"
            );
        }


        return semester;
    }


    /**
     * 按班级关键字（TRIM 后 LIKE）查询学生，
     * 年级导出与班级导出共用。
     */
    private List<SysUser> queryStudentsByClassNameLike(
            String keyword) {

        List<SysUser> students =
                sysUserMapper.selectList(

                        new LambdaQueryWrapper<SysUser>()

                                /*
                                 * PostgreSQL：
                                 * TRIM(class_name) LIKE '%xxx%'
                                 */
                                .apply(
                                        "TRIM(class_name) LIKE CONCAT('%', {0}, '%')",
                                        keyword
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


        return students == null
                ? new ArrayList<>()
                : students;
    }


    /**
     * 查询某个部门当前在编（status = 1）的学生成员。
     */
    private List<SysUser> queryDepartmentMembers(
            Long departmentId) {

        List<SysUserDepartment> relations =
                userDepartmentMapper.selectList(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getDepartmentId,
                                        departmentId
                                )
                                .eq(
                                        SysUserDepartment::getStatus,
                                        (short) 1
                                )
                );


        if (relations == null || relations.isEmpty()) {

            return new ArrayList<>();
        }


        List<Long> userIds =
                relations.stream()
                        .filter(Objects::nonNull)
                        .map(SysUserDepartment::getUserId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();


        if (userIds.isEmpty()) {

            return new ArrayList<>();
        }


        List<SysUser> users =
                sysUserMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .in(
                                        SysUser::getId,
                                        userIds
                                )
                                .orderByAsc(
                                        SysUser::getStudentNo
                                )
                                .orderByAsc(
                                        SysUser::getId
                                )
                );


        return users == null
                ? new ArrayList<>()
                : users;
    }


    /* =========================================================
       Excel 导出（统一实现，所有导出范围共用）
       ========================================================= */

    private void writeWorkbook(
            SysSemester semester,
            List<SysUser> students,
            String fileLabel,
            HttpServletResponse response) {


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


            CellStyle detailStyle =
                    createDetailStyle(
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
                                                semester.getId()
                                        )

                                        /*
                                         * 正式有效成绩。
                                         *
                                         * status/admin_hidden 是后续补充到历史表的字段，
                                         * 早期生成的个人证书成绩记录可能为 NULL。
                                         * NULL 与默认值 1/0 一样按有效、未隐藏处理，
                                         * 否则证书加分会在导出时被整条过滤掉。
                                         */
                                        .and(wrapper -> wrapper
                                                .eq(
                                                        ScoreRecord::getStatus,
                                                        (short) 1
                                                )
                                                .or()
                                                .isNull(
                                                        ScoreRecord::getStatus
                                                )
                                        )

                                        /* 管理员没有隐藏（兼容历史 NULL） */
                                        .and(wrapper -> wrapper
                                                .eq(
                                                        ScoreRecord::getAdminHidden,
                                                        (short) 0
                                                )
                                                .or()
                                                .isNull(
                                                        ScoreRecord::getAdminHidden
                                                )
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
             * 10. 批量预取项目名称
             *
             * 一次查库取出本次导出用到的：
             *
             * 规则名称
             * 个人证书（获奖名称 + 获奖级别）
             * 部门申报标题
             * 管理员调整原因
             *
             * 避免逐条记录查询（N+1），
             * 解析规则与页面明细完全一致。
             * =================================================
             */

            ScoreProjectNameResolver.PreloadedNames
                    preloadedNames =
                    scoreProjectNameResolver.preload(
                            recordMap.values()
                                    .stream()
                                    .flatMap(
                                            Collection::stream
                                    )
                                    .toList()
                    );


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
                     *
                     * 名称来自第 10 步的批量预取，
                     * 与页面明细使用同一套解析规则：
                     *
                     * 规则名称 -> 个人证书获奖名称（含获奖级别）
                     * -> 部门申报标题 -> 管理员调整原因 -> 综合测评项目
                     * =================================================
                     */

                    String title =
                            preloadedNames.resolve(
                                    record,
                                    true
                            );


                    /*
                     * =================================================
                     * 20. 生成明细字符串
                     *
                     * ★★★ 分值不要加括号
                     *
                     * 加分：
                     *
                     * 志愿服务+2（第3周）
                     *
                     * 减分：
                     *
                     * 无故缺席-1（第5周）
                     *
                     * 第 X 周以当前学期的开始日期为第 1 周起点，
                     * 按成绩记录时间动态计算，
                     * 学期配置（开始 / 结束日期）更新后，
                     * 再次导出的第 X 周会自动跟着更新。
                     * =================================================
                     */

                    if (
                            !title.isEmpty()
                    ) {

                        String weekText =
                                formatWeek(
                                        semester,
                                        record
                                );

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
                                    )
                                            + weekText;

                        } else {

                            detail =
                                    title
                                            + "-"
                                            + formatScore(
                                            score.abs()
                                    )
                                            + weekText;
                        }


                        details.add(
                                detail
                        );
                    }
                }


                /*
                 * =================================================
                 * 21. 写入加分
                 *
                 * 汇总表的「加分」列不超过综合测评基础上限，
                 * 例如累计加分 59 分时只写 40 分。
                 * 具体情况列仍按每条原始分值展示。
                 * =================================================
                 */

                BigDecimal summaryAddScore =
                        addScore.min(
                                ScoreConstants.MAX_SCORE
                        );


                Cell addCell =
                        row.createCell(2);


                addCell.setCellValue(
                        formatScore(
                                summaryAddScore
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
                 * 单元格开启自动换行，
                 * 内容较长时会在列宽内自动折行，不会被截断
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
                        detailStyle
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
                    safeString(fileLabel)
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
       明细样式（自动换行）
       ========================================================= */

    private CellStyle createDetailStyle(
            Workbook workbook) {

        CellStyle style =
                createNormalStyle(
                        workbook
                );


        /*
         * 加减分具体情况通常较长，
         * 必须开启自动换行，否则内容会溢出列宽或被截断。
         */

        style.setWrapText(
                true
        );


        /*
         * 多行内容顶部对齐，阅读更自然
         */

        style.setVerticalAlignment(
                VerticalAlignment.TOP
        );


        return style;
    }


    /* =========================================================
       第 X 周文案

       学期未配置开始日期或成绩没有记录时间时，
       返回空字符串，不强行补周次。
       ========================================================= */

    private String formatWeek(
            SysSemester semester,
            ScoreRecord record) {

        Integer week =
                resolveWeek(
                        semester,
                        record
                );


        if (week == null) {

            return "";
        }


        return "（第" + week + "周）";
    }


    /* =========================================================
       计算成绩属于第几周

       规则：

       1. 以当前学期的开始日期为第 1 周第一天；
       2. 每满 7 天进入下一周；
       3. 早于学期开始的时间按第 1 周算；
       4. 学期配置了结束日期时，不超出学期总周数；
       5. 学期开始日期为空时无法推算，返回 null。

       周次完全由学期配置动态推算，不落库，
       因此学期开始 / 结束日期一改，
       下次导出的第 X 周就会跟着变。
       ========================================================= */

    private Integer resolveWeek(
            SysSemester semester,
            ScoreRecord record) {

        if (semester == null
                || semester.getStartDate() == null) {

            return null;
        }


        if (record == null
                || record.getCreateTime() == null) {

            return null;
        }


        LocalDate startDate =
                semester.getStartDate();


        LocalDate recordDate =
                record.getCreateTime()
                        .toLocalDate();


        int week =
                (int) (
                        ChronoUnit.DAYS.between(
                                startDate,
                                recordDate
                        ) / 7
                ) + 1;


        /*
         * 早于学期开始：按第 1 周处理，
         * 不出现 0 周 / 负周次。
         */

        if (week < 1) {

            week = 1;
        }


        /*
         * 不超出学期总周数
         */

        if (semester.getEndDate() != null) {

            long totalDays =
                    ChronoUnit.DAYS.between(
                            startDate,
                            semester.getEndDate()
                    );


            int totalWeeks =
                    (int) (totalDays / 7) + 1;


            if (totalWeeks > 0
                    && week > totalWeeks) {

                week = totalWeeks;
            }
        }


        return week;
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
