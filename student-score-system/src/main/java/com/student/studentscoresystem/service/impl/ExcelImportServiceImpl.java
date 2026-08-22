package com.student.studentscoresystem.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.student.studentscoresystem.dto.DepartmentMemberExcelRow;
import com.student.studentscoresystem.dto.StudentExcelRow;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.ExcelImportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Excel 批量导入服务
 */
@Service
public class ExcelImportServiceImpl implements ExcelImportService {

    private final SysUserMapper sysUserMapper;
    private final DepartmentMapper departmentMapper;
    private final SysUserDepartmentMapper userDepartmentMapper;

    public ExcelImportServiceImpl(
            SysUserMapper sysUserMapper,
            DepartmentMapper departmentMapper,
            SysUserDepartmentMapper userDepartmentMapper
    ) {
        this.sysUserMapper = sysUserMapper;
        this.departmentMapper = departmentMapper;
        this.userDepartmentMapper = userDepartmentMapper;
    }

    /**
     * ================================
     * 导入学生名单
     * ================================
     *
     * Excel：
     *
     * 学号 | 姓名 | 班级
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importStudents(MultipartFile file) {

        List<StudentExcelRow> rows;

        try {
            rows = EasyExcel
                    .read(file.getInputStream())
                    .head(StudentExcelRow.class)
                    .sheet()
                    .doReadSync();
        } catch (Exception e) {
            throw new RuntimeException("学生 Excel 读取失败：" + e.getMessage());
        }

        int successCount = 0;
        int failCount = 0;

        List<Map<String, Object>> errors = new ArrayList<>();

        if (rows == null || rows.isEmpty()) {
            return buildResult(
                    0,
                    0,
                    errors,
                    "Excel 中没有数据"
            );
        }

        /*
         * 用于检测 Excel 内部重复学号
         */
        Set<String> excelStudentNos = new HashSet<>();

        int rowNumber = 1;

        for (StudentExcelRow row : rows) {

            rowNumber++;

            String studentNo = trim(row.getStudentNo());
            String realName = trim(row.getRealName());
            String className = trim(row.getClassName());

            /*
             * 1. 学号不能为空
             */
            if (isEmpty(studentNo)) {
                failCount++;

                errors.add(error(
                        rowNumber,
                        "学号不能为空"
                ));

                continue;
            }

            /*
             * 2. 姓名不能为空
             */
            if (isEmpty(realName)) {
                failCount++;

                errors.add(error(
                        rowNumber,
                        "姓名不能为空"
                ));

                continue;
            }

            /*
             * 3. 班级不能为空
             */
            if (isEmpty(className)) {
                failCount++;

                errors.add(error(
                        rowNumber,
                        "班级不能为空"
                ));

                continue;
            }

            /*
             * 4. Excel 内部重复
             */
            if (!excelStudentNos.add(studentNo)) {

                failCount++;

                errors.add(error(
                        rowNumber,
                        "Excel 中存在重复学号：" + studentNo
                ));

                continue;
            }

            /*
             * 5. 查询数据库
             */
            SysUser user = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(
                                    SysUser::getStudentNo,
                                    studentNo
                            )
                            .last("LIMIT 1")
            );

            LocalDateTime now = LocalDateTime.now();

            /*
             * 6. 学生不存在 -> 新增
             */
            if (user == null) {

                user = new SysUser();

                user.setStudentNo(studentNo);
                user.setRealName(realName);
                user.setClassName(className);

                /*
                 * 新导入学生默认启用
                 */
                user.setStatus((short) 1);

                /*
                 * 注意：
                 *
                 * 这里不设置部门。
                 *
                 * 干事/副部长/部长属于额外部门关系，
                 * 后面通过部门成员 Excel 导入。
                 */

                user.setCreateTime(now);
                user.setUpdateTime(now);

                sysUserMapper.insert(user);

                successCount++;

                continue;
            }

            /*
             * 7. 学生已经存在
             *
             * 更新姓名、班级
             */
            user.setRealName(realName);
            user.setClassName(className);
            user.setUpdateTime(now);

            sysUserMapper.updateById(user);

            successCount++;
        }

        return buildResult(
                successCount,
                failCount,
                errors,
                "学生名单导入完成"
        );
    }

    /**
     * ================================
     * 导入部门成员
     * ================================
     *
     * Excel：
     *
     * 部门 | 学号 | 姓名 | 职位
     *
     * 职位：
     *
     * 干事
     * 副部长
     * 部长
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importDepartmentMembers(
            MultipartFile file
    ) {

        List<DepartmentMemberExcelRow> rows;

        try {
            rows = EasyExcel
                    .read(file.getInputStream())
                    .head(DepartmentMemberExcelRow.class)
                    .sheet()
                    .doReadSync();

        } catch (Exception e) {

            throw new RuntimeException(
                    "部门成员 Excel 读取失败：" + e.getMessage()
            );
        }

        int successCount = 0;
        int failCount = 0;

        List<Map<String, Object>> errors = new ArrayList<>();

        if (rows == null || rows.isEmpty()) {

            return buildResult(
                    0,
                    0,
                    errors,
                    "Excel 中没有数据"
            );
        }

        /*
         * 防止 Excel 自己重复写同一个部门成员
         *
         * key：
         *
         * 部门ID + 学号
         */
        Set<String> excelRelations = new HashSet<>();

        int rowNumber = 1;

        for (DepartmentMemberExcelRow row : rows) {

            rowNumber++;

            String departmentName =
                    trim(row.getDepartmentName());

            String studentNo =
                    trim(row.getStudentNo());

            String realName =
                    trim(row.getRealName());

            String position =
                    trim(row.getPosition());

            /*
             * ============================
             * 1. 基础参数校验
             * ============================
             */

            if (isEmpty(departmentName)) {

                failCount++;

                errors.add(error(
                        rowNumber,
                        "部门不能为空"
                ));

                continue;
            }

            if (isEmpty(studentNo)) {

                failCount++;

                errors.add(error(
                        rowNumber,
                        "学号不能为空"
                ));

                continue;
            }

            if (isEmpty(realName)) {

                failCount++;

                errors.add(error(
                        rowNumber,
                        "姓名不能为空"
                ));

                continue;
            }

            if (isEmpty(position)) {

                failCount++;

                errors.add(error(
                        rowNumber,
                        "职位不能为空"
                ));

                continue;
            }

            /*
             * ============================
             * 2. 职位校验
             * ============================
             */

            if (!isValidPosition(position)) {

                failCount++;

                errors.add(error(
                        rowNumber,
                        "职位不合法，只允许：干事、副部长、部长"
                ));

                continue;
            }

            /*
             * ============================
             * 3. 查询部门
             * ============================
             */

            Department department =
                    departmentMapper.selectOne(
                            new LambdaQueryWrapper<Department>()
                                    .eq(
                                            Department::getName,
                                            departmentName
                                    )
                                    .eq(
                                            Department::getStatus,
                                            (short) 1
                                    )
                                    .last("LIMIT 1")
                    );

            if (department == null) {

                failCount++;

                errors.add(error(
                        rowNumber,
                        "部门不存在或已停用：" + departmentName
                ));

                continue;
            }

            /*
             * ============================
             * 4. 查询学生
             * ============================
             */

            SysUser user =
                    sysUserMapper.selectOne(
                            new LambdaQueryWrapper<SysUser>()
                                    .eq(
                                            SysUser::getStudentNo,
                                            studentNo
                                    )
                                    .last("LIMIT 1")
                    );

            if (user == null) {

                failCount++;

                errors.add(error(
                        rowNumber,
                        "学生不存在，无法建立部门关系，学号：" + studentNo
                ));

                continue;
            }

            /*
             * ============================
             * 5. 姓名校验
             * ============================
             *
             * 学号是唯一依据。
             *
             * 姓名只是为了防止 Excel 填错人。
             */
            if (!realName.equals(user.getRealName())) {

                failCount++;

                errors.add(error(
                        rowNumber,
                        "姓名与系统不一致：学号 "
                                + studentNo
                                + "，系统姓名为【"
                                + user.getRealName()
                                + "】"
                ));

                continue;
            }

            /*
             * ============================
             * 6. Excel 内部重复
             * ============================
             */

            String relationKey =
                    department.getId()
                            + "_"
                            + user.getId();

            if (!excelRelations.add(relationKey)) {

                failCount++;

                errors.add(error(
                        rowNumber,
                        "Excel 中重复导入该部门成员"
                ));

                continue;
            }

            /*
             * ============================
             * 7. 查询数据库已有关系
             * ============================
             */

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
                                    .last("LIMIT 1")
                    );

            /*
             * ============================
             * 8. 已经存在
             * ============================
             *
             * 不重复插入。
             *
             * 如果职位发生变化，
             * 则更新职位。
             */
            if (relation != null) {

                relation.setPosition(position);
                relation.setStatus((short) 1);

                userDepartmentMapper.updateById(relation);

                successCount++;

                continue;
            }

            /*
             * ============================
             * 9. 创建新的部门关系
             * ============================
             */

            relation = new SysUserDepartment();

            relation.setUserId(user.getId());
            relation.setDepartmentId(department.getId());

            /*
             * 部长 / 副部长 / 干事
             */
            relation.setPosition(position);

            /*
             * 在职
             */
            relation.setStatus((short) 1);

            /*
             * 这里故意不处理 joinTime。
             *
             * 你的需求已经确定：
             * 不在学生端显示入部时间。
             */
            relation.setJoinTime(null);

            userDepartmentMapper.insert(relation);

            successCount++;
        }

        return buildResult(
                successCount,
                failCount,
                errors,
                "部门成员导入完成"
        );
    }

    /**
     * ================================
     * 职位是否合法
     * ================================
     */
    private boolean isValidPosition(String position) {

        return "干事".equals(position)
                || "副部长".equals(position)
                || "部长".equals(position);
    }

    /**
     * ================================
     * 构造错误信息
     * ================================
     */
    private Map<String, Object> error(
            int row,
            String message
    ) {

        Map<String, Object> map =
                new LinkedHashMap<>();

        map.put("row", row);
        map.put("message", message);

        return map;
    }

    /**
     * ================================
     * 构造返回结果
     * ================================
     */
    private Map<String, Object> buildResult(
            int successCount,
            int failCount,
            List<Map<String, Object>> errors,
            String message
    ) {

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put("message", message);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("totalCount",
                successCount + failCount);
        result.put("errors", errors);

        return result;
    }

    /**
     * ================================
     * 字符串处理
     * ================================
     */
    private String trim(String value) {

        if (value == null) {
            return null;
        }

        return value.trim();
    }

    private boolean isEmpty(String value) {

        return value == null
                || value.trim().isEmpty();
    }
}