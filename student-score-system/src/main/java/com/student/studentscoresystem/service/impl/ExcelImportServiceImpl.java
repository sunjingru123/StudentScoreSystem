package com.student.studentscoresystem.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.dto.DepartmentMemberExcelRow;
import com.student.studentscoresystem.dto.StudentExcelRow;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysDepartment;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysDepartmentMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.ExcelImportService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

@Service
public class ExcelImportServiceImpl implements ExcelImportService {

    private final SysUserMapper sysUserMapper;
    private final DepartmentMapper departmentMapper;
    private final SysDepartmentMapper sysDepartmentMapper;
    private final SysUserDepartmentMapper userDepartmentMapper;
    private final SysUserPositionMapper sysUserPositionMapper;
    private final SysPositionMapper sysPositionMapper;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public ExcelImportServiceImpl(
            SysUserMapper sysUserMapper,
            DepartmentMapper departmentMapper,
            SysDepartmentMapper sysDepartmentMapper,
            SysUserDepartmentMapper userDepartmentMapper,
            SysUserPositionMapper sysUserPositionMapper,
            SysPositionMapper sysPositionMapper
    ) {
        this.sysUserMapper = sysUserMapper;
        this.departmentMapper = departmentMapper;
        this.sysDepartmentMapper = sysDepartmentMapper;
        this.userDepartmentMapper = userDepartmentMapper;
        this.sysUserPositionMapper = sysUserPositionMapper;
        this.sysPositionMapper = sysPositionMapper;
    }

    /**
     * =========================================================
     * 导入学生名单
     * =========================================================
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importStudents(
            MultipartFile file
    ) {

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

        /*
         * =========================================================
         * 查找学生岗位
         * =========================================================
         */
        SysPosition studentPosition =
                sysPositionMapper.selectOne(
                        new LambdaQueryWrapper<SysPosition>()
                                .eq(
                                        SysPosition::getName,
                                        "学生"
                                )
                                .eq(
                                        SysPosition::getStatus,
                                        (short) 1
                                )
                                .orderByAsc(
                                        SysPosition::getId
                                )
                                .last("LIMIT 1")
                );

        if (studentPosition == null) {
            throw new RuntimeException(
                    "系统中不存在有效的“学生”岗位，请先创建学生岗位"
            );
        }

        /*
         * =========================================================
         * 找一个系统部门
         *
         * 因为 sys_user_position.department_id
         * 不能为 NULL。
         *
         * 学生普通岗位不属于具体学生组织部门，
         * 所以这里使用一个系统默认部门。
         * =========================================================
         */
        SysDepartment defaultSysDepartment =
                sysDepartmentMapper.selectOne(
                        new LambdaQueryWrapper<SysDepartment>()
                                .eq(
                                        SysDepartment::getStatus,
                                        (short) 1
                                )
                                .orderByAsc(
                                        SysDepartment::getId
                                )
                                .last("LIMIT 1")
                );

        /*
         * 如果系统里连一个 sys_department 都没有，
         * 自动创建“学生组织”系统部门。
         */
        if (defaultSysDepartment == null) {

            defaultSysDepartment =
                    new SysDepartment();

            defaultSysDepartment.setName(
                    "学生组织"
            );

            defaultSysDepartment.setDescription(
                    "系统默认学生组织部门"
            );

            defaultSysDepartment.setStatus(
                    (short) 1
            );

            defaultSysDepartment.setCreateTime(
                    LocalDateTime.now()
            );

            defaultSysDepartment.setUpdateTime(
                    LocalDateTime.now()
            );

            sysDepartmentMapper.insert(
                    defaultSysDepartment
            );
        }

        Set<String> excelStudentNos =
                new HashSet<>();

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

            LocalDateTime now =
                    LocalDateTime.now();

            SysUser user =
                    sysUserMapper.selectOne(
                            new LambdaQueryWrapper<SysUser>()
                                    .eq(
                                            SysUser::getStudentNo,
                                            studentNo
                                    )
                                    .last("LIMIT 1")
                    );

            /*
             * =====================================================
             * 学生不存在 → 新增
             * =====================================================
             */
            if (user == null) {

                user = new SysUser();

                user.setStudentNo(
                        studentNo
                );

                user.setUsername(
                        studentNo
                );

                user.setPassword(
                        passwordEncoder.encode(
                                "123456"
                        )
                );

                user.setFirstLogin(
                        (short) 1
                );

                user.setRealName(
                        realName
                );

                user.setClassName(
                        className
                );

                user.setStatus(
                        (short) 1
                );

                user.setCreateTime(
                        now
                );

                user.setUpdateTime(
                        now
                );

                int insert =
                        sysUserMapper.insert(
                                user
                        );

                if (insert <= 0) {

                    failCount++;

                    errors.add(
                            error(
                                    rowNumber,
                                    "学生创建失败：" +
                                            studentNo
                            )
                    );

                    continue;
                }

            } else {

                /*
                 * =================================================
                 * 学生已经存在 → 更新
                 * =================================================
                 */

                user.setUsername(
                        studentNo
                );

                if (isEmpty(user.getPassword())) {

                    user.setPassword(
                            passwordEncoder.encode(
                                    "123456"
                            )
                    );

                    user.setFirstLogin(
                            (short) 1
                    );
                }

                user.setRealName(
                        realName
                );

                user.setClassName(
                        className
                );

                if (user.getStatus() == null) {
                    user.setStatus(
                            (short) 1
                    );
                }

                user.setUpdateTime(
                        now
                );

                sysUserMapper.updateById(
                        user
                );
            }

            /*
             * =====================================================
             * 自动绑定学生岗位
             * =====================================================
             */
            SysUserPosition userPosition =
                    sysUserPositionMapper.selectOne(
                            new LambdaQueryWrapper<SysUserPosition>()
                                    .eq(
                                            SysUserPosition::getUserId,
                                            user.getId()
                                    )
                                    .eq(
                                            SysUserPosition::getPositionId,
                                            studentPosition.getId()
                                    )
                                    .last("LIMIT 1")
                    );

            if (userPosition == null) {

                userPosition =
                        new SysUserPosition();

                userPosition.setUserId(
                        user.getId()
                );

                userPosition.setDepartmentId(
                        defaultSysDepartment.getId()
                );

                userPosition.setPositionId(
                        studentPosition.getId()
                );

                userPosition.setCreateTime(
                        now
                );

                sysUserPositionMapper.insert(
                        userPosition
                );
            }

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

        Set<String> excelRelations =
                new HashSet<>();

        /*
         * =========================================================
         * 当前部门
         *
         * 用于支持：
         *
         * 档案部  孙靖茹
         *        姜昊月
         *        朱宪阔
         *
         * 自动继承档案部
         * =========================================================
         */
        String currentDepartmentName = null;

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

            String realName =
                    trim(row.getRealName());

            String studentNo =
                    trim(row.getStudentNo());

            String position =
                    trim(row.getPosition());

            /*
             * =====================================================
             * 1. 部门自动继承
             * =====================================================
             */
            if (!isEmpty(departmentName)) {

                currentDepartmentName =
                        departmentName;

            } else {

                departmentName =
                        currentDepartmentName;
            }

            if (isEmpty(departmentName)) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "部门不能为空，且没有可以继承的上一部门"
                        )
                );

                continue;
            }

            /*
             * =====================================================
             * 2. 学号
             * =====================================================
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

            /*
             * =====================================================
             * 3. 姓名
             * =====================================================
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

            /*
             * =====================================================
             * 4. 职位
             * =====================================================
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

            /*
             * =====================================================
             * 5. 查询岗位
             *
             * 必须在任何 INSERT 前执行。
             * =====================================================
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
                                    .orderByAsc(
                                            SysPosition::getId
                                    )
                                    .last("LIMIT 1")
                    );

            if (sysPosition == null) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "系统中不存在有效岗位：" +
                                        position +
                                        "，请先创建该岗位"
                        )
                );

                continue;
            }

            /*
             * =====================================================
             * 6. 查询 / 创建业务部门 department
             *
             * 这是这次非常重要的修改。
             *
             * Excel 里写了：
             *
             * 档案部
             *
             * 数据库没有 → 自动创建。
             * =====================================================
             */
            Department department =
                    departmentMapper.selectOne(
                            new LambdaQueryWrapper<Department>()
                                    .eq(
                                            Department::getName,
                                            departmentName
                                    )
                                    .last("LIMIT 1")
                    );

            if (department == null) {

                department =
                        new Department();

                department.setName(
                        departmentName
                );

                department.setTeacherId(
                        null
                );

                department.setStatus(
                        (short) 1
                );

                department.setCreateTime(
                        LocalDateTime.now()
                );

                int departmentInsert =
                        departmentMapper.insert(
                                department
                        );

                if (departmentInsert <= 0) {

                    failCount++;

                    errors.add(
                            error(
                                    rowNumber,
                                    "部门创建失败：" +
                                            departmentName
                            )
                    );

                    continue;
                }
            } else {

                /*
                 * 已存在但停用 → 自动启用
                 */
                if (!Short.valueOf((short) 1)
                        .equals(department.getStatus())) {

                    department.setStatus(
                            (short) 1
                    );

                    departmentMapper.updateById(
                            department
                    );
                }
            }

            /*
             * =====================================================
             * 7. 查询 / 创建 sys_department
             *
             * sys_user_position.department_id
             * 指向的不是 department.id，
             * 而是 sys_department.id。
             *
             * 所以这里必须单独处理。
             * =====================================================
             */
            SysDepartment sysDepartment =
                    sysDepartmentMapper.selectOne(
                            new LambdaQueryWrapper<SysDepartment>()
                                    .eq(
                                            SysDepartment::getName,
                                            departmentName
                                    )
                                    .last("LIMIT 1")
                    );

            if (sysDepartment == null) {

                sysDepartment =
                        new SysDepartment();

                sysDepartment.setName(
                        departmentName
                );

                sysDepartment.setDescription(
                        "由部门成员 Excel 自动创建"
                );

                sysDepartment.setStatus(
                        (short) 1
                );

                sysDepartment.setCreateTime(
                        LocalDateTime.now()
                );

                sysDepartment.setUpdateTime(
                        LocalDateTime.now()
                );

                int sysDepartmentInsert =
                        sysDepartmentMapper.insert(
                                sysDepartment
                        );

                if (sysDepartmentInsert <= 0) {

                    failCount++;

                    errors.add(
                            error(
                                    rowNumber,
                                    "系统部门创建失败：" +
                                            departmentName)
                            );

                    continue;
                }

            } else if (!Short.valueOf((short) 1)
                    .equals(sysDepartment.getStatus())) {

                sysDepartment.setStatus(
                        (short) 1
                );

                sysDepartment.setUpdateTime(
                        LocalDateTime.now()
                );

                sysDepartmentMapper.updateById(
                        sysDepartment
                );
            }

            /*
             * =====================================================
             * 8. 查询学生
             * =====================================================
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
                                "系统中不存在该学生：" +
                                        studentNo
                        )
                );

                continue;
            }

            /*
             * =====================================================
             * 9. 姓名校验
             * =====================================================
             */
            if (!realName.equals(
                    user.getRealName()
            )) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "姓名与系统不一致：学号 " +
                                        studentNo +
                                        "，系统姓名：" +
                                        user.getRealName() +
                                        "，Excel 姓名：" +
                                        realName
                        )
                );

                continue;
            }

            /*
             * =====================================================
             * 10. Excel 内部重复
             * =====================================================
             */
            String relationKey =
                    department.getId() +
                            "_" +
                            user.getId();

            if (!excelRelations.add(
                    relationKey
            )) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "Excel 中重复出现该学生的部门关系"
                        )
                );

                continue;
            }

            /*
             * =====================================================
             * 11. sys_user_department
             *
             * department_id = department.id
             *
             * 注意：
             * join_time 数据库 NOT NULL，
             * 绝对不能 setJoinTime(null)。
             * =====================================================
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

            LocalDateTime now =
                    LocalDateTime.now();

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

                /*
                 * 数据库 NOT NULL
                 */
                relation.setJoinTime(
                        now
                );

                int insert =
                        userDepartmentMapper.insert(
                                relation
                        );

                if (insert <= 0) {

                    failCount++;

                    errors.add(
                            error(
                                    rowNumber,
                                    "部门成员关系创建失败"
                            )
                    );

                    continue;
                }

            } else {

                relation.setPosition(
                        position
                );

                relation.setStatus(
                        (short) 1
                );

                /*
                 * 不修改原来的 join_time
                 */

                int update =
                        userDepartmentMapper.updateById(
                                relation
                        );

                if (update <= 0) {

                    failCount++;

                    errors.add(
                            error(
                                    rowNumber,
                                    "部门成员关系更新失败"
                            )
                    );

                    continue;
                }
            }

            /*
             * =====================================================
             * 12. sys_user_position
             *
             * department_id =
             * sys_department.id
             *
             * 不能使用 department.id！
             * =====================================================
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
                                            sysDepartment.getId()
                                    )
                                    .last("LIMIT 1")
                    );

            if (userPosition == null) {

                userPosition =
                        new SysUserPosition();

                userPosition.setUserId(
                        user.getId()
                );

                userPosition.setDepartmentId(
                        sysDepartment.getId()
                );

                userPosition.setPositionId(
                        sysPosition.getId()
                );

                userPosition.setCreateTime(
                        now
                );

                int insert =
                        sysUserPositionMapper.insert(
                                userPosition
                        );

                if (insert <= 0) {

                    failCount++;

                    errors.add(
                            error(
                                    rowNumber,
                                    "用户岗位关系创建失败"
                            )
                    );

                    continue;
                }

            } else {

                userPosition.setPositionId(
                        sysPosition.getId()
                );

                sysUserPositionMapper.updateById(
                        userPosition
                );
            }

            /*
             * =====================================================
             * 13. 成功
             * =====================================================
             */
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
     * 判断职位
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
     * 构造错误
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
     * 去除空格
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
     * 判断空字符串
     * =========================================================
     */
    private boolean isEmpty(
            String value
    ) {

        return value == null
                || value.trim().isEmpty();
    }
}