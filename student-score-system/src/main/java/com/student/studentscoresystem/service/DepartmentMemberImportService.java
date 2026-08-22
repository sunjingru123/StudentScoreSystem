package com.student.studentscoresystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class DepartmentMemberImportService {

    private final SysUserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final SysUserDepartmentMapper userDepartmentMapper;

    public DepartmentMemberImportService(
            SysUserMapper userMapper,
            DepartmentMapper departmentMapper,
            SysUserDepartmentMapper userDepartmentMapper
    ) {
        this.userMapper = userMapper;
        this.departmentMapper = departmentMapper;
        this.userDepartmentMapper = userDepartmentMapper;
    }

    /**
     * Excel 导入部门成员
     *
     * Excel 格式：
     *
     * 部门 | 学号 | 姓名 | 职位
     *
     * 例如：
     * 学习部 | 20240101 | 张三 | 部长
     * 学习部 | 20240102 | 李四 | 副部长
     * 学习部 | 20240103 | 王五 | 干事
     */
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importExcel(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择 Excel 文件");
        }

        String filename = file.getOriginalFilename();

        if (filename == null ||
                (!filename.toLowerCase().endsWith(".xlsx")
                        && !filename.toLowerCase().endsWith(".xls"))) {

            throw new IllegalArgumentException(
                    "只支持 .xlsx 或 .xls 格式的 Excel 文件"
            );
        }

        List<String> errors = new ArrayList<>();

        int successCount = 0;
        int updateCount = 0;
        int newCount = 0;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Excel 中没有工作表");
            }

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() <= 1) {
                throw new IllegalArgumentException("Excel 没有可导入的数据");
            }

            // 从第二行开始读取
            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                // 空行直接跳过
                if (isEmptyRow(row)) {
                    continue;
                }

                int excelRow = rowIndex + 1;

                try {

                    String departmentName =
                            getCellString(row.getCell(0));

                    String studentNo =
                            getCellString(row.getCell(1));

                    String realName =
                            getCellString(row.getCell(2));

                    String position =
                            getCellString(row.getCell(3));

                    // ============================
                    // 参数校验
                    // ============================

                    if (departmentName.isBlank()) {
                        throw new IllegalArgumentException("部门不能为空");
                    }

                    if (studentNo.isBlank()) {
                        throw new IllegalArgumentException("学号不能为空");
                    }

                    if (realName.isBlank()) {
                        throw new IllegalArgumentException("姓名不能为空");
                    }

                    if (position.isBlank()) {
                        throw new IllegalArgumentException("职位不能为空");
                    }

                    if (!isValidPosition(position)) {
                        throw new IllegalArgumentException(
                                "职位必须是：干事、副部长、部长"
                        );
                    }

                    // ============================
                    // 查学生
                    // ============================

                    SysUser user =
                            userMapper.selectOne(
                                    new LambdaQueryWrapper<SysUser>()
                                            .eq(
                                                    SysUser::getStudentNo,
                                                    studentNo
                                            )
                            );

                    if (user == null) {
                        throw new IllegalArgumentException(
                                "系统中不存在该学号学生：" + studentNo
                        );
                    }

                    // 姓名校验
                    if (!realName.equals(user.getRealName())) {
                        throw new IllegalArgumentException(
                                "学号对应姓名为【"
                                        + user.getRealName()
                                        + "】，Excel 中填写的是【"
                                        + realName
                                        + "】"
                        );
                    }

                    // ============================
                    // 查部门
                    // ============================

                    Department department =
                            departmentMapper.selectOne(
                                    new LambdaQueryWrapper<Department>()
                                            .eq(
                                                    Department::getName,
                                                    departmentName
                                            )
                            );

                    if (department == null) {
                        throw new IllegalArgumentException(
                                "系统中不存在部门：" + departmentName
                        );
                    }

                    // ============================
                    // 查部门成员关系
                    // ============================

                    SysUserDepartment relation =
                            userDepartmentMapper.selectOne(
                                    new LambdaQueryWrapper<SysUserDepartment>()
                                            .eq(
                                                    SysUserDepartment::getUserId,
                                                    user.getId()
                                            )
                                            .eq(
                                                    SysUserDepartment::getDepartmentId,
                                                    department.getId()
                                            )
                            );

                    if (relation == null) {

                        // ========================
                        // 新增
                        // ========================

                        relation = new SysUserDepartment();

                        relation.setUserId(user.getId());
                        relation.setDepartmentId(department.getId());
                        relation.setPosition(position);
                        relation.setStatus((short) 1);

                        userDepartmentMapper.insert(relation);

                        newCount++;
                        successCount++;

                    } else {

                        // ========================
                        // 已存在，更新
                        // ========================

                        relation.setPosition(position);
                        relation.setStatus((short) 1);

                        userDepartmentMapper.updateById(relation);

                        updateCount++;
                        successCount++;
                    }

                } catch (Exception e) {

                    errors.add(
                            "第 " + excelRow + " 行：" + e.getMessage()
                    );
                }
            }

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Excel 读取失败：" + e.getMessage(),
                    e
            );
        }

        ImportResult result = new ImportResult();

        result.setSuccessCount(successCount);
        result.setNewCount(newCount);
        result.setUpdateCount(updateCount);
        result.setErrorCount(errors.size());
        result.setErrors(errors);

        return result;
    }

    /**
     * 判断职位是否合法
     */
    private boolean isValidPosition(String position) {

        return "干事".equals(position)
                || "副部长".equals(position)
                || "部长".equals(position);
    }

    /**
     * 判断是否空行
     */
    private boolean isEmptyRow(Row row) {

        if (row == null) {
            return true;
        }

        for (int i = 0; i < 4; i++) {

            Cell cell = row.getCell(i);

            if (cell != null
                    && !getCellString(cell).isBlank()) {

                return false;
            }
        }

        return true;
    }

    /**
     * Excel 单元格转字符串
     */
    private String getCellString(Cell cell) {

        if (cell == null) {
            return "";
        }

        DataFormatter formatter = new DataFormatter();

        return formatter.formatCellValue(cell).trim();
    }

    /**
     * 导入结果
     */
    public static class ImportResult {

        private int successCount;

        private int newCount;

        private int updateCount;

        private int errorCount;

        private List<String> errors;

        public int getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }

        public int getNewCount() {
            return newCount;
        }

        public void setNewCount(int newCount) {
            this.newCount = newCount;
        }

        public int getUpdateCount() {
            return updateCount;
        }

        public void setUpdateCount(int updateCount) {
            this.updateCount = updateCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public void setErrorCount(int errorCount) {
            this.errorCount = errorCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
    }
}