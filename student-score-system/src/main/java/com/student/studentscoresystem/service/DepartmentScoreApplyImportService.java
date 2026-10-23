package com.student.studentscoresystem.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.dto.DepartmentScoreApplyExcelRow;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.entity.DepartmentScoreTemplate;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFName;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * =========================================================
 * 部门加减分申报 Excel 批量导入
 *
 * 业务约束（与单条申报 /departmentScoreApply/add 保持一致）：
 *
 * 1. 只有该部门在职的干事 / 副部长 / 部长可以导入；
 * 2. 项目必须来自本部门已启用的 department_score_template；
 * 3. 加分 / 减分类型和分值以项目模板为准，
 *    导入文件里的「分值」仅用于交叉校验，不会覆盖模板；
 * 4. 同一个学生的同一个项目允许出现多行（例如一周内多次旷课），
 *    只有「同一学生 + 同一部门 + 同一项目 + 同一天（日期非空）
 *    + 同一备注」完全一致时才判为重复；
 * 5. 导入后申报单状态为「待部门审核」。
 * =========================================================
 */
@Service
public class DepartmentScoreApplyImportService {

    /**
     * 「项目」列的下拉选项写在隐藏 sheet 里，
     * 避免 POI 显式列表 255 字符的限制
     */
    private static final String OPTION_SHEET_NAME = "项目选项";

    private static final String OPTION_RANGE_NAME = "PROJECT_OPTIONS";

    private static final String TEMPLATE_SHEET_NAME = "申报导入模板";

    private static final String GUIDE_SHEET_NAME = "填写说明";

    /**
     * 下拉生效的数据行范围（含表头共 1001 行）
     */
    private static final int TEMPLATE_DATA_ROWS = 1000;

    /**
     * 返回给前端的失败明细上限，避免大文件时响应过大
     */
    private static final int MAX_ERRORS = 50;

    /**
     * 「项目」列下标：学号0 姓名1 日期2 项目3 分值4 备注5
     */
    private static final int PROJECT_COLUMN_INDEX = 3;

    private static final String[] HEADERS =
            {"学号", "姓名", "日期", "项目", "分值", "备注"};

    private final DepartmentMapper departmentMapper;

    private final SysUserMapper sysUserMapper;

    private final SysUserDepartmentMapper userDepartmentMapper;

    private final IDepartmentScoreTemplateService templateService;

    private final IDepartmentScoreApplyService applyService;

    private final DepartmentScoreApplyDuplicateChecker duplicateChecker;

    public DepartmentScoreApplyImportService(
            DepartmentMapper departmentMapper,
            SysUserMapper sysUserMapper,
            SysUserDepartmentMapper userDepartmentMapper,
            IDepartmentScoreTemplateService templateService,
            IDepartmentScoreApplyService applyService,
            DepartmentScoreApplyDuplicateChecker duplicateChecker) {

        this.departmentMapper = departmentMapper;
        this.sysUserMapper = sysUserMapper;
        this.userDepartmentMapper = userDepartmentMapper;
        this.templateService = templateService;
        this.applyService = applyService;
        this.duplicateChecker = duplicateChecker;
    }

    /**
     * =========================================================
     * 生成导入模板
     *
     * 表头固定 6 列，「项目」列通过数据有效性下拉
     * 引用隐藏 sheet 中的本部门可用项目。
     * =========================================================
     */
    public byte[] buildTemplateWorkbook(
            Long departmentId,
            Long currentUserId) {

        requireEnabledDepartment(departmentId);

        requireApplyPermission(departmentId, currentUserId);

        List<DepartmentScoreTemplate> templates =
                loadAvailableTemplates(departmentId);

        if (templates.isEmpty()) {

            throw new IllegalArgumentException(
                    "该部门暂无可用加减分项目，请先在管理员端配置评分项目，"
                            + "或在「管理本部门活动」中新增活动后再下载模板"
            );
        }

        List<String> labels = new ArrayList<>();

        for (DepartmentScoreTemplate template : templates) {

            String label = buildTemplateLabel(template);

            if (!labels.contains(label)) {

                labels.add(label);
            }
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet =
                    workbook.createSheet(TEMPLATE_SHEET_NAME);

            writeHeaderRow(workbook, sheet);

            writeOptionSheet(workbook, labels);

            applyProjectValidation(workbook, sheet, labels.size());

            writeGuideSheet(workbook);

            workbook.write(out);

            return out.toByteArray();

        } catch (IOException e) {

            throw new IllegalStateException(
                    "生成部门申报导入模板失败：" + e.getMessage(),
                    e
            );
        }
    }

    /**
     * =========================================================
     * 导入部门加减分申报
     *
     * 逐行校验：单行失败只记录原因，不影响其他行；
     * 非业务异常（例如数据库异常）直接抛出并整体回滚。
     * =========================================================
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importApplies(
            Long departmentId,
            Long currentUserId,
            MultipartFile file) {

        requireEnabledDepartment(departmentId);

        requireApplyPermission(departmentId, currentUserId);

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException("请选择需要导入的 Excel 文件");
        }

        String filename = file.getOriginalFilename();

        if (filename == null
                || (!filename.toLowerCase().endsWith(".xlsx")
                && !filename.toLowerCase().endsWith(".xls"))) {

            throw new IllegalArgumentException(
                    "只支持 .xlsx 或 .xls 格式的 Excel 文件"
            );
        }

        List<DepartmentScoreTemplate> templates =
                loadAvailableTemplates(departmentId);

        if (templates.isEmpty()) {

            throw new IllegalArgumentException(
                    "该部门暂无可用加减分项目，无法导入申报"
            );
        }

        List<DepartmentScoreApplyExcelRow> rows;

        byte[] content;

        try {

            content = file.getBytes();

        } catch (IOException e) {

            throw new IllegalArgumentException(
                    "读取上传文件失败：" + e.getMessage()
            );
        }

        validateTemplateHeader(content);

        /*
         * 合并单元格补值：
         * 同一个学生多次旷课时，使用者常常会把「学号」「姓名」
         * 单元格纵向合并后填写多行，而合并区域只有左上角有值。
         * 先补全合并区域的值，再交给 EasyExcel 解析，
         * 否则从第二行开始会报「学号不能为空」。
         */
        content = expandMergedCells(content);

        try (InputStream in = new ByteArrayInputStream(content)) {

            /*
             * 只读取第一个 sheet：
             * 模板里还有「项目选项」「填写说明」两个辅助 sheet。
             */
            rows = EasyExcel
                    .read(in)
                    .sheet(0)
                    .head(DepartmentScoreApplyExcelRow.class)
                    .doReadSync();

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Excel 读取失败：" + e.getMessage()
            );
        }

        int successCount = 0;
        int failCount = 0;

        List<Map<String, Object>> errors =
                new ArrayList<>();

        if (rows == null || rows.isEmpty()) {

            return buildResult(
                    0,
                    0,
                    errors,
                    "Excel 中没有可导入的数据，请先填写申报内容"
            );
        }

        Map<String, List<DepartmentScoreTemplate>> templatesByLabel =
                new HashMap<>();

        Map<String, List<DepartmentScoreTemplate>> templatesByName =
                new HashMap<>();

        indexTemplates(
                templates,
                templatesByLabel,
                templatesByName
        );

        /*
         * 学号 -> 学生 缓存，避免每行重复查询；
         * notFound 记录已经查过但不存在/状态异常的学号。
         */
        Map<String, SysUser> studentCache =
                new HashMap<>();

        Set<String> studentNotUsable =
                new HashSet<>();

        /*
         * 本文件里已经出现过的「学生 + 部门 + 项目 + 日期 + 备注」，
         * key -> 第一次出现的行号，用于提示重复的是哪一行。
         */
        Map<String, Integer> seenRows =
                new HashMap<>();

        int rowNumber = 1;

        for (DepartmentScoreApplyExcelRow row : rows) {

            rowNumber++;

            if (row == null) {

                failCount++;

                addError(errors, rowNumber, "Excel 当前行数据为空");

                continue;
            }

            try {

                DepartmentScoreApply apply =
                        buildApply(
                                row,
                                departmentId,
                                currentUserId,
                                templatesByLabel,
                                templatesByName,
                                studentCache,
                                studentNotUsable,
                                seenRows,
                                rowNumber
                        );

                /*
                 * 空行直接跳过，不计入成功/失败
                 */
                if (apply == null) {

                    continue;
                }

                boolean saved =
                        applyService.save(apply);

                if (!saved || apply.getId() == null) {

                    throw new IllegalStateException(
                            "申报保存失败，未生成申报记录"
                    );
                }

                successCount++;

            } catch (IllegalArgumentException e) {

                failCount++;

                addError(
                        errors,
                        rowNumber,
                        e.getMessage()
                );
            }
        }

        if (failCount > errors.size()) {

            errors.add(
                    error(
                            -1,
                            "失败原因过多，仅显示前 "
                                    + MAX_ERRORS
                                    + " 条，共 "
                                    + failCount
                                    + " 条失败"
                    )
            );
        }

        return buildResult(
                successCount,
                failCount,
                errors,
                failCount == 0
                        ? "部门加减分申报导入完成"
                        : "部门加减分申报导入完成，部分行未导入"
        );
    }

    /**
     * =========================================================
     * 校验表头
     *
     * EasyExcel 按列下标映射字段，
     * 如果使用者删列/调换列顺序，数据会错位，
     * 这里先按表头拦住，避免把错位数据写进库。
     * =========================================================
     */
    private void validateTemplateHeader(
            byte[] content) {

        try (Workbook workbook =
                     WorkbookFactory.create(
                             new ByteArrayInputStream(content)
                     )) {

            if (workbook.getNumberOfSheets() <= 0) {

                throw new IllegalArgumentException(
                        "导入文件没有工作表，请使用系统下载的模板"
                );
            }

            Sheet sheet = workbook.getSheetAt(0);

            Row header = sheet.getRow(0);

            if (header == null) {

                throw new IllegalArgumentException(
                        "导入文件缺少表头，请使用系统下载的模板"
                );
            }

            DataFormatter formatter =
                    new DataFormatter();

            for (int i = 0; i < HEADERS.length; i++) {

                Cell cell = header.getCell(i);

                String actual =
                        cell == null
                                ? ""
                                : formatter
                                .formatCellValue(cell)
                                .trim();

                if (!HEADERS[i].equals(actual)) {

                    throw new IllegalArgumentException(
                            "模板表头不正确：第 "
                                    + (i + 1)
                                    + " 列应为「"
                                    + HEADERS[i]
                                    + "」，实际为「"
                                    + actual
                                    + "」，请重新下载导入模板"
                    );
                }
            }

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Excel 读取失败：" + e.getMessage()
            );
        }
    }


    /**
     * =========================================================
     * 展开合并单元格
     *
     * Excel 的合并区域只有左上角单元格保存值，
     * 其余单元格读出来都是空的（POI / EasyExcel 行为一致）。
     *
     * 部门干部在填写申报表时，经常把同一个学生的「学号」
     * 「姓名」单元格纵向合并后填写多行（例如多次旷课），
     * 不补值就会从第二行开始报「学号不能为空」。
     *
     * 这里严格按「合并区域」补值，而不是整列向下填充：
     *
     * 1. 只有位于合并区域内的空单元格才会被补齐，
     *    使用者自己漏填的普通空单元格依然会照常报错，
     *    不会被静默地算到上一位同学身上；
     * 2. 只把左上角的值往下补到同一列，
     *    所以纵向合并「学号」「姓名」都能正确识别；
     *    即使使用者把学号 + 姓名合并成一个两列的大单元格，
     *    也只补学号、不动姓名（姓名本身是可选的）。
     * =========================================================
     */
    private byte[] expandMergedCells(
            byte[] content) {

        try (Workbook workbook =
                     WorkbookFactory.create(
                             new ByteArrayInputStream(content)
                     )) {

            if (workbook.getNumberOfSheets() <= 0) {

                return content;
            }

            Sheet sheet =
                    workbook.getSheetAt(0);

            List<CellRangeAddress> regions =
                    new ArrayList<>(
                            sheet.getMergedRegions()
                    );

            if (regions.isEmpty()) {

                return content;
            }

            DataFormatter formatter =
                    new DataFormatter();

            boolean changed =
                    false;

            for (CellRangeAddress region : regions) {

                Row sourceRow =
                        sheet.getRow(
                                region.getFirstRow()
                        );

                if (sourceRow == null) {

                    continue;
                }

                Cell source =
                        sourceRow.getCell(
                                region.getFirstColumn()
                        );

                if (source == null) {

                    continue;
                }

                String value =
                        formatter
                                .formatCellValue(source)
                                .trim();

                if (isEmpty(value)) {

                    continue;
                }

                for (int rowIndex =
                     region.getFirstRow() + 1;
                     rowIndex <= region.getLastRow();
                     rowIndex++) {

                    Row targetRow =
                            sheet.getRow(rowIndex);

                    if (targetRow == null) {

                        targetRow =
                                sheet.createRow(rowIndex);
                    }

                    Cell target =
                            targetRow.getCell(
                                    region.getFirstColumn()
                            );

                    /*
                     * 合并区域里本来就写了值的不覆盖
                     */
                    if (target != null
                            && !isBlankCell(target)) {

                        continue;
                    }

                    if (target == null) {

                        target =
                                targetRow.createCell(
                                        region.getFirstColumn()
                                );
                    }

                    target.setCellValue(value);

                    changed = true;
                }
            }

            if (!changed) {

                return content;
            }

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            workbook.write(out);

            return out.toByteArray();

        } catch (Exception e) {

            /*
             * 补值只是兼容性增强：
             * 失败时保持原文件，让表头校验 / 行校验
             * 给出更明确的提示，不在这里吞掉业务校验。
             */
            return content;
        }
    }


    /**
     * 单元格是否为空
     */
    private boolean isBlankCell(
            Cell cell) {

        if (cell == null) {

            return true;
        }

        CellType type =
                cell.getCellType();

        if (type == CellType.BLANK) {

            return true;
        }

        if (type == CellType.STRING) {

            return isEmpty(
                    cell.getStringCellValue()
            );
        }

        return false;
    }


    /**
     * =========================================================
     * 单行解析与校验
     *
     * 返回 null 表示空行（跳过）；
     * 业务校验失败抛出 IllegalArgumentException。
     * =========================================================
     */
    private DepartmentScoreApply buildApply(
            DepartmentScoreApplyExcelRow row,
            Long departmentId,
            Long applicantId,
            Map<String, List<DepartmentScoreTemplate>> templatesByLabel,
            Map<String, List<DepartmentScoreTemplate>> templatesByName,
            Map<String, SysUser> studentCache,
            Set<String> studentNotUsable,
            Map<String, Integer> seenRows,
            int rowNumber) {

        String studentNo =
                normalizeStudentNo(row.getStudentNo());

        String studentName =
                trim(row.getName());

        String dateText =
                trim(row.getDate());

        String projectText =
                trim(row.getProject());

        String scoreText =
                trim(row.getScore());

        String remark =
                trim(row.getRemark());

        boolean emptyRow =
                isEmpty(studentNo)
                        && isEmpty(studentName)
                        && isEmpty(dateText)
                        && isEmpty(projectText)
                        && isEmpty(scoreText)
                        && isEmpty(remark);

        if (emptyRow) {

            return null;
        }

        /*
         * =====================================================
         * 1. 学号 / 学生
         * =====================================================
         */
        if (isEmpty(studentNo)) {

            throw new IllegalArgumentException(
                    "学号不能为空"
                            + "（如果是同一个学生的多行记录，"
                            + "请把「学号」「姓名」单元格纵向合并）"
            );
        }

        SysUser student =
                findStudent(
                        studentNo,
                        studentCache,
                        studentNotUsable
                );

        if (student == null) {

            throw new IllegalArgumentException(
                    "学号不存在或该学生已停用：" + studentNo
            );
        }

        /*
         * =====================================================
         * 2. 姓名交叉校验
         * =====================================================
         */
        if (!isEmpty(studentName)) {

            String expected =
                    trim(student.getRealName());

            if (!studentName.equals(expected)) {

                throw new IllegalArgumentException(
                        "姓名与学号不匹配：学号 "
                                + studentNo
                                + " 对应的是「"
                                + expected
                                + "」"
                );
            }
        }

        /*
         * =====================================================
         * 2.1 允许给自己申报
         *
         * 部门干事 / 副部长 / 部长 也可以给自己申报加减分，
         * 与单条申报 /departmentScoreApply/add 保持同一口径。
         *
         * 自己的申报不会出现在本人的待审核列表，
         * 需要由部门其他负责人初审、辅导员终审；
         * 审核侧「不能审核自己的申报」的约束保持不变。
         * =====================================================
         */

        /*
         * =====================================================
         * 3. 项目（必须来自本部门可用项目）
         * =====================================================
         */
        if (isEmpty(projectText)) {

            throw new IllegalArgumentException(
                    "项目不能为空，请从下拉列表中选择"
            );
        }

        DepartmentScoreTemplate template =
                resolveTemplate(
                        projectText,
                        scoreText,
                        templatesByLabel,
                        templatesByName
                );

        /*
         * =====================================================
         * 4. 分值校验（只校验，不覆盖模板分值）
         * =====================================================
         */
        BigDecimal providedScore =
                parseScore(scoreText);

        if (providedScore != null
                && providedScore.abs().compareTo(
                template.getScore()) != 0) {

            throw new IllegalArgumentException(
                    "分值与该项目的分值不一致："
                            + template.getName()
                            + " 应为 "
                            + formatScore(template.getScore())
                            + " 分"
            );
        }

        /*
         * =====================================================
         * 5. 日期
         * =====================================================
         */
        LocalDate activityDate =
                parseActivityDate(dateText);

        /*
         * =====================================================
         * 6. 备注
         * =====================================================
         */
        String description;

        if (isEmpty(remark)) {

            description = template.getDescription();

        } else {

            if (remark.length() > 1000) {

                throw new IllegalArgumentException(
                        "备注不能超过 1000 个字符"
                );
            }

            description = remark;
        }

        /*
         * =====================================================
         * 7. 窄口径判重
         *
         * 同一个学生的同一个项目允许出现多行（例如多次旷课），
         * 只有「同一学生 + 同一部门 + 同一项目 + 同一天（日期非空）
         * + 同一备注」完全一致时才算重复，
         * 用来拦截同一个文件重复导入 / 同一条记录重复提交。
         * 日期留空的行不判重。
         * =====================================================
         */
        if (activityDate != null) {

            String duplicateKey =
                    student.getId()
                            + ":"
                            + departmentId
                            + ":"
                            + template.getId()
                            + ":"
                            + activityDate
                            + ":"
                            + (description == null
                            ? ""
                            : description.trim());

            Integer seenRow =
                    seenRows.get(duplicateKey);

            if (seenRow != null) {

                throw new IllegalArgumentException(
                        "本文件第 "
                                + seenRow
                                + " 行与本行完全相同（同一学生、同一项目、同一天、同一备注），"
                                + "请确认是否重复填写"
                );
            }

            if (duplicateChecker.isDuplicate(
                    student.getId(),
                    departmentId,
                    template.getId(),
                    activityDate,
                    description
            )) {

                throw new IllegalArgumentException(
                        "该学生该项目在 "
                                + activityDate
                                + " 已有相同备注的申报，请勿重复提交"
                );
            }

            seenRows.put(duplicateKey, rowNumber);
        }

        /*
         * =====================================================
         * 8. 组装申报单
         * =====================================================
         */
        LocalDateTime now =
                LocalDateTime.now();

        DepartmentScoreApply apply =
                new DepartmentScoreApply();

        apply.setStudentId(student.getId());

        apply.setApplicantId(applicantId);

        apply.setDepartmentId(departmentId);

        apply.setTemplateId(template.getId());

        apply.setScoreType(template.getScoreType());

        /*
         * 申报表中的 score 永远保存正数
         */
        apply.setScore(template.getScore());

        apply.setTitle(template.getName().trim());

        apply.setDescription(description);

        /*
         * Excel 导入没有证明材料
         */
        apply.setEvidenceUrl(null);

        apply.setStatus((short) 0);

        apply.setFinalStatus((short) 0);

        apply.setActivityDate(activityDate);

        apply.setCreateTime(now);

        apply.setUpdateTime(now);

        return apply;
    }

    /**
     * =========================================================
     * 查找学生
     *
     * 只接受启用状态的学生；
     * sys_user.student_no 有唯一约束，这里取不到即为无效学号。
     * =========================================================
     */
    private SysUser findStudent(
            String studentNo,
            Map<String, SysUser> studentCache,
            Set<String> studentNotUsable) {

        if (studentCache.containsKey(studentNo)) {

            return studentCache.get(studentNo);
        }

        if (studentNotUsable.contains(studentNo)) {

            return null;
        }

        SysUser student =
                sysUserMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(
                                        SysUser::getStudentNo,
                                        studentNo
                                )
                                .last("LIMIT 1")
                );

        if (student == null
                || !Short.valueOf((short) 1).equals(
                student.getStatus())) {

            studentNotUsable.add(studentNo);

            return null;
        }

        studentCache.put(studentNo, student);

        return student;
    }

    /**
     * =========================================================
     * 解析项目
     *
     * 匹配顺序：
     * 1. 完整项目标签（项目名（+1分））
     * 2. 项目名称
     * 3. 从「名称（+1分）」中拆出的名称
     *
     * 同名多条时用「分值」列消歧，
     * 无法消歧则强制要求填写带分值的完整标签，
     * 避免匹配到不同分值的同名项目。
     * =========================================================
     */
    private DepartmentScoreTemplate resolveTemplate(
            String projectText,
            String scoreText,
            Map<String, List<DepartmentScoreTemplate>> templatesByLabel,
            Map<String, List<DepartmentScoreTemplate>> templatesByName) {

        List<DepartmentScoreTemplate> candidates =
                templatesByLabel.get(
                        normalizeLabel(projectText)
                );

        if (candidates == null || candidates.isEmpty()) {

            candidates =
                    templatesByName.get(
                            normalizeLabel(projectText)
                    );
        }

        if (candidates == null || candidates.isEmpty()) {

            String namePart =
                    extractNamePart(projectText);

            if (namePart != null) {

                candidates =
                        templatesByName.get(
                                normalizeLabel(namePart)
                        );
            }
        }

        if (candidates == null || candidates.isEmpty()) {

            throw new IllegalArgumentException(
                    "项目《"
                            + projectText
                            + "》不在本部门可用项目中，请从下拉列表中选择"
            );
        }

        if (candidates.size() == 1) {

            DepartmentScoreTemplate only =
                    candidates.get(0);

            if (only.getScoreType() == null
                    || (only.getScoreType() != 1
                    && only.getScoreType() != -1)
                    || only.getScore() == null
                    || only.getScore().compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        "项目《"
                                + projectText
                                + "》的加减分类型或分值配置错误，请联系管理员"
                );
            }

            return only;
        }

        /*
         * 同名多条：用分值列消歧
         */
        BigDecimal providedScore =
                parseScore(scoreText);

        if (providedScore != null) {

            List<DepartmentScoreTemplate> matched =
                    new ArrayList<>();

            for (DepartmentScoreTemplate template : candidates) {

                if (template.getScore() != null
                        && template.getScoreType() != null
                        && template.getScore().compareTo(
                        providedScore.abs()) == 0) {

                    matched.add(template);
                }
            }

            if (matched.size() == 1) {

                return matched.get(0);
            }

            if (matched.isEmpty()) {

                throw new IllegalArgumentException(
                        "项目《"
                                + projectText
                                + "》没有 "
                                + formatScore(providedScore.abs())
                                + " 分的配置，请从下拉列表中选择"
                );
            }
        }

        throw new IllegalArgumentException(
                "项目《"
                        + projectText
                        + "》存在同名多项，请在项目列填写带分值的完整名称（例如 项目名（+1分））"
        );
    }

    /**
     * 从「名称（+1分）」中拆出名称，解析失败返回 null
     */
    private String extractNamePart(
            String projectText) {

        if (projectText == null) {

            return null;
        }

        String text =
                projectText.trim();

        int end =
                text.length();

        if (end > 0
                && (text.charAt(end - 1) == '）'
                || text.charAt(end - 1) == ')')) {

            end--;
        }

        int open =
                Math.max(
                        text.lastIndexOf('（'),
                        text.lastIndexOf('(')
                );

        if (open <= 0 || open >= end) {

            return null;
        }

        String suffix =
                text.substring(open + 1, end).trim();

        if (!suffix.endsWith("分")) {

            return null;
        }

        return text.substring(0, open).trim();
    }

    /**
     * 建立 标签 / 名称 索引
     */
    private void indexTemplates(
            List<DepartmentScoreTemplate> templates,
            Map<String, List<DepartmentScoreTemplate>> templatesByLabel,
            Map<String, List<DepartmentScoreTemplate>> templatesByName) {

        for (DepartmentScoreTemplate template : templates) {

            String label =
                    normalizeLabel(
                            buildTemplateLabel(template)
                    );

            templatesByLabel
                    .computeIfAbsent(label, key -> new ArrayList<>())
                    .add(template);

            String name =
                    normalizeLabel(template.getName());

            templatesByName
                    .computeIfAbsent(name, key -> new ArrayList<>())
                    .add(template);
        }
    }

    /**
     * 部门可用项目：启用 + 分值/类型合法
     */
    private List<DepartmentScoreTemplate> loadAvailableTemplates(
            Long departmentId) {

        List<DepartmentScoreTemplate> list =
                templateService.list(
                        new LambdaQueryWrapper<DepartmentScoreTemplate>()
                                .eq(
                                        DepartmentScoreTemplate::getDepartmentId,
                                        departmentId
                                )
                                .eq(
                                        DepartmentScoreTemplate::getStatus,
                                        (short) 1
                                )
                                .orderByAsc(
                                        DepartmentScoreTemplate::getId
                                )
                );

        List<DepartmentScoreTemplate> usable =
                new ArrayList<>();

        if (list == null) {

            return usable;
        }

        for (DepartmentScoreTemplate template : list) {

            boolean valid =
                    template != null
                            && !isEmpty(template.getName())
                            && template.getScoreType() != null
                            && (template.getScoreType() == 1
                            || template.getScoreType() == -1)
                            && template.getScore() != null
                            && template.getScore()
                            .compareTo(BigDecimal.ZERO) > 0;

            if (valid) {

                usable.add(template);
            }
        }

        return usable;
    }

    /**
     * 校验部门存在且启用
     */
    private Department requireEnabledDepartment(
            Long departmentId) {

        if (departmentId == null) {

            throw new IllegalArgumentException("请选择申报部门");
        }

        Department department =
                departmentMapper.selectById(departmentId);

        if (department == null) {

            throw new IllegalArgumentException("申报部门不存在");
        }

        if (!Short.valueOf((short) 1).equals(
                department.getStatus())) {

            throw new IllegalArgumentException("该部门当前未启用");
        }

        return department;
    }

    /**
     * =========================================================
     * 校验当前用户具备该部门的申报权限
     *
     * 岗位沿用系统现有数据：干事 / 副部长 / 部长
     * =========================================================
     */
    private void requireApplyPermission(
            Long departmentId,
            Long currentUserId) {

        if (currentUserId == null) {

            throw new IllegalArgumentException("请先登录");
        }

        SysUserDepartment relation =
                userDepartmentMapper.selectOne(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getUserId,
                                        currentUserId
                                )
                                .eq(
                                        SysUserDepartment::getDepartmentId,
                                        departmentId
                                )
                                .eq(
                                        SysUserDepartment::getStatus,
                                        (short) 1
                                )
                );

        if (relation == null) {

            throw new IllegalArgumentException(
                    "你不是该部门在职成员，不能导入该部门申报"
            );
        }

        String position =
                relation.getPosition();

        boolean canApply =
                "干事".equals(position)
                        || "副部长".equals(position)
                        || "部长".equals(position);

        if (!canApply) {

            throw new IllegalArgumentException(
                    "你没有部门加减分申报权限"
            );
        }
    }

    /**
     * 项目标签：项目名（+1分） / 项目名（-0.5分）
     */
    private String buildTemplateLabel(
            DepartmentScoreTemplate template) {

        String name =
                template.getName() == null
                        ? ""
                        : template.getName().trim();

        String sign =
                Short.valueOf((short) -1)
                        .equals(template.getScoreType())
                        ? "-"
                        : "+";

        return name
                + "（"
                + sign
                + formatScore(template.getScore())
                + "分）";
    }

    private String formatScore(
            BigDecimal score) {

        if (score == null) {

            return "0";
        }

        return score
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    /**
     * 统一标签比较口径：去空格、全角括号转半角
     */
    private String normalizeLabel(
            String value) {

        if (value == null) {

            return "";
        }

        return value
                .trim()
                .replaceAll("\\s+", "")
                .replace('（', '(')
                .replace('）', ')');
    }

    /**
     * 学号归一化：兼容数字单元格（20240101.0 / 2.0240101E7）
     */
    private String normalizeStudentNo(
            String value) {

        if (isEmpty(value)) {

            return null;
        }

        String text =
                value.trim();

        if (text.matches("\\d+\\.0+")) {

            text = text.substring(
                    0,
                    text.indexOf('.')
            );
        }

        if (text.matches(".*[eE].*")) {

            try {

                text = new BigDecimal(text)
                        .toPlainString();

            } catch (NumberFormatException ignored) {

                return text;
            }
        }

        return text;
    }

    /**
     * 分值解析，空值返回 null，格式错误抛出业务异常
     */
    private BigDecimal parseScore(
            String scoreText) {

        if (isEmpty(scoreText)) {

            return null;
        }

        String text =
                scoreText.trim()
                        .replace("+", "");

        try {

            return new BigDecimal(text)
                    .setScale(2, RoundingMode.HALF_UP);

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "分值格式不正确：" + scoreText
            );
        }
    }

    /**
     * 日期解析，空值返回 null
     *
     * 支持 2024-05-01 / 2024/5/1 / 2024.5.1 / 20240501 /
     * Excel 日期序列号 / 带时间的单元格
     */
    private LocalDate parseActivityDate(
            String dateText) {

        if (isEmpty(dateText)) {

            return null;
        }

        String text =
                dateText.trim();

        if (text.matches("\\d+\\.0+")) {

            text = text.substring(
                    0,
                    text.indexOf('.')
            );
        }

        int spaceIndex =
                text.indexOf(' ');

        if (spaceIndex > 0) {

            text = text.substring(0, spaceIndex);
        }

        int timeIndex =
                text.indexOf('T');

        if (timeIndex > 0) {

            text = text.substring(0, timeIndex);
        }

        if (text.matches("\\d{8}")) {

            return LocalDate.parse(
                    text,
                    DateTimeFormatter.BASIC_ISO_DATE
            );
        }

        /*
         * Excel 日期序列号（1900 日期系统）
         */
        if (text.matches("\\d{5}")) {

            long serial =
                    Long.parseLong(text);

            return LocalDate.of(1899, 12, 30)
                    .plusDays(serial);
        }

        String normalized =
                text.replace('/', '-')
                        .replace('.', '-');

        String[] parts =
                normalized.split("-");

        if (parts.length == 3) {

            try {

                return LocalDate.of(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2])
                );

            } catch (Exception ignored) {

                // 落到下面统一报错
            }
        }

        throw new IllegalArgumentException(
                "日期格式不正确（"
                        + dateText
                        + "），请使用 2024-05-01 格式"
        );
    }

    /* =========================================================
     * 模板文件生成
     * ========================================================= */

    private void writeHeaderRow(
            XSSFWorkbook workbook,
            XSSFSheet sheet) {

        CellStyle headerStyle =
                workbook.createCellStyle();

        Font font =
                workbook.createFont();

        font.setBold(true);

        headerStyle.setFont(font);

        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        headerStyle.setFillForegroundColor(
                IndexedColors.GREY_25_PERCENT.getIndex()
        );

        headerStyle.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        headerStyle.setBorderBottom(BorderStyle.THIN);

        headerStyle.setBorderTop(BorderStyle.THIN);

        headerStyle.setBorderLeft(BorderStyle.THIN);

        headerStyle.setBorderRight(BorderStyle.THIN);

        XSSFRow header =
                sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {

            header.createCell(i)
                    .setCellValue(HEADERS[i]);

            header.getCell(i)
                    .setCellStyle(headerStyle);
        }

        int[] widths =
                {18, 12, 14, 40, 10, 40};

        for (int i = 0; i < widths.length; i++) {

            sheet.setColumnWidth(i, widths[i] * 256);
        }

        sheet.createFreezePane(0, 1);
    }

    /**
     * 隐藏 sheet：项目下拉数据源
     */
    private void writeOptionSheet(
            XSSFWorkbook workbook,
            List<String> labels) {

        XSSFSheet optionSheet =
                workbook.createSheet(OPTION_SHEET_NAME);

        for (int i = 0; i < labels.size(); i++) {

            XSSFRow row =
                    optionSheet.createRow(i);

            row.createCell(0)
                    .setCellValue(labels.get(i));
        }

        optionSheet.setColumnWidth(0, 40 * 256);

        XSSFName name =
                workbook.createName();

        name.setNameName(OPTION_RANGE_NAME);

        name.setRefersToFormula(
                "'"
                        + OPTION_SHEET_NAME
                        + "'!$A$1:$A$"
                        + Math.max(labels.size(), 1)
        );

        workbook.setSheetHidden(
                workbook.getSheetIndex(optionSheet),
                true
        );
    }

    /**
     * 「项目」列数据有效性：只允许从下拉中选择本部门项目
     */
    private void applyProjectValidation(
            XSSFWorkbook workbook,
            XSSFSheet sheet,
            int optionCount) {

        if (optionCount <= 0) {

            return;
        }

        DataValidationHelper helper =
                sheet.getDataValidationHelper();

        DataValidationConstraint constraint =
                helper.createFormulaListConstraint(
                        OPTION_RANGE_NAME
                );

        CellRangeAddressList range =
                new CellRangeAddressList(
                        1,
                        TEMPLATE_DATA_ROWS,
                        PROJECT_COLUMN_INDEX,
                        PROJECT_COLUMN_INDEX
                );

        DataValidation validation =
                helper.createValidation(
                        constraint,
                        range
                );

        validation.setShowErrorBox(true);

        validation.setErrorStyle(
                DataValidation.ErrorStyle.STOP
        );

        validation.createErrorBox(
                "项目无效",
                "请从下拉列表中选择本部门已有的加减分项目，不能手动输入"
        );

        validation.setShowPromptBox(true);

        validation.createPromptBox(
                "加减分项目",
                "从下拉列表选择，加分/减分类型和分值由项目决定"
        );

        sheet.addValidationData(validation);
    }

    /**
     * 填写说明 sheet，保证不误导使用者
     */
    private void writeGuideSheet(
            XSSFWorkbook workbook) {

        XSSFSheet guide =
                workbook.createSheet(GUIDE_SHEET_NAME);

        String[] lines = {
                "部门学生加减分申报导入说明",
                "1. 六列固定为：学号、姓名、日期、项目、分值、备注，请勿增删列。",
                "2. 学号必填，且必须是系统中已启用的学生；姓名用于交叉校验，填写后必须与学号一致。",
                "3. 项目必须从下拉列表中选择，不能手动输入；加分/减分类型由所选项目决定。",
                "4. 分值需与所选项目的分值一致（例如 +1 分项目填 1）；留空表示以项目分值为准。",
                "5. 日期可选，支持 2024-05-01 / 2024/5/1 / 20240501 格式。",
                "6. 备注可选，将作为申报说明；同一个学生的同一个项目可以填写多行（如多次旷课）。",
                "7. 只有同一学生、同一项目、同一天（日期非空）且备注完全相同时才会被判为重复；日期留空的行不做判重。",
                "8. 同一个学生填写多行（例如多次旷课）时，可以把「学号」「姓名」单元格纵向合并，系统会自动识别。",
                "9. 也可以给自己申报加减分；自己的申报不能由本人审核，需由部门其他负责人初审、辅导员终审。",
                "10. 导入后申报单进入「待部门审核」状态，仍需按原有流程审核。",
        };

        for (int i = 0; i < lines.length; i++) {

            XSSFRow row =
                    guide.createRow(i);

            row.createCell(0)
                    .setCellValue(lines[i]);
        }

        guide.setColumnWidth(0, 100 * 256);
    }

    /* =========================================================
     * 通用小工具
     * ========================================================= */

    private void addError(
            List<Map<String, Object>> errors,
            int rowNumber,
            String message) {

        if (errors.size() < MAX_ERRORS) {

            errors.add(
                    error(
                            rowNumber,
                            message
                    )
            );
        }
    }

    private Map<String, Object> error(
            int row,
            String message) {

        Map<String, Object> map =
                new LinkedHashMap<>();

        map.put("row", row);

        map.put("message", message);

        return map;
    }

    private Map<String, Object> buildResult(
            int successCount,
            int failCount,
            List<Map<String, Object>> errors,
            String message) {

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put("message", message);

        result.put("successCount", successCount);

        result.put("failCount", failCount);

        result.put("totalCount", successCount + failCount);

        result.put("errors", errors);

        return result;
    }

    private String trim(
            String value) {

        return value == null
                ? null
                : value.trim();
    }

    private boolean isEmpty(
            String value) {

        return value == null
                || value.trim().isEmpty();
    }
}
