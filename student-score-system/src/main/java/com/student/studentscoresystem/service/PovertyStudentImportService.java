package com.student.studentscoresystem.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.dto.PovertyStudentExcelRow;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.SysUserMapper;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * =========================================================
 * 贫困生名单导入
 * =========================================================
 *
 * 业务约定：
 *
 * 1. 模板固定两列：学号 | 姓名；
 * 2. 以「学号」唯一匹配 sys_user；
 * 3. 只把名单中的学生标记为贫困生（poverty_status = 1），
 *    不在名单中的学生保持原有状态，不清空历史标记；
 * 4. 单行失败只记录原因（行号 / 学号 / 原因），
 *    不影响其他行导入；
 * 5. 非业务异常直接抛出，整体回滚。
 * =========================================================
 */
@Service
public class PovertyStudentImportService {

    private static final int MAX_ERRORS = 100;

    private static final String[] HEADERS = {"学号", "姓名"};

    private final SysUserMapper sysUserMapper;

    private final PovertyImportPermissionService permissionService;

    public PovertyStudentImportService(
            SysUserMapper sysUserMapper,
            PovertyImportPermissionService permissionService
    ) {

        this.sysUserMapper = sysUserMapper;

        this.permissionService = permissionService;
    }

    /**
     * =========================================================
     * 生成导入模板
     * =========================================================
     */
    public byte[] buildTemplateWorkbook(
            Long currentUserId
    ) {

        requireImportPermission(currentUserId);


        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet =
                    workbook.createSheet("贫困生名单");

            CellStyle headerStyle =
                    workbook.createCellStyle();

            Font headerFont =
                    workbook.createFont();

            headerFont.setBold(true);

            headerStyle.setFont(headerFont);


            Row header =
                    sheet.createRow(0);

            for (int i = 0; i < HEADERS.length; i++) {

                header.createCell(i).setCellValue(HEADERS[i]);

                header.getCell(i).setCellStyle(headerStyle);
            }


            sheet.setColumnWidth(0, 20 * 256);

            sheet.setColumnWidth(1, 14 * 256);

            sheet.createFreezePane(0, 1);


            workbook.write(out);

            return out.toByteArray();

        } catch (IOException e) {

            throw new IllegalStateException(
                    "生成贫困生导入模板失败：" + e.getMessage(),
                    e
            );
        }
    }

    /**
     * =========================================================
     * 导入贫困生名单
     *
     * 返回结构与其他 Excel 导入保持一致：
     *   totalCount / successCount / failCount / errors
     * =========================================================
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importPovertyStudents(
            Long currentUserId,
            MultipartFile file
    ) {

        requireImportPermission(currentUserId);


        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException("请选择需要导入的 Excel 文件");
        }


        String filename =
                file.getOriginalFilename();

        if (filename == null
                || (!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")
                && !filename.toLowerCase(Locale.ROOT).endsWith(".xls"))) {

            throw new IllegalArgumentException(
                    "只支持 .xlsx 或 .xls 格式的 Excel 文件"
            );
        }


        List<PovertyStudentExcelRow> rows;

        try (InputStream in = file.getInputStream()) {

            rows = EasyExcel
                    .read(in)
                    .sheet(0)
                    .head(PovertyStudentExcelRow.class)
                    .doReadSync();

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "解析 Excel 失败，请使用系统提供的导入模板："
                            + e.getMessage()
            );
        }


        if (rows == null || rows.isEmpty()) {

            throw new IllegalArgumentException(
                    "Excel 中没有可导入的数据"
            );
        }


        List<Map<String, Object>> errors =
                new ArrayList<>();

        Set<String> handledStudentNos =
                new HashSet<>();

        int totalCount = 0;

        int successCount = 0;


        for (int index = 0; index < rows.size(); index++) {

            PovertyStudentExcelRow row =
                    rows.get(index);

            int rowNo =
                    index + 2;

            if (row == null) {

                continue;
            }


            String studentNo =
                    trim(row.getStudentNo());

            String realName =
                    trim(row.getRealName());


            if (studentNo.isEmpty() && realName.isEmpty()) {

                continue;
            }


            totalCount++;


            if (studentNo.isEmpty()) {

                addError(errors, rowNo, "", "学号不能为空");

                continue;
            }


            if (!handledStudentNos.add(studentNo)) {

                addError(errors, rowNo, studentNo, "该学号在文件中重复出现");

                continue;
            }


            SysUser user =
                    findStudentByStudentNo(studentNo);


            if (user == null) {

                addError(errors, rowNo, studentNo, "系统中不存在该学号");

                continue;
            }


            if (user.getStatus() != null && user.getStatus() != 1) {

                addError(errors, rowNo, studentNo, "该账号已被停用");

                continue;
            }


            /*
             * 已经是贫困生则直接计入成功，
             * 避免重复导入反复写库。
             */
            if (user.getPovertyStatus() != null
                    && user.getPovertyStatus() == 1) {

                successCount++;

                continue;
            }


            user.setPovertyStatus(
                    (short) 1
            );

            sysUserMapper.updateById(
                    user
            );

            successCount++;
        }


        return buildResult(
                totalCount,
                successCount,
                errors,
                "共处理 " + totalCount + " 条，"
                        + "成功标记 " + successCount + " 人"
        );
    }


    private void requireImportPermission(Long currentUserId) {

        if (currentUserId == null) {

            throw new IllegalArgumentException("请先登录");
        }


        if (!permissionService.canImportPovertyList(currentUserId)) {

            throw new SecurityException(
                    "你没有导入贫困生名单的权限"
            );
        }
    }


    private SysUser findStudentByStudentNo(String studentNo) {

        List<SysUser> users =
                sysUserMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(
                                        SysUser::getStudentNo,
                                        studentNo
                                )
                );


        if (users == null || users.isEmpty()) {

            return null;
        }


        return users.get(0);
    }


    private void addError(
            List<Map<String, Object>> errors,
            int rowNo,
            String studentNo,
            String reason
    ) {

        if (errors.size() >= MAX_ERRORS) {

            return;
        }


        Map<String, Object> item =
                new HashMap<>();

        item.put("row", rowNo);

        item.put("studentNo", studentNo);

        item.put("reason", reason);

        errors.add(item);
    }


    private Map<String, Object> buildResult(
            int totalCount,
            int successCount,
            List<Map<String, Object>> errors,
            String message
    ) {

        Map<String, Object> result =
                new HashMap<>();

        result.put("totalCount", totalCount);

        result.put("successCount", successCount);

        result.put("failCount", totalCount - successCount);

        result.put("errors", errors);

        result.put("message", message);

        return result;
    }


    private String trim(String value) {

        return value == null
                ? ""
                : value.trim();
    }
}