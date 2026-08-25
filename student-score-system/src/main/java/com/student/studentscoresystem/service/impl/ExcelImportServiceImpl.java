package com.student.studentscoresystem.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.dto.DepartmentMemberExcelRow;
import com.student.studentscoresystem.dto.StudentExcelRow;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.ExcelImportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Excel 批量导入服务
 *
 * 学生 Excel：
 * 学号 | 姓名 | 班级
 *
 * 部门成员 Excel：
 * 部门 | 学号 | 姓名 | 职位
 */
@Service
public class ExcelImportServiceImpl implements ExcelImportService {

    private final SysUserMapper sysUserMapper;
    private final DepartmentMapper departmentMapper;
    private final SysUserDepartmentMapper userDepartmentMapper;
    private final SysUserPositionMapper sysUserPositionMapper;
    private final SysPositionMapper sysPositionMapper;

    public ExcelImportServiceImpl(
            SysUserMapper sysUserMapper,
            DepartmentMapper departmentMapper,
            SysUserDepartmentMapper userDepartmentMapper,
            SysUserPositionMapper sysUserPositionMapper,
            SysPositionMapper sysPositionMapper
    ) {
        this.sysUserMapper = sysUserMapper;
        this.departmentMapper = departmentMapper;
        this.userDepartmentMapper = userDepartmentMapper;
        this.sysUserPositionMapper = sysUserPositionMapper;
        this.sysPositionMapper = sysPositionMapper;
    }

    /**
     * =========================================================
     * 导入学生名单
     * =========================================================
     *
     * 重要：
     *
     * 一个 Excel 里面可以有多个 Sheet：
     *
     * 2025级1班
     * 2025级2班
     * 2025级3班
     * 2025级4班
     * ...
     *
     * 使用 doReadAllSync()
     * 而不是 sheet().doReadSync()
     *
     * 否则只会读取第一张 Sheet。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importStudents(MultipartFile file) {

        List<StudentExcelRow> rows;

        try {

            rows = EasyExcel
                    .read(file.getInputStream())
                    .head(StudentExcelRow.class)
                    .doReadAllSync();

        } catch (Exception e) {

            throw new RuntimeException(
                    "学生 Excel 读取失败：" + e.getMessage(),
                    e
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
                    "Excel 中没有数据"
            );
        }

        /**
         * 防止整个 Excel 中出现重复学号
         *
         * 因为现在是多 Sheet，
         * 所以这个 Set 必须放在所有 Sheet 数据的外面。
         */
        Set<String> excelStudentNos =
                new HashSet<>();

        /**
         * EasyExcel 读取多 Sheet 后，
         * 这里只能保证数据行编号顺序。
         *
         * 第一行通常是表头，所以从 2 开始。
         */
        int rowNumber = 1;

        for (StudentExcelRow row : rows) {

            rowNumber++;

            if (row == null) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "Excel 当前行数据为空"
                        )
                );

                continue;
            }

            String studentNo =
                    trim(row.getStudentNo());

            String realName =
                    trim(row.getRealName());

            String className =
                    trim(row.getClassName());

            /**
             * =================================================
             * 1. 学号
             * =================================================
             */
            if (isEmpty(studentNo)) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "学号不能为空"
                        )
                );

                continue;
            }

            /**
             * =================================================
             * 2. 姓名
             * =================================================
             */
            if (isEmpty(realName)) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "姓名不能为空"
                        )
                );

                continue;
            }

            /**
             * =================================================
             * 3. 班级
             * =================================================
             */
            if (isEmpty(className)) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "班级不能为空"
                        )
                );

                continue;
            }

            /**
             * =================================================
             * 4. Excel 内部重复学号
             * =================================================
             */
            if (!excelStudentNos.add(studentNo)) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "Excel 中存在重复学号：" +
                                        studentNo
                        )
                );

                continue;
            }

            /**
             * =================================================
             * 5. 查询数据库
             * =================================================
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

            LocalDateTime now =
                    LocalDateTime.now();

            /**
             * =================================================
             * 6. 学生不存在 → 新增
             * =================================================
             */
            if (user == null) {

                user = new SysUser();

                /**
                 * 学号
                 */
                user.setStudentNo(studentNo);

                /**
                 * 登录账号 = 学号
                 */
                user.setUsername(studentNo);

                /**
                 * 默认密码 = 学号
                 *
                 * 例如：
                 *
                 * 账号：2025404558
                 * 密码：2025404558
                 */
                user.setPassword(studentNo);

                /**
                 * 姓名
                 */
                user.setRealName(realName);

                /**
                 * 班级
                 */
                user.setClassName(className);

                /**
                 * 默认启用
                 */
                user.setStatus((short) 1);

                /**
                 * 时间
                 */
                user.setCreateTime(now);
                user.setUpdateTime(now);

                /**
                 * 插入数据库
                 */
                sysUserMapper.insert(user);

                successCount++;

                continue;
            }

            /**
             * =================================================
             * 7. 学生已经存在 → 更新
             * =================================================
             */

            /**
             * 保证用户名不是 null
             */
            user.setUsername(studentNo);

            /**
             * 如果密码为空，
             * 自动使用学号作为密码。
             */
            if (isEmpty(user.getPassword())) {

                user.setPassword(studentNo);
            }

            /**
             * 更新姓名
             */
            user.setRealName(realName);

            /**
             * 更新班级
             */
            user.setClassName(className);

            /**
             * 如果状态为空，
             * 默认启用。
             */
            if (user.getStatus() == null) {

                user.setStatus((short) 1);
            }

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
     * =========================================================
     * 导入部门成员
     * =========================================================
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

            /**
             * 同样使用 doReadAllSync()
             *
             * 防止部门成员 Excel 有多个 Sheet
             */
            rows = EasyExcel
                    .read(file.getInputStream())
                    .head(DepartmentMemberExcelRow.class)
                    .doReadAllSync();

        } catch (Exception e) {

            throw new RuntimeException(
                    "部门成员 Excel 读取失败：" +
                            e.getMessage(),
                    e
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
                    "Excel 中没有数据"
            );
        }

        /**
         * Excel 内部防重复：
         *
         * 一个学生
         * +
         * 一个部门
         *
         * 只能出现一次。
         */
        Set<String> excelRelations =
                new HashSet<>();

        int rowNumber = 1;

        for (DepartmentMemberExcelRow row : rows) {

            rowNumber++;

            if (row == null) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "Excel 当前行数据为空"
                        )
                );

                continue;
            }

            String departmentName =
                    trim(row.getDepartmentName());

            String studentNo =
                    trim(row.getStudentNo());

            String realName =
                    trim(row.getRealName());

            String position =
                    trim(row.getPosition());

            /**
             * =================================================
             * 1. 部门
             * =================================================
             */
            if (isEmpty(departmentName)) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "部门不能为空"
                        )
                );

                continue;
            }

            /**
             * =================================================
             * 2. 学号
             * =================================================
             */
            if (isEmpty(studentNo)) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "学号不能为空"
                        )
                );

                continue;
            }

            /**
             * =================================================
             * 3. 姓名
             * =================================================
             */
            if (isEmpty(realName)) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "姓名不能为空"
                        )
                );

                continue;
            }

            /**
             * =================================================
             * 4. 职位
             * =================================================
             */
            if (isEmpty(position)) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "职位不能为空"
                        )
                );

                continue;
            }

            /**
             * =================================================
             * 5. 职位合法性
             * =================================================
             */
            if (!isValidPosition(position)) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "职位不合法，只允许：干事、副部长、部长"
                        )
                );

                continue;
            }

            /**
             * =================================================
             * 6. 查询部门
             * =================================================
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

                errors.add(
                        error(
                                rowNumber,
                                "部门不存在或已停用：" +
                                        departmentName
                        )
                );

                continue;
            }

            /**
             * =================================================
             * 7. 查询学生
             * =================================================
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

                errors.add(
                        error(
                                rowNumber,
                                "学生不存在，无法建立部门关系，学号：" +
                                        studentNo
                        )
                );

                continue;
            }

            /**
             * =================================================
             * 8. 姓名校验
             * =================================================
             */
            if (!realName.equals(user.getRealName())) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "姓名与系统不一致：学号 " +
                                        studentNo +
                                        "，系统姓名为：" +
                                        user.getRealName()
                        )
                );

                continue;
            }

            /**
             * =================================================
             * 9. Excel 内部重复
             * =================================================
             */
            String relationKey =
                    department.getId() +
                            "_" +
                            user.getId();

            if (!excelRelations.add(relationKey)) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "Excel 中重复导入该部门成员"
                        )
                );

                continue;
            }

            /**
             * =================================================
             * 10. 创建 / 更新 sys_user_department
             * =================================================
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

            if (relation == null) {

                relation =
                        new SysUserDepartment();

                relation.setUserId(
                        user.getId()
                );

                relation.setDepartmentId(
                        department.getId()
                );

                relation.setPosition(
                        position
                );

                relation.setStatus(
                        (short) 1
                );

                /**
                 * 按你的要求：
                 * 不设置加入部门时间
                 */
                relation.setJoinTime(null);

                userDepartmentMapper.insert(
                        relation
                );

            } else {

                relation.setPosition(
                        position
                );

                relation.setStatus(
                        (short) 1
                );

                relation.setJoinTime(null);

                userDepartmentMapper.updateById(
                        relation
                );
            }

            /**
             * =================================================
             * 11. 查询系统岗位
             * =================================================
             */
            SysPosition sysPosition =
                    sysPositionMapper.selectOne(
                            new LambdaQueryWrapper<SysPosition>()
                                    .eq(
                                            SysPosition::getName,
                                            position
                                    )
                                    .eq(
                                            SysPosition::getStatus,
                                            (short) 1
                                    )
                                    .last("LIMIT 1")
                    );

            if (sysPosition == null) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "系统岗位不存在或已停用：" +
                                        position
                        )
                );

                continue;
            }

            /**
             * =================================================
             * 12. 创建 / 更新 sys_user_position
             * =================================================
             *
             * 重点：
             *
             * department_id
             * 必须明确设置。
             *
             * 这里绝对不能：
             *
             * new SysUserPosition()
             * 然后直接 insert
             *
             * 否则 department_id 就是 null。
             */
            SysUserPosition userPosition =
                    sysUserPositionMapper.selectOne(
                            new LambdaQueryWrapper<SysUserPosition>()
                                    .eq(
                                            SysUserPosition::getUserId,
                                            user.getId()
                                    )
                                    .eq(
                                            SysUserPosition::getDepartmentId,
                                            department.getId()
                                    )
                                    .last("LIMIT 1")
                    );

            /**
             * =================================================
             * 13. 不存在 → 新增
             * =================================================
             */
            if (userPosition == null) {

                userPosition =
                        new SysUserPosition();

                userPosition.setUserId(
                        user.getId()
                );

                /**
                 * 重点！
                 *
                 * 这里必须设置 departmentId
                 */
                userPosition.setDepartmentId(
                        department.getId()
                );

                /**
                 * 设置岗位
                 */
                userPosition.setPositionId(
                        sysPosition.getId()
                );

                userPosition.setCreateTime(
                        LocalDateTime.now()
                );

                sysUserPositionMapper.insert(
                        userPosition
                );

            } else {

                /**
                 * =================================================
                 * 14. 已存在 → 更新岗位
                 * =================================================
                 */

                /**
                 * 即使数据库已有记录，
                 * 这里也再次确保 departmentId 不为空。
                 */
                userPosition.setDepartmentId(
                        department.getId()
                );

                userPosition.setPositionId(
                        sysPosition.getId()
                );

                sysUserPositionMapper.updateById(
                        userPosition
                );
            }

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
     * =========================================================
     * 判断职位是否合法
     * =========================================================
     */
    private boolean isValidPosition(
            String position
    ) {

        return "干事".equals(position)
                || "副部长".equals(position)
                || "部长".equals(position);
    }

    /**
     * =========================================================
     * 构造错误信息
     * =========================================================
     */
    private Map<String, Object> error(
            int row,
            String message
    ) {

        Map<String, Object> map =
                new LinkedHashMap<>();

        map.put(
                "row",
                row
        );

        map.put(
                "message",
                message
        );

        return map;
    }

    /**
     * =========================================================
     * 构造返回结果
     * =========================================================
     */
    private Map<String, Object> buildResult(
            int successCount,
            int failCount,
            List<Map<String, Object>> errors,
            String message
    ) {

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put(
                "message",
                message
        );

        result.put(
                "successCount",
                successCount
        );

        result.put(
                "failCount",
                failCount
        );

        result.put(
                "totalCount",
                successCount + failCount
        );

        result.put(
                "errors",
                errors
        );

        return result;
    }

    /**
     * =========================================================
     * 去除字符串两端空格
     * =========================================================
     */
    private String trim(
            String value
    ) {

        if (value == null) {
            return null;
        }

        return value.trim();
    }

    /**
     * =========================================================
     * 判断字符串为空
     * =========================================================
     */
    private boolean isEmpty(
            String value
    ) {

        return value == null
                || value.trim().isEmpty();
    }
}