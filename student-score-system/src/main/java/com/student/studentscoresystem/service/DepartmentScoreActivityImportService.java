package com.student.studentscoresystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.DepartmentScoreActivity;
import com.student.studentscoresystem.entity.DepartmentScoreApply;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.DepartmentScoreActivityMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.utils.DepartmentScoreReviewRules;
import com.student.studentscoresystem.utils.OrgPositionLevel;
import com.student.studentscoresystem.vo.ActivityBatchPreviewVO;
import com.student.studentscoresystem.vo.ActivityBatchRowVO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 活动批量加分申报。
 *
 * 与原有「部门加减分申报」完全并存、互不影响：
 *
 * 1. 独立模板：直接下发学院在用的活动加分统计表（Word 原表）；
 * 2. 独立解析、校验、导入：自动识别 Word / Excel 表结构；
 * 3. 一场活动一条活动记录，每名学生各生成一条申报明细，
 *    沿用原有审核链路（组织负责人 → 大社团 → 档案部 → 辅导员）。
 *
 * 审批结果不写在表里，全部由系统线上审批记录体现。
 */
@Service
public class DepartmentScoreActivityImportService {

    private static final String HEADER_NAME = "姓名";

    private static final String HEADER_STUDENT_NO = "学号";

    private static final String HEADER_SCORE = "加分分值";

    private static final String HEADER_EVIDENCE = "加分凭证";

    private static final String HEADER_CLASS_NAME = "班级";

    /**
     * 原始活动加分统计表（Word）。
     *
     * 系统直接下发学院在用原表，不做任何改写，
     * 导入时也按该表结构自动识别，保证「表原封不动当模板」。
     */
    private static final String TEMPLATE_RESOURCE =
            "templates/activity-score-template.docx";

    private final DepartmentMapper departmentMapper;

    private final SysUserMapper sysUserMapper;

    private final SysUserDepartmentMapper userDepartmentMapper;

    private final DepartmentScoreActivityMapper activityMapper;

    private final IDepartmentScoreApplyService applyService;

    public DepartmentScoreActivityImportService(
            DepartmentMapper departmentMapper,
            SysUserMapper sysUserMapper,
            SysUserDepartmentMapper userDepartmentMapper,
            DepartmentScoreActivityMapper activityMapper,
            IDepartmentScoreApplyService applyService
    ) {

        this.departmentMapper = departmentMapper;
        this.sysUserMapper = sysUserMapper;
        this.userDepartmentMapper = userDepartmentMapper;
        this.activityMapper = activityMapper;
        this.applyService = applyService;
    }

    /* =========================================================
     * 模板
     * ========================================================= */

    /**
     * 下载模板：直接返回学院在用的原表（Word），原封不动。
     */
    public byte[] buildTemplateDocument() {

        ClassPathResource resource =
                new ClassPathResource(TEMPLATE_RESOURCE);

        if (!resource.exists()) {

            throw new IllegalStateException(
                    "活动加分统计表模板未随应用打包：" + TEMPLATE_RESOURCE
            );
        }

        try (InputStream in = resource.getInputStream()) {

            return in.readAllBytes();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "读取活动加分统计表模板失败：" + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 备用模板：按原表结构生成的 Excel 版本（仍可导入）。
     */
    public byte[] buildTemplateWorkbook() {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("活动批量加分申报");

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle labelStyle = workbook.createCellStyle();
            Font labelFont = workbook.createFont();
            labelFont.setBold(true);
            labelStyle.setFont(labelFont);

            /* 标题 */
            Row title = sheet.createRow(0);
            Cell titleCell = title.createCell(0);
            titleCell.setCellValue("人工智能与计算机学院加分统计表");
            titleCell.setCellStyle(titleStyle);

            /* 活动名称 */
            Row nameRow = sheet.createRow(1);
            label(nameRow, 0, "活动名称", labelStyle);
            nameRow.createCell(1);

            /* 举办部门 / 举办时间 / 活动地点（与原 Word 一致） */
            Row infoRow = sheet.createRow(2);
            label(infoRow, 0, "举办部门", labelStyle);
            infoRow.createCell(1);
            label(infoRow, 2, "举办时间", labelStyle);
            infoRow.createCell(3);
            label(infoRow, 4, "活动地点", labelStyle);
            infoRow.createCell(5);

            /* 组织人员 */
            Row organizerRow = sheet.createRow(3);
            label(organizerRow, 0, "组织人员", labelStyle);
            organizerRow.createCell(1);

            /* 学生明细表头：左右两栏，与 Word 一致 */
            Row header = sheet.createRow(4);
            String[] blockHeaders = {"姓名", "班级", "学号", "加分凭证", "加分"};

            for (int block = 0; block < 2; block++) {

                for (int c = 0; c < blockHeaders.length; c++) {

                    Cell cell = header.createCell(
                            block * blockHeaders.length + c
                    );

                    cell.setCellValue(blockHeaders[c]);
                    cell.setCellStyle(labelStyle);
                }
            }

            /* 预留 10 行空明细（行数不限，用户可自行插入） */
            for (int i = 0; i < 10; i++) {

                Row row = sheet.createRow(5 + i);

                for (int c = 0; c < 10; c++) {
                    row.createCell(c);
                }
            }

            /* 审批栏：仅保留业务含义，系统内线上审批，无需填写 */
            Row approval = sheet.createRow(16);
            approval.createCell(0).setCellValue("活动组织者意见");
            approval.createCell(2).setCellValue("档案部主要负责人意见");
            approval.createCell(4).setCellValue("指导老师意见");
            approval.createCell(6).setCellValue("团委意见");
            approval.createCell(8).setCellValue("团委公章");

            Row approvalNote = sheet.createRow(17);
            approvalNote.createCell(0).setCellValue(
                    "以上审批栏在系统中由线上审批记录体现，无需在此填写，也不生成实体公章。"
            );

            Row archiveNote = sheet.createRow(18);
            archiveNote.createCell(0).setCellValue(
                    "本表一式两份，签字后档案部存档一份，承办活动部门存档一份。"
            );

            for (int c = 0; c < 10; c++) {
                sheet.setColumnWidth(c, 16 * 256);
            }

            sheet.setColumnWidth(3, 24 * 256);
            sheet.setColumnWidth(8, 24 * 256);

            /* 填写说明 */
            XSSFSheet note = workbook.createSheet("填写说明");

            String[] notes = {
                    "1. 活动名称、举办部门、举办时间为必填项。",
                    "2. 举办部门必须与本人在系统中的所属组织名称完全一致。",
                    "3. 学生明细左右两栏均可填写（与原表一致），行数不限；",
                    "   每名学生的加分与凭证可以不同。",
                    "4. 系统按「学号」识别学生，姓名、班级仅用于核对，填错会被拦截。",
                    "5. 加分必须为大于 0 的数字（只填数字，不用写 + 号）。",
                    "6. 同一场活动中，同一学生重复出现且分值相同会被判为重复导入。",
                    "7. 底部的组织者 / 档案部 / 指导老师 / 团委审批栏",
                    "   全部在系统内线上完成，不需要在 Excel 中填写审批意见，",
                    "   也不会生成实体公章。",
                    "8. 「一式两份」为线下存档说明，电子申报无需提交纸质材料。"
            };

            for (int i = 0; i < notes.length; i++) {
                note.createRow(i).createCell(0).setCellValue(notes[i]);
            }

            note.setColumnWidth(0, 90 * 256);

            workbook.write(out);

            return out.toByteArray();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "生成活动批量加分模板失败：" + e.getMessage(),
                    e
            );
        }
    }

    private void label(
            Row row,
            int column,
            String text,
            CellStyle style
    ) {

        Cell cell = row.createCell(column);

        cell.setCellValue(text);
        cell.setCellStyle(style);
    }

    /* =========================================================
     * 预览（导入前核对）
     * ========================================================= */

    public ActivityBatchPreviewVO preview(
            MultipartFile file,
            Long applicantId
    ) {

        Parsed parsed = parse(file);

        ActivityBatchPreviewVO vo = new ActivityBatchPreviewVO();

        vo.setActivityName(parsed.activityName);
        vo.setDepartmentName(parsed.departmentName);
        vo.setActivityDate(parsed.activityDate);
        vo.setLocation(parsed.location);
        vo.setOrganizers(parsed.organizers);
        vo.setEvidence(parsed.evidence);

        String headerError = checkHeader(parsed, applicantId);

        List<ActivityBatchRowVO> rows =
                validateRows(parsed, headerError == null
                        ? findDepartment(parsed.departmentName)
                        : null);

        vo.setRows(rows);

        int valid = 0;

        for (ActivityBatchRowVO row : rows) {

            if (row.getError() == null) {
                valid++;
            }
        }

        vo.setValidCount(valid);
        vo.setInvalidCount(rows.size() - valid);
        vo.setMessage(headerError);

        return vo;
    }

    /* =========================================================
     * 导入
     * ========================================================= */

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importBatch(
            MultipartFile file,
            Long applicantId
    ) {

        Parsed parsed = parse(file);

        String headerError = checkHeader(parsed, applicantId);

        if (headerError != null) {

            throw new IllegalArgumentException(headerError);
        }

        Department department = findDepartment(parsed.departmentName);

        LocalDate activityDate = parsed.activityDateValue;

        List<ActivityBatchRowVO> rows =
                validateRows(parsed, department);

        int validCount = 0;

        for (ActivityBatchRowVO row : rows) {

            if (row.getError() == null) {
                validCount++;
            }
        }

        if (validCount == 0) {

            throw new IllegalArgumentException(
                    "没有可导入的有效学生记录，请先修正文件中的错误"
            );
        }

        DepartmentScoreActivity activity =
                findOrCreateActivity(
                        department,
                        parsed,
                        applicantId,
                        activityDate
                );

        String applicantLevel =
                resolveLevel(applicantId, department.getId());

        Map<String, Object> result = new LinkedHashMap<>();

        List<Map<String, Object>> errors = new ArrayList<>();

        int successCount = 0;

        LocalDateTime now = LocalDateTime.now();

        for (ActivityBatchRowVO row : rows) {

            if (row.getError() != null) {

                errors.add(error(row.getRowNumber(), row.getError()));

                continue;
            }

            DepartmentScoreApply apply = new DepartmentScoreApply();

            apply.setStudentId(row.getStudentId());

            apply.setApplicantId(applicantId);
            apply.setDepartmentId(department.getId());
            apply.setActivityId(activity.getId());
            apply.setTemplateId(null);

            /* 活动批量申报固定为加分 */
            apply.setScoreType((short) 1);
            apply.setScore(row.getScore());

            /* 活动名称作为加减分项目名称 */
            apply.setTitle(activity.getName());

            apply.setDescription(
                    buildDescription(parsed)
            );

            apply.setActivityDate(activityDate);

            apply.setEvidenceUrl(
                    isBlank(row.getEvidence())
                            ? activity.getEvidenceUrl()
                            : row.getEvidence()
            );

            apply.setStatus(
                    DepartmentScoreReviewRules.initialStatus(
                            department,
                            applicantLevel
                    )
            );

            apply.setBigClubStatus(
                    DepartmentScoreReviewRules.initialBigClubStatus(
                            department
                    )
            );

            apply.setFinalStatus((short) 0);
            apply.setArchiveStatus((short) 0);
            apply.setCreateTime(now);
            apply.setUpdateTime(now);

            if (applyService.save(apply)) {

                successCount++;

            } else {

                errors.add(
                        error(row.getRowNumber(), "申报保存失败")
                );
            }
        }

        result.put("activityId", activity.getId());
        result.put("activityName", activity.getName());
        result.put("successCount", successCount);
        result.put("failCount", errors.size());
        result.put("errors", errors);
        result.put(
                "message",
                "活动批量加分申报导入完成：成功 "
                        + successCount
                        + " 条，失败 "
                        + errors.size()
                        + " 条"
        );

        return result;
    }

    /* =========================================================
     * 解析
     * ========================================================= */

    private Parsed parse(MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "请选择活动加分统计表文件（.docx 或 .xlsx）"
            );
        }

        String filename = file.getOriginalFilename();

        String lower = filename == null
                ? ""
                : filename.toLowerCase(Locale.ROOT);

        /* 学院原表是 Word，直接按 Word 表格识别 */
        if (lower.endsWith(".docx")) {

            try (InputStream in = file.getInputStream()) {

                return parseDocx(in);

            } catch (IllegalArgumentException e) {

                throw e;

            } catch (Exception e) {

                throw new IllegalArgumentException(
                        "Word 文件读取失败：" + e.getMessage()
                );
            }
        }

        if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {

            throw new IllegalArgumentException(
                    "只支持 .docx、.xlsx 或 .xls 格式的模板文件"
            );
        }

        try (InputStream in = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {

            if (workbook.getNumberOfSheets() == 0) {

                throw new IllegalArgumentException("Excel 中没有工作表");
            }

            Sheet sheet = workbook.getSheetAt(0);

            Parsed parsed = new Parsed();

            int headerRowIndex = findHeaderRow(sheet);

            if (headerRowIndex < 0) {

                throw new IllegalArgumentException(
                        "未找到学生明细表头，请使用系统提供的活动批量加分模板"
                );
            }

            readActivityInfo(sheet, headerRowIndex, parsed);
            readStudentRows(sheet, headerRowIndex, parsed);

            return parsed;

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Excel 读取失败：" + e.getMessage()
            );
        }
    }

    private int findHeaderRow(Sheet sheet) {

        for (int i = 0; i <= sheet.getLastRowNum(); i++) {

            Row row = sheet.getRow(i);

            if (row == null) {
                continue;
            }

            boolean hasName = false;
            boolean hasStudentNo = false;

            int last = Math.max(
                    row.getLastCellNum(),
                    (short) 0
            );

            for (int c = 0; c < last; c++) {

                String text = cellText(row.getCell(c));

                if (HEADER_NAME.equals(text)) {
                    hasName = true;
                } else if (HEADER_STUDENT_NO.equals(text)) {
                    hasStudentNo = true;
                }
            }

            if (hasName && hasStudentNo) {
                return i;
            }
        }

        return -1;
    }

    /**
     * 读取表头上方的活动基本信息。
     *
     * 兼容两种常见写法：
     *   ① 竖向：标签在 A 列，值在 B 列；
     *   ② 横向：举办部门 | 值 | 举办时间 | 值 | 活动地点 | 值
     *      （与原 Word 表格一致）。
     */
    private void readActivityInfo(
            Sheet sheet,
            int headerRowIndex,
            Parsed parsed
    ) {

        for (int i = 0; i < headerRowIndex; i++) {

            Row row = sheet.getRow(i);

            if (row == null) {
                continue;
            }

            int last = Math.max(
                    row.getLastCellNum(),
                    (short) 0
            );

            for (int c = 0; c < last; c++) {

                String label = cellText(row.getCell(c));

                String key = infoKey(label);

                if (key == null) {
                    continue;
                }

                Cell valueCell = row.getCell(c + 1);

                String value = cellText(valueCell);

                applyInfo(parsed, key, value, valueCell);

                c++;
            }
        }
    }

    private String infoKey(String label) {

        if (label == null || label.isEmpty()) {
            return null;
        }

        if (label.startsWith("活动名称")) {
            return "name";
        }

        if (label.startsWith("举办部门") || label.startsWith("申报组织")) {
            return "department";
        }

        if (label.startsWith("举办时间") || label.startsWith("活动时间")) {
            return "date";
        }

        if (label.startsWith("活动地点") || label.startsWith("地点")) {
            return "location";
        }

        if (label.startsWith("组织人员")) {
            return "organizers";
        }

        if (label.startsWith("活动凭证")) {
            return "evidence";
        }

        return null;
    }

    private void applyInfo(
            Parsed parsed,
            String key,
            String value,
            Cell valueCell
    ) {

        switch (key) {

            case "name":
                parsed.activityName = value;
                break;

            case "department":
                parsed.departmentName = value;
                break;

            case "date":

                parsed.activityDate = value;

                if (valueCell != null
                        && valueCell.getCellType()
                        == org.apache.poi.ss.usermodel.CellType.NUMERIC
                        && DateUtil.isCellDateFormatted(valueCell)) {

                    parsed.activityDateValue =
                            valueCell.getLocalDateTimeCellValue()
                                    .toLocalDate();
                }

                break;

            case "location":
                parsed.location = value;
                break;

            case "organizers":
                parsed.organizers = value;
                break;

            case "evidence":
                parsed.evidence = value;
                break;

            default:
                break;
        }
    }

    /**
     * 读取学生明细。
     *
     * 支持原 Word 表的左右双栏写法：
     *   姓名 | 班级 | 学号 | 加分凭证 | 加分 | 姓名 | 班级 | 学号 | 加分凭证 | 加分
     * 也兼容单栏写法。列位置按表头文本识别，不依赖固定列号。
     */
    private void readStudentRows(
            Sheet sheet,
            int headerRowIndex,
            Parsed parsed
    ) {

        Row headerRow = sheet.getRow(headerRowIndex);

        if (headerRow == null) {
            return;
        }

        int lastCol = Math.max(
                headerRow.getLastCellNum(),
                (short) 0
        );

        List<Integer> blockStarts = new ArrayList<>();

        for (int c = 0; c < lastCol; c++) {

            if (HEADER_NAME.equals(cellText(headerRow.getCell(c)))) {
                blockStarts.add(c);
            }
        }

        if (blockStarts.isEmpty()) {
            return;
        }

        for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {

            Row row = sheet.getRow(r);

            if (row == null) {
                continue;
            }

            /* 跳过底部审批栏 */
            if (isApprovalRow(row, lastCol)) {
                break;
            }

            for (int b = 0; b < blockStarts.size(); b++) {

                int start = blockStarts.get(b);

                int end = b + 1 < blockStarts.size()
                        ? blockStarts.get(b + 1)
                        : Math.min(start + 5, lastCol);

                Map<String, Integer> columns =
                        headerColumns(headerRow, start, end);

                RawRow raw = readRow(row, columns, r);

                if (raw != null) {
                    parsed.rows.add(raw);
                }
            }
        }
    }

    private boolean isApprovalRow(
            Row row,
            int lastCol
    ) {

        for (int c = 0; c < lastCol; c++) {

            String text = cellText(row.getCell(c));

            if (text.contains("意见") || text.contains("公章")) {
                return true;
            }
        }

        return false;
    }

    private Map<String, Integer> headerColumns(
            Row headerRow,
            int start,
            int end
    ) {

        Map<String, Integer> columns = new HashMap<>();

        for (int c = start; c < end; c++) {

            String key = headerKey(cellText(headerRow.getCell(c)));

            if (key != null && !columns.containsKey(key)) {
                columns.put(key, c);
            }
        }

        return columns;
    }

    private String headerKey(String text) {

        if (text == null || text.isEmpty()) {
            return null;
        }

        if (HEADER_NAME.equals(text)) {
            return "name";
        }

        if (HEADER_CLASS_NAME.equals(text)) {
            return "class";
        }

        if (HEADER_STUDENT_NO.equals(text)) {
            return "no";
        }

        if (HEADER_EVIDENCE.equals(text)) {
            return "evidence";
        }

        if (text.startsWith("加分")) {
            return "score";
        }

        return null;
    }

    private RawRow readRow(
            Row row,
            Map<String, Integer> columns,
            int rowIndex
    ) {

        Integer nameCol = columns.get("name");
        Integer noCol = columns.get("no");
        Integer scoreCol = columns.get("score");
        Integer evidenceCol = columns.get("evidence");
        Integer classCol = columns.get("class");

        String realName = nameCol == null
                ? ""
                : cellText(row.getCell(nameCol));

        String studentNo = noCol == null
                ? ""
                : cellText(row.getCell(noCol));

        String evidence = evidenceCol == null
                ? ""
                : cellText(row.getCell(evidenceCol));

        String className = classCol == null
                ? ""
                : cellText(row.getCell(classCol));

        String scoreText = scoreCol == null
                ? ""
                : cellText(row.getCell(scoreCol));

        Cell scoreCell = scoreCol == null
                ? null
                : row.getCell(scoreCol);

        if (studentNo.isEmpty()
                && realName.isEmpty()
                && scoreText.isEmpty()
                && evidence.isEmpty()) {

            return null;
        }

        RawRow raw = new RawRow();

        raw.rowNumber = rowIndex + 1;
        raw.realName = realName;
        raw.className = className;
        raw.studentNo = studentNo;
        raw.scoreText = scoreText;
        raw.evidence = evidence;

        if (scoreCell != null
                && scoreCell.getCellType()
                == org.apache.poi.ss.usermodel.CellType.NUMERIC) {

            raw.scoreValue = BigDecimal.valueOf(
                    scoreCell.getNumericCellValue()
            );
        }

        return raw;
    }

    /* =========================================================
     * Word 原表解析
     * ========================================================= */

    /**
     * 解析学院原表（Word）。
     *
     * 表结构（与原表一致，明细行数不限）：
     *   活动名称 / 举办部门 · 举办时间 · 活动地点 / 组织人员
     *   姓名 班级 学号 加分凭证 加分（左右双栏）
     *   底部审批栏
     *
     * 单元格按 w:tc 顺序读取，不依赖固定列号，
     * 因此原表中的合并单元格（活动名称、加分凭证等）不影响解析。
     */
    private Parsed parseDocx(InputStream in) {

        try (XWPFDocument document = new XWPFDocument(in)) {

            XWPFTable table = findActivityTable(document);

            if (table == null) {

                throw new IllegalArgumentException(
                        "未在 Word 中找到包含「姓名 / 学号」的学生明细表格，"
                                + "请使用系统提供的活动加分统计表模板"
                );
            }

            List<XWPFTableRow> rows = table.getRows();

            int headerIndex = -1;

            for (int i = 0; i < rows.size(); i++) {

                List<String> texts = docxTexts(rows.get(i));

                if (texts.contains(HEADER_NAME)
                        && texts.contains(HEADER_STUDENT_NO)) {

                    headerIndex = i;
                    break;
                }
            }

            if (headerIndex < 0) {

                throw new IllegalArgumentException(
                        "未找到学生明细表头（姓名 / 学号），"
                                + "请使用系统提供的活动加分统计表模板"
                );
            }

            Parsed parsed = new Parsed();

            readDocxActivityInfo(rows, headerIndex, parsed);
            readDocxStudentRows(rows, headerIndex, parsed);

            return parsed;

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Word 文件读取失败：" + e.getMessage(),
                    e
            );
        }
    }

    private XWPFTable findActivityTable(XWPFDocument document) {

        for (XWPFTable table : document.getTables()) {

            for (XWPFTableRow row : table.getRows()) {

                List<String> texts = docxTexts(row);

                if (texts.contains(HEADER_NAME)
                        && texts.contains(HEADER_STUDENT_NO)) {

                    return table;
                }
            }
        }

        return null;
    }

    private List<String> docxTexts(XWPFTableRow row) {

        List<String> texts = new ArrayList<>();

        for (XWPFTableCell cell : row.getTableCells()) {

            texts.add(normalizeCellText(cell));
        }

        return texts;
    }

    private String normalizeCellText(XWPFTableCell cell) {

        if (cell == null) {

            return "";
        }

        String text = cell.getText();

        if (text == null) {

            return "";
        }

        return text
                .replace('\u00A0', ' ')
                .replace('\u3000', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void readDocxActivityInfo(
            List<XWPFTableRow> rows,
            int headerIndex,
            Parsed parsed
    ) {

        for (int i = 0; i < headerIndex; i++) {

            List<String> texts = docxTexts(rows.get(i));

            for (int c = 0; c < texts.size(); c++) {

                String key = infoKey(texts.get(c));

                if (key == null) {
                    continue;
                }

                String value = docxValue(texts, c + 1);

                applyInfo(parsed, key, value, null);

                c++;
            }
        }
    }

    private void readDocxStudentRows(
            List<XWPFTableRow> rows,
            int headerIndex,
            Parsed parsed
    ) {

        List<String> header = docxTexts(rows.get(headerIndex));

        List<Integer> blockStarts = new ArrayList<>();

        for (int c = 0; c < header.size(); c++) {

            if (HEADER_NAME.equals(header.get(c))) {
                blockStarts.add(c);
            }
        }

        for (int i = headerIndex + 1; i < rows.size(); i++) {

            List<String> texts = docxTexts(rows.get(i));

            /* 底部审批栏不计入明细 */
            if (isApprovalTexts(texts)) {
                break;
            }

            for (int b = 0; b < blockStarts.size(); b++) {

                int start = blockStarts.get(b);

                int end = b + 1 < blockStarts.size()
                        ? blockStarts.get(b + 1)
                        : header.size();

                Map<String, Integer> columns = new HashMap<>();

                for (int c = start; c < end; c++) {

                    String key = headerKey(header.get(c));

                    if (key != null && !columns.containsKey(key)) {
                        columns.put(key, c);
                    }
                }

                RawRow raw = readDocxRow(texts, columns, i);

                if (raw != null) {
                    parsed.rows.add(raw);
                }
            }
        }
    }

    private boolean isApprovalTexts(List<String> texts) {

        for (String text : texts) {

            if (text.contains("意见") || text.contains("公章")) {
                return true;
            }
        }

        return false;
    }

    private RawRow readDocxRow(
            List<String> texts,
            Map<String, Integer> columns,
            int rowIndex
    ) {

        String realName = docxValue(texts, columns.get("name"));
        String className = docxValue(texts, columns.get("class"));
        String studentNo = docxValue(texts, columns.get("no"));
        String evidence = docxValue(texts, columns.get("evidence"));
        String scoreText = docxValue(texts, columns.get("score"));

        if (studentNo.isEmpty()
                && realName.isEmpty()
                && scoreText.isEmpty()
                && evidence.isEmpty()) {

            return null;
        }

        RawRow raw = new RawRow();

        raw.rowNumber = rowIndex + 1;
        raw.realName = realName;
        raw.className = className;
        raw.studentNo = studentNo;
        raw.scoreText = scoreText;
        raw.evidence = evidence;

        return raw;
    }

    private String docxValue(List<String> texts, Integer index) {

        if (index == null || index < 0 || index >= texts.size()) {
            return "";
        }

        return texts.get(index);
    }

    /* =========================================================
     * 校验
     * ========================================================= */

    private String checkHeader(
            Parsed parsed,
            Long applicantId
    ) {

        if (applicantId == null) {
            return "请先登录";
        }

        if (isBlank(parsed.activityName)) {
            return "活动名称不能为空";
        }

        if (parsed.activityName.trim().length() > 200) {
            return "活动名称不能超过 200 个字符";
        }

        if (isBlank(parsed.departmentName)) {
            return "举办部门不能为空";
        }

        Department department = findDepartment(parsed.departmentName);

        if (department == null) {
            return "系统中不存在该组织：" + parsed.departmentName;
        }

        if (!Short.valueOf((short) 1).equals(department.getStatus())) {
            return "该组织当前未启用：" + parsed.departmentName;
        }

        if (parsed.activityDateValue == null) {

            parsed.activityDateValue = parseDate(parsed.activityDate);

            if (parsed.activityDateValue == null) {
                return "举办时间不能为空，格式示例：2024-05-01";
            }
        }

        if (parsed.rows.isEmpty()) {
            return "学生加分明细不能为空";
        }

        String level = resolveLevel(applicantId, department.getId());

        if (!OrgPositionLevel.isLeaderOrDeputy(level)) {
            return "你不是该组织的负责人，没有活动加分申报权限";
        }

        return null;
    }

    private List<ActivityBatchRowVO> validateRows(
            Parsed parsed,
            Department department
    ) {

        List<ActivityBatchRowVO> result = new ArrayList<>();

        Set<String> seen = new HashSet<>();

        /* 同活动已存在的申报：用于防止重复导入 */
        Set<String> existingKeys = department == null
                ? new HashSet<>()
                : loadExistingKeys(department, parsed);

        for (RawRow raw : parsed.rows) {

            ActivityBatchRowVO vo = new ActivityBatchRowVO();

            vo.setRowNumber(raw.rowNumber);
            vo.setRealName(raw.realName);
            vo.setClassName(raw.className);
            vo.setStudentNo(raw.studentNo);
            vo.setEvidence(raw.evidence);
            vo.setScore(raw.scoreValue);

            if (isBlank(raw.studentNo)) {

                vo.setError("学号不能为空");
                result.add(vo);
                continue;
            }

            SysUser student = findStudent(raw.studentNo);

            if (student == null) {

                vo.setError("系统中不存在该学号：" + raw.studentNo);
                result.add(vo);
                continue;
            }

            vo.setStudentId(student.getId());

            if (!isBlank(raw.realName)
                    && !raw.realName.equals(student.getRealName())) {

                vo.setError(
                        "学号与姓名不一致：系统姓名是【"
                                + student.getRealName()
                                + "】"
                );
                result.add(vo);
                continue;
            }

            if (isBlank(raw.className)) {
                vo.setClassName(student.getClassName());
            }

            BigDecimal score = raw.scoreValue;

            if (score == null) {

                score = parseScore(raw.scoreText);
            }

            vo.setScore(score);

            if (score == null) {

                vo.setError("加分分值必须是数字");
                result.add(vo);
                continue;
            }

            if (score.compareTo(BigDecimal.ZERO) <= 0) {

                vo.setError("加分分值必须大于 0");
                result.add(vo);
                continue;
            }

            String key = student.getId() + "|" + score.stripTrailingZeros().toPlainString();

            if (!seen.add(key + "@file")) {

                vo.setError("文件中重复出现该学生的相同加分");
                result.add(vo);
                continue;
            }

            if (existingKeys.contains(key)) {

                vo.setError("该学生本次活动的加分已申报，请勿重复导入");
                result.add(vo);
                continue;
            }

            vo.setStudentNo(student.getStudentNo());

            result.add(vo);
        }

        return result;
    }

    private Set<String> loadExistingKeys(
            Department department,
            Parsed parsed
    ) {

        Set<String> keys = new HashSet<>();

        if (parsed.activityDateValue == null) {
            return keys;
        }

        DepartmentScoreActivity activity =
                activityMapper.selectOne(
                        new LambdaQueryWrapper<DepartmentScoreActivity>()
                                .eq(
                                        DepartmentScoreActivity::getDepartmentId,
                                        department.getId()
                                )
                                .eq(
                                        DepartmentScoreActivity::getName,
                                        parsed.activityName.trim()
                                )
                                .eq(
                                        DepartmentScoreActivity::getActivityDate,
                                        parsed.activityDateValue
                                )
                                .orderByAsc(
                                        DepartmentScoreActivity::getId
                                )
                                .last("LIMIT 1")
                );

        if (activity == null) {
            return keys;
        }

        List<DepartmentScoreApply> applies =
                applyService.list(
                        new LambdaQueryWrapper<DepartmentScoreApply>()
                                .eq(
                                        DepartmentScoreApply::getActivityId,
                                        activity.getId()
                                )
                                .ne(
                                        DepartmentScoreApply::getStatus,
                                        (short) 2
                                )
                );

        if (applies == null) {
            return keys;
        }

        for (DepartmentScoreApply apply : applies) {

            if (apply == null
                    || apply.getStudentId() == null
                    || apply.getScore() == null) {

                continue;
            }

            keys.add(
                    apply.getStudentId()
                            + "|"
                            + apply.getScore()
                            .stripTrailingZeros()
                            .toPlainString()
            );
        }

        return keys;
    }

    /* =========================================================
     * 保存
     * ========================================================= */

    private DepartmentScoreActivity findOrCreateActivity(
            Department department,
            Parsed parsed,
            Long applicantId,
            LocalDate activityDate
    ) {

        DepartmentScoreActivity exist =
                activityMapper.selectOne(
                        new LambdaQueryWrapper<DepartmentScoreActivity>()
                                .eq(
                                        DepartmentScoreActivity::getDepartmentId,
                                        department.getId()
                                )
                                .eq(
                                        DepartmentScoreActivity::getName,
                                        parsed.activityName.trim()
                                )
                                .eq(
                                        DepartmentScoreActivity::getActivityDate,
                                        activityDate
                                )
                                .orderByAsc(
                                        DepartmentScoreActivity::getId
                                )
                                .last("LIMIT 1")
                );

        if (exist != null) {
            return exist;
        }

        DepartmentScoreActivity activity =
                new DepartmentScoreActivity();

        activity.setName(parsed.activityName.trim());
        activity.setDepartmentId(department.getId());
        activity.setOrganizerId(applicantId);
        activity.setActivityDate(activityDate);
        activity.setLocation(parsed.location);
        activity.setOrganizers(parsed.organizers);
        activity.setEvidenceUrl(parsed.evidence);
        activity.setDescription(buildDescription(parsed));
        activity.setCreateTime(LocalDateTime.now());

        activityMapper.insert(activity);

        return activity;
    }

    private String buildDescription(Parsed parsed) {

        StringBuilder builder = new StringBuilder();

        if (!isBlank(parsed.location)) {
            builder.append("活动地点：").append(parsed.location.trim());
        }

        if (!isBlank(parsed.organizers)) {

            if (builder.length() > 0) {
                builder.append("；");
            }

            builder.append("组织人员：").append(parsed.organizers.trim());
        }

        return builder.length() == 0
                ? null
                : builder.toString();
    }

    /* =========================================================
     * 查询辅助
     * ========================================================= */

    private Department findDepartment(String name) {

        if (isBlank(name)) {
            return null;
        }

        return departmentMapper.selectOne(
                new LambdaQueryWrapper<Department>()
                        .eq(Department::getName, name.trim())
                        .last("LIMIT 1")
        );
    }

    private SysUser findStudent(String studentNo) {

        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getStudentNo, studentNo.trim())
                        .eq(SysUser::getStatus, (short) 1)
                        .last("LIMIT 1")
        );
    }

    private String resolveLevel(
            Long applicantId,
            Long departmentId
    ) {

        SysUserDepartment relation =
                userDepartmentMapper.selectOne(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getUserId,
                                        applicantId
                                )
                                .eq(
                                        SysUserDepartment::getDepartmentId,
                                        departmentId
                                )
                                .eq(
                                        SysUserDepartment::getStatus,
                                        (short) 1
                                )
                                .last("LIMIT 1")
                );

        return relation == null
                ? null
                : OrgPositionLevel.levelOf(relation);
    }

    /* =========================================================
     * 工具
     * ========================================================= */

    private String cellText(Cell cell) {

        if (cell == null) {
            return "";
        }

        return new DataFormatter()
                .formatCellValue(cell)
                .trim();
    }

    private BigDecimal parseScore(String text) {

        if (isBlank(text)) {
            return null;
        }

        try {
            return new BigDecimal(text.trim().replace("+", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(String text) {

        if (isBlank(text)) {
            return null;
        }

        String value = text.trim()
                .replace("年", "-")
                .replace("月", "-")
                .replace("日", "")
                .replace("/", "-")
                .replace(".", "-");

        /* 20240501 */
        if (value.matches("\\d{8}")) {

            try {
                return LocalDate.of(
                        Integer.parseInt(value.substring(0, 4)),
                        Integer.parseInt(value.substring(4, 6)),
                        Integer.parseInt(value.substring(6, 8))
                );
            } catch (Exception e) {
                return null;
            }
        }

        String[] parts = value.split("-");

        if (parts.length != 3) {
            return null;
        }

        try {
            return LocalDate.of(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
            );
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isBlank(String value) {

        return value == null || value.trim().isEmpty();
    }

    private Map<String, Object> error(
            int row,
            String message
    ) {

        Map<String, Object> map = new LinkedHashMap<>();

        map.put("row", row);
        map.put("message", message);

        return map;
    }

    /* =========================================================
     * 解析结构
     * ========================================================= */

    private static final class Parsed {

        private String activityName;

        private String departmentName;

        private String activityDate;

        private LocalDate activityDateValue;

        private String location;

        private String organizers;

        private String evidence;

        private final List<RawRow> rows = new ArrayList<>();
    }

    private static final class RawRow {

        private int rowNumber;

        private String realName;

        private String className;

        private String studentNo;

        private String scoreText;

        private BigDecimal scoreValue;

        private String evidence;
    }
}
