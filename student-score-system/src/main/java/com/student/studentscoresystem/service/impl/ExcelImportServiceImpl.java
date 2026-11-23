package com.student.studentscoresystem.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.dto.ClubMemberExcelRow;
import com.student.studentscoresystem.dto.DepartmentMemberExcelRow;
import com.student.studentscoresystem.dto.ScoreRuleExcelRow;
import com.student.studentscoresystem.dto.StudentExcelRow;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.entity.SysDepartment;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.ScoreRuleMapper;
import com.student.studentscoresystem.mapper.SysDepartmentMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.DepartmentMemberManageService;
import com.student.studentscoresystem.utils.ClassNameUtils;
import com.student.studentscoresystem.utils.OrgPositionLevel;
import com.student.studentscoresystem.service.DepartmentTemplateSyncService;
import com.student.studentscoresystem.service.ExcelImportService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private final ScoreRuleMapper scoreRuleMapper;

    private final DepartmentTemplateSyncService departmentTemplateSyncService;

    private final DepartmentMemberManageService departmentMemberManageService;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public ExcelImportServiceImpl(
            SysUserMapper sysUserMapper,
            DepartmentMapper departmentMapper,
            SysDepartmentMapper sysDepartmentMapper,
            SysUserDepartmentMapper userDepartmentMapper,
            SysUserPositionMapper sysUserPositionMapper,
            SysPositionMapper sysPositionMapper,
            ScoreRuleMapper scoreRuleMapper,
            DepartmentTemplateSyncService departmentTemplateSyncService,
            DepartmentMemberManageService departmentMemberManageService
    ) {
        this.sysUserMapper = sysUserMapper;
        this.departmentMapper = departmentMapper;
        this.sysDepartmentMapper = sysDepartmentMapper;
        this.userDepartmentMapper = userDepartmentMapper;
        this.sysUserPositionMapper = sysUserPositionMapper;
        this.sysPositionMapper = sysPositionMapper;
        this.scoreRuleMapper = scoreRuleMapper;
        this.departmentTemplateSyncService = departmentTemplateSyncService;
        this.departmentMemberManageService = departmentMemberManageService;
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
                    ClassNameUtils.normalize(
                            trim(row.getClassName())
                    );

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
         * 部门成员“覆盖”同步
         *
         * 记录 Excel 中出现的部门、行数以及成功导入的成员，
         * 导入结束后，把该部门内不在 Excel 名单中的在编成员软移出。
         * =========================================================
         */
        Map<String, DepartmentRoster> rosters =
                new LinkedHashMap<>();

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
             * 记录该部门在 Excel 中的行数，
             * 用于判断该部门是否存在失败行。
             */
            DepartmentRoster roster =
                    rosters.computeIfAbsent(
                            departmentName,
                            DepartmentRoster::new
                    );

            roster.incrementRowCount();

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
                                "职位不合法，只允许："
                                        + OrgPositionLevel.titlesOfOrgType(null)
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
                    findSysPositionByTitleOrLevel(
                            position
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

                /*
                 * 部门成员导入创建的是普通部门
                 */
                department.setDepartmentType(
                        OrgPositionLevel.TYPE_DEPARTMENT
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
             * 职位必须与组织类型匹配：
             * 普通部门只允许：部长、副部长、站长、副站长、干事。
             */
            Integer orgType =
                    department.getDepartmentType() == null
                            ? (int) OrgPositionLevel.TYPE_DEPARTMENT
                            : department.getDepartmentType().intValue();

            if (!OrgPositionLevel.isValidTitleForOrgType(
                    position,
                    orgType
            )) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "职位《"
                                        + position
                                        + "》不适用于该组织，只允许："
                                        + OrgPositionLevel.titlesOfOrgType(orgType)
                        )
                );

                continue;
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

                relation.setPositionLevel(
                        OrgPositionLevel.resolveLevel(position)
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

                relation.setPositionLevel(
                        OrgPositionLevel.resolveLevel(position)
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
             * 记录部门 ID，供导入结束后的覆盖同步使用。
             */
            roster.setDepartmentId(
                    department.getId()
            );

            /*
             * =====================================================
             * 13. 成功
             * =====================================================
             */
            roster.getKeptUserIds().add(
                    user.getId()
            );

            successCount++;
        }

        /*
         * =========================================================
         * 覆盖同步：
         *
         * 对 Excel 中出现、且每一行都导入成功的部门，
         * 以 Excel 名单为准，把不在名单中的在编成员移出。
         *
         * 只要该部门存在任何失败行，就跳过覆盖，
         * 避免因学号 / 姓名写错而误删成员。
         * =========================================================
         */
        int removedCount = 0;

        List<String> skippedDepartments =
                new ArrayList<>();

        for (DepartmentRoster item : rosters.values()) {

            if (item.getDepartmentId() == null) {

                continue;
            }

            if (item.getRowCount()
                    != item.getKeptUserIds().size()) {

                skippedDepartments.add(
                        item.getDepartmentName()
                );

                continue;
            }

            removedCount +=
                    departmentMemberManageService.syncRoster(
                            item.getDepartmentId(),
                            item.getKeptUserIds()
                    );
        }

        Map<String, Object> result =
                buildResult(
                        successCount,
                        failCount,
                        errors,
                        "部门成员导入完成"
                );

        result.put(
                "removedCount",
                removedCount
        );

        result.put(
                "skippedDepartments",
                skippedDepartments
        );

        return result;
    }


    /**
     * =========================================================
     * 导入社团成员
     * =========================================================
     *
     * Excel 列：
     *
     * 大社团名称 | 小社团名称 | 姓名 | 学号 | 职位
     *
     * 规则：
     *
     * 1. 大社团名称留空时继承上一行；
     * 2. 小社团名称留空表示该成员直接属于大社团
     *    （理事长 / 副理事长 / 主席 / 副主席）；
     * 3. 小社团名称不为空时自动创建（或复用）该大社团下的小社团；
     * 4. 本导入为“新增 / 更新”模式，不会移除 Excel 中未出现的成员，
     *    避免因名单不完整误删成员。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importClubMembers(
            MultipartFile file
    ) {

        List<ClubMemberExcelRow> rows;

        try {

            rows = EasyExcel
                    .read(file.getInputStream())
                    .head(ClubMemberExcelRow.class)
                    .doReadAllSync();

        } catch (Exception e) {

            throw new RuntimeException(
                    "社团成员 Excel 读取失败：" +
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

        Set<String> importedRelations =
                new HashSet<>();

        String currentBigClubName = null;

        String currentSmallClubName = null;

        /*
         * 覆盖同步上下文：
         *
         * 以 Excel 中出现的每个组织（大社团 / 小社团）为单位，
         * 记录行数与成功导入的成员；
         * 导入结束后，把该组织内不在名单中的在编成员软移出（撤掉职位）。
         */
        Map<String, DepartmentRoster> rosters =
                new LinkedHashMap<>();

        int rowNumber = 1;

        for (ClubMemberExcelRow row : rows) {

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

            String bigClubName =
                    trim(row.getBigClubName());

            String smallClubName =
                    trim(row.getSmallClubName());

            String realName =
                    trim(row.getRealName());

            String studentNo =
                    trim(row.getStudentNo());

            String position =
                    trim(row.getPosition());

            /*
             * 组织归属继承规则：
             *
             * 1. 只填大社团，小社团留空 → 该成员属于大社团本身；
             * 2. 大社团、小社团都填 → 属于该小社团；
             * 3. 大社团与小社团都留空 → 完全继承上一行；
             * 4. 只填小社团 → 大社团继承上一行。
             */
            if (!isEmpty(bigClubName)) {

                currentBigClubName =
                        bigClubName;

                if (!isEmpty(smallClubName)) {

                    currentSmallClubName =
                            smallClubName;

                } else {

                    currentSmallClubName = null;
                }

            } else {

                bigClubName =
                        currentBigClubName;

                if (!isEmpty(smallClubName)) {

                    currentSmallClubName =
                            smallClubName;

                } else {

                    smallClubName =
                            currentSmallClubName;
                }
            }

            if (isEmpty(bigClubName)) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "大社团名称不能为空，且没有可以继承的上一组织"
                        )
                );

                continue;
            }

            /*
             * 记录该组织在 Excel 中的行数。
             *
             * 只要该组织任意一行失败，就跳过它的覆盖同步，
             * 避免因学号 / 姓名写错而误移出成员。
             */
            String organizationKey =
                    isEmpty(smallClubName)
                            ? bigClubName
                            : smallClubName;

            DepartmentRoster roster =
                    rosters.computeIfAbsent(
                            organizationKey,
                            DepartmentRoster::new
                    );

            roster.incrementRowCount();

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

            /*
             * 小社团名称为空 → 成员属于大社团
             */
            boolean belongToBigClub =
                    isEmpty(smallClubName);

            int orgType =
                    belongToBigClub
                            ? OrgPositionLevel.TYPE_BIG_CLUB
                            : OrgPositionLevel.TYPE_SMALL_CLUB;

            if (!OrgPositionLevel.isValidTitleForOrgType(
                    position,
                    orgType
            )) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "职位《"
                                        + position
                                        + "》不适用于"
                                        + (belongToBigClub ? "大社团" : "小社团")
                                        + "，只允许："
                                        + OrgPositionLevel.titlesOfOrgType(orgType)
                        )
                );

                continue;
            }

            /*
             * 查询系统岗位（名称优先，回退到同层级）
             */
            SysPosition sysPosition =
                    findSysPositionByTitleOrLevel(
                            position
                    );

            if (sysPosition == null) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "系统中不存在有效岗位："
                                        + position
                                        + "，请先创建该岗位"
                        )
                );

                continue;
            }

            /*
             * 查询 / 创建大社团
             */
            Department bigClub;

            try {

                bigClub =
                        ensureOrganization(
                                bigClubName,
                                OrgPositionLevel.TYPE_BIG_CLUB,
                                null
                        );

            } catch (IllegalArgumentException e) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                e.getMessage()
                        )
                );

                continue;
            }

            /*
             * 确定成员所属组织
             */
            Department targetOrg;

            if (belongToBigClub) {

                targetOrg =
                        bigClub;

            } else {

                try {

                    targetOrg =
                            ensureOrganization(
                                    smallClubName,
                                    OrgPositionLevel.TYPE_SMALL_CLUB,
                                    bigClub.getId()
                            );

                } catch (IllegalArgumentException e) {

                    failCount++;

                    errors.add(
                            error(
                                    rowNumber,
                                    e.getMessage()
                            )
                    );

                    continue;
                }
            }

            /*
             * 组织已确定，记录到覆盖同步上下文
             */
            roster.setDepartmentId(
                    targetOrg.getId()
            );

            /*
             * 查询学生
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

            if (!realName.equals(
                    user.getRealName()
            )) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "姓名与系统不一致：学号 "
                                        + studentNo
                                        + "，系统姓名："
                                        + user.getRealName()
                                        + "，Excel 姓名："
                                        + realName
                        )
                );

                continue;
            }

            /*
             * Excel 内部重复
             */
            String relationKey =
                    targetOrg.getId() +
                            "_" +
                            user.getId();

            if (!importedRelations.add(
                    relationKey
            )) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "Excel 中重复出现该学生的组织关系"
                        )
                );

                continue;
            }

            try {

                /*
                 * 1. 系统部门（sys_user_position 使用）
                 */
                SysDepartment sysDepartment =
                        ensureSysDepartment(
                                targetOrg.getName()
                        );

                /*
                 * 2. 成员关系
                 */
                LocalDateTime now =
                        LocalDateTime.now();

                SysUserDepartment relation =
                        userDepartmentMapper.selectOne(
                                new LambdaQueryWrapper<SysUserDepartment>()
                                        .eq(
                                                SysUserDepartment::getUserId,
                                                user.getId()
                                        )
                                        .eq(
                                                SysUserDepartment::getDepartmentId,
                                                targetOrg.getId()
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
                            targetOrg.getId()
                    );

                    relation.setPosition(
                            position
                    );

                    relation.setPositionLevel(
                            OrgPositionLevel.resolveLevel(position)
                    );

                    relation.setStatus(
                            (short) 1
                    );

                    relation.setJoinTime(
                            now
                    );

                    userDepartmentMapper.insert(
                            relation
                    );

                } else {

                    relation.setPosition(
                            position
                    );

                    relation.setPositionLevel(
                            OrgPositionLevel.resolveLevel(position)
                    );

                    relation.setStatus(
                            (short) 1
                    );

                    if (relation.getJoinTime() == null) {

                        relation.setJoinTime(
                                now
                        );
                    }

                    userDepartmentMapper.updateById(
                            relation
                    );
                }

                /*
                 * 3. 系统岗位关系
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

                    sysUserPositionMapper.insert(
                            userPosition
                    );

                } else {

                    userPosition.setPositionId(
                            sysPosition.getId()
                    );

                    sysUserPositionMapper.updateById(
                            userPosition
                    );
                }

            } catch (Exception e) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "成员导入失败：" + e.getMessage()
                        )
                );

                continue;
            }

            successCount++;

            roster.getKeptUserIds().add(
                    user.getId()
            );
        }

        /*
         * =========================================================
         * 覆盖同步
         *
         * 对 Excel 中出现、且每一行都导入成功的组织，
         * 以 Excel 名单为准，把不在名单中的在编成员移出
         * （同时清理其部长 / 副部长 / 干事岗位）。
         * =========================================================
         */
        int removedCount = 0;

        List<String> skippedOrganizations =
                new ArrayList<>();

        for (DepartmentRoster item : rosters.values()) {

            if (item.getDepartmentId() == null) {

                continue;
            }

            if (item.getRowCount()
                    != item.getKeptUserIds().size()) {

                skippedOrganizations.add(
                        item.getDepartmentName()
                );

                continue;
            }

            removedCount +=
                    departmentMemberManageService.syncRoster(
                            item.getDepartmentId(),
                            item.getKeptUserIds()
                    );
        }

        Map<String, Object> result =
                buildResult(
                        successCount,
                        failCount,
                        errors,
                        "社团成员导入完成（覆盖模式：名单外的原有成员已撤掉职位）"
                );

        result.put(
                "removedCount",
                removedCount
        );

        result.put(
                "skippedDepartments",
                skippedOrganizations
        );

        return result;
    }


    /**
     * 查询 / 创建组织
     *
     * @param name     组织名称（全局唯一）
     * @param type     1 = 部门，2 = 大社团，3 = 小社团
     * @param parentId 小社团所属大社团 ID，其它类型为 null
     */
    private Department ensureOrganization(
            String name,
            short type,
            Long parentId
    ) {

        Department exist =
                departmentMapper.selectOne(
                        new LambdaQueryWrapper<Department>()
                                .eq(Department::getName, name)
                                .last("LIMIT 1")
                );

        if (exist != null) {

            Short existType =
                    exist.getDepartmentType();

            if (existType != null
                    && existType != type) {

                throw new IllegalArgumentException(
                        "已存在同名组织《"
                                + name
                                + "》，但类型不一致（当前为 "
                                + typeNameOf(existType)
                                + "），请先到组织管理中确认"
                );
            }

            if (type == OrgPositionLevel.TYPE_SMALL_CLUB
                    && parentId != null
                    && exist.getParentId() != null
                    && !parentId.equals(exist.getParentId())) {

                throw new IllegalArgumentException(
                        "小社团《"
                                + name
                                + "》已归属于其它大社团，不能同时挂在另一个大社团下"
                );
            }

            boolean changed =
                    false;

            if (existType == null) {

                exist.setDepartmentType(
                        type
                );

                changed = true;
            }

            if (type == OrgPositionLevel.TYPE_SMALL_CLUB
                    && exist.getParentId() == null
                    && parentId != null) {

                exist.setParentId(
                        parentId
                );

                changed = true;
            }

            if (!Short.valueOf((short) 1).equals(
                    exist.getStatus()
            )) {

                exist.setStatus(
                        (short) 1
                );

                changed = true;
            }

            if (changed) {

                departmentMapper.updateById(
                        exist
                );
            }

            return exist;
        }

        Department department =
                new Department();

        department.setName(
                name
        );

        department.setDepartmentType(
                type
        );

        department.setParentId(
                type == OrgPositionLevel.TYPE_SMALL_CLUB
                        ? parentId
                        : null
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

        if (departmentMapper.insert(
                department
        ) <= 0) {

            throw new IllegalArgumentException(
                    "组织创建失败：" + name
            );
        }

        return department;
    }


    /**
     * 查询 / 创建系统部门（sys_department）
     */
    private SysDepartment ensureSysDepartment(
            String name
    ) {

        SysDepartment sysDepartment =
                sysDepartmentMapper.selectOne(
                        new LambdaQueryWrapper<SysDepartment>()
                                .eq(SysDepartment::getName, name)
                                .last("LIMIT 1")
                );

        if (sysDepartment == null) {

            sysDepartment =
                    new SysDepartment();

            sysDepartment.setName(
                    name
            );

            sysDepartment.setDescription(
                    "由社团成员 Excel 自动创建"
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

            sysDepartmentMapper.insert(
                    sysDepartment
            );

            return sysDepartment;
        }

        if (!Short.valueOf((short) 1).equals(
                sysDepartment.getStatus()
        )) {

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

        return sysDepartment;
    }


    private String typeNameOf(
            Short type
    ) {

        if (type != null
                && type == OrgPositionLevel.TYPE_BIG_CLUB) {

            return "大社团";
        }

        if (type != null
                && type == OrgPositionLevel.TYPE_SMALL_CLUB) {

            return "小社团";
        }

        return "部门";
    }

    /**
     * =========================================================
     * 导入评分项目（规则）
     *
     * 部门 + 规则名称 已存在则更新，否则新增
     * =========================================================
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importScoreRules(
            MultipartFile file
    ) {

        List<ScoreRuleExcelRow> rows;

        try {

            rows = EasyExcel
                    .read(file.getInputStream())
                    .head(ScoreRuleExcelRow.class)
                    .doReadAllSync();

        } catch (Exception e) {

            throw new RuntimeException(
                    "规则 Excel 读取失败：" +
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

        /*
         * 部门名称缓存，避免每行都查一次数据库
         */
        Map<String, Department> departmentCache =
                new HashMap<>();

        int rowNumber = 1;

        for (ScoreRuleExcelRow row : rows) {

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

            String ruleName =
                    trim(row.getName());

            String scoreTypeText =
                    trim(row.getScoreType());

            String scoreText =
                    trim(row.getScore());

            String description =
                    trim(row.getDescription());

            String statusText =
                    trim(row.getStatus());

            /*
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

            Department department =
                    getImportDepartment(
                            departmentName,
                            departmentCache
                    );

            /*
             * 规则必须挂在系统已有的部门上，
             * 这里不做自动创建，避免写错部门名称时产生脏数据。
             */
            if (department == null) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "系统中不存在有效部门：" +
                                        departmentName
                        )
                );

                continue;
            }

            /*
             * =================================================
             * 2. 规则名称
             * =================================================
             */
            if (isEmpty(ruleName)) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "规则名称不能为空"
                        )
                );

                continue;
            }

            if (ruleName.length() > 100) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "规则名称不能超过 100 个字符"
                        )
                );

                continue;
            }

            /*
             * =================================================
             * 3. 加分 / 减分类型
             * =================================================
             */
            Short scoreType =
                    parseRuleScoreType(
                            scoreTypeText
                    );

            if (scoreType == null) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "类型只能填写：加分、减分"
                        )
                );

                continue;
            }

            /*
             * =================================================
             * 4. 分值
             * =================================================
             */
            if (isEmpty(scoreText)) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "分值不能为空"
                        )
                );

                continue;
            }

            BigDecimal score =
                    parseScore(
                            scoreText
                    );

            if (score == null) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "分值格式不正确：" +
                                        scoreText
                        )
                );

                continue;
            }

            if (score.compareTo(
                    BigDecimal.ZERO
            ) <= 0) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "分值必须大于 0"
                        )
                );

                continue;
            }

            /*
             * =================================================
             * 5. 状态
             * =================================================
             */
            Short status =
                    parseRuleStatus(
                            statusText
                    );

            if (status == null) {

                failCount++;

                errors.add(
                        error(
                                rowNumber,
                                "状态只能填写：启用、停用"
                        )
                );

                continue;
            }

            /*
             * =================================================
             * 6. 新增 / 更新
             *
             * 规则身份是 部门 + 名称 + 加分/减分类型 + 分值：
             * 只有四者全部一致才视为同一条规则并更新，
             * 同名但类型/分值不同的行会新增为独立规则。
             * =================================================
             */
            ScoreRule exist =
                    scoreRuleMapper.selectOne(
                            new LambdaQueryWrapper<ScoreRule>()
                                    .eq(
                                            ScoreRule::getDepartmentId,
                                            department.getId()
                                    )
                                    .eq(
                                            ScoreRule::getName,
                                            ruleName
                                    )
                                    .eq(
                                            ScoreRule::getScoreType,
                                            scoreType
                                    )
                                    .eq(
                                            ScoreRule::getScore,
                                            score
                                    )
                                    .last("LIMIT 1")
                    );

            LocalDateTime now =
                    LocalDateTime.now();

            if (exist == null) {

                ScoreRule scoreRule =
                        new ScoreRule();

                scoreRule.setDepartmentId(
                        department.getId()
                );

                scoreRule.setName(
                        ruleName
                );

                scoreRule.setScoreType(
                        scoreType
                );

                scoreRule.setScore(
                        score
                );

                scoreRule.setDescription(
                        emptyToNull(description)
                );

                scoreRule.setStatus(
                        status
                );

                scoreRule.setCreateTime(
                        now
                );

                scoreRule.setUpdateTime(
                        now
                );

                int insert =
                        scoreRuleMapper.insert(
                                scoreRule
                        );

                if (insert <= 0) {

                    failCount++;

                    errors.add(
                            error(
                                    rowNumber,
                                    "规则新增失败：" +
                                            ruleName
                            )
                    );

                    continue;
                }

                /*
                 * 同步到该部门的加减分模板，
                 * 学生端申报下拉即可选到。
                 */
                departmentTemplateSyncService.sync(
                        scoreRule
                );

            } else {

                exist.setScoreType(
                        scoreType
                );

                exist.setScore(
                        score
                );

                exist.setDescription(
                        emptyToNull(description)
                );

                exist.setStatus(
                        status
                );

                exist.setUpdateTime(
                        now
                );

                int update =
                        scoreRuleMapper.updateById(
                                exist
                        );

                if (update <= 0) {

                    failCount++;

                    errors.add(
                            error(
                                    rowNumber,
                                    "规则更新失败：" +
                                            ruleName
                            )
                    );

                    continue;
                }

                /*
                 * 同步到该部门的加减分模板，
                 * 学生端申报下拉即可选到。
                 */
                departmentTemplateSyncService.sync(
                        exist
                );
            }

            successCount++;
        }

        return buildResult(
                successCount,
                failCount,
                errors,
                "规则导入完成"
        );
    }

    /**
     * =========================================================
     * 学生名单导入模板
     * =========================================================
     *
     * 表头与 {@link StudentExcelRow} 的 @ExcelProperty 完全一致，
     * 用户直接填写即可导入。
     */
    @Override
    public byte[] buildStudentTemplateWorkbook() {

        return buildTemplateWorkbook(
                new String[]{"姓名", "班级", "学号"},
                new int[]{16, 20, 20}
        );
    }


    /**
     * =========================================================
     * 部门成员名单导入模板
     * =========================================================
     *
     * 表头与 {@link DepartmentMemberExcelRow} 的 @ExcelProperty 完全一致。
     */
    @Override
    public byte[] buildDepartmentMemberTemplateWorkbook() {

        return buildTemplateWorkbook(
                new String[]{"部门", "姓名", "学号", "职位"},
                new int[]{20, 16, 20, 14}
        );
    }


    /**
     * =========================================================
     * 社团成员名单导入模板
     * =========================================================
     *
     * 表头与 {@link ClubMemberExcelRow} 的 @ExcelProperty 完全一致。
     */
    @Override
    public byte[] buildClubMemberTemplateWorkbook() {

        return buildTemplateWorkbook(
                new String[]{"大社团名称", "小社团名称", "姓名", "学号", "职位"},
                new int[]{22, 22, 16, 20, 14}
        );
    }


    /**
     * =========================================================
     * 生成只有表头的导入模板
     * =========================================================
     *
     * 不写示例数据行：避免用户忘记删除示例行，
     * 把“张三”之类的示例当成真实数据导进系统。
     */
    private byte[] buildTemplateWorkbook(
            String[] headers,
            int[] widths
    ) {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out =
                     new ByteArrayOutputStream()) {

            XSSFSheet sheet =
                    workbook.createSheet("导入数据");

            CellStyle headerStyle =
                    workbook.createCellStyle();

            Font headerFont =
                    workbook.createFont();

            headerFont.setBold(true);

            headerStyle.setFont(headerFont);


            Row header =
                    sheet.createRow(0);


            for (int i = 0; i < headers.length; i++) {

                Cell cell =
                        header.createCell(i);

                cell.setCellValue(headers[i]);

                cell.setCellStyle(headerStyle);
            }


            if (widths != null) {

                for (
                        int i = 0;
                        i < widths.length
                                && i < headers.length;
                        i++) {

                    sheet.setColumnWidth(
                            i,
                            widths[i] * 256
                    );
                }
            }


            sheet.createFreezePane(0, 1);


            workbook.write(out);

            return out.toByteArray();

        } catch (IOException e) {

            throw new IllegalStateException(
                    "生成导入模板失败：" + e.getMessage(),
                    e
            );
        }
    }


    /**
     * =========================================================
     * 带缓存的部门查询
     * =========================================================
     */
    private Department getImportDepartment(
            String departmentName,
            Map<String, Department> departmentCache
    ) {

        if (departmentCache.containsKey(
                departmentName
        )) {

            return departmentCache.get(
                    departmentName
            );
        }

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

        departmentCache.put(
                departmentName,
                department
        );

        return department;
    }

    /**
     * =========================================================
     * 解析分值
     *
     * 解析失败返回 null
     * =========================================================
     */
    private BigDecimal parseScore(
            String scoreText
    ) {

        try {

            return new BigDecimal(
                    scoreText
            ).setScale(
                    2,
                    RoundingMode.HALF_UP
            );

        } catch (Exception e) {

            return null;
        }
    }

    /**
     * =========================================================
     * 解析加分 / 减分类型
     *
     * 加分 -> 1
     * 减分 -> -1
     * 无法识别返回 null
     * =========================================================
     */
    private Short parseRuleScoreType(
            String typeText
    ) {

        if (isEmpty(typeText)) {

            return null;
        }

        if ("加分".equals(typeText)
                || "1".equals(typeText)) {

            return (short) 1;
        }

        if ("减分".equals(typeText)
                || "-1".equals(typeText)) {

            return (short) -1;
        }

        return null;
    }

    /**
     * =========================================================
     * 解析规则状态
     *
     * 留空默认启用，无法识别返回 null
     * =========================================================
     */
    private Short parseRuleStatus(
            String statusText
    ) {

        if (isEmpty(statusText)) {

            return (short) 1;
        }

        if ("启用".equals(statusText)
                || "1".equals(statusText)
                || "是".equals(statusText)) {

            return (short) 1;
        }

        if ("停用".equals(statusText)
                || "0".equals(statusText)
                || "否".equals(statusText)) {

            return (short) 0;
        }

        return null;
    }

    /**
     * =========================================================
     * 空字符串转 null
     * =========================================================
     */
    private String emptyToNull(
            String value
    ) {

        return isEmpty(value)
                ? null
                : value;
    }

    /**
     * 按职位名称查系统岗位；
     * 名称不存在时回退到同层级岗位，保证权限不丢失。
     */
    private SysPosition findSysPositionByTitleOrLevel(
            String title
    ) {

        SysPosition byName =
                sysPositionMapper.selectOne(
                        new LambdaQueryWrapper<SysPosition>()
                                .eq(SysPosition::getName, title)
                                .eq(SysPosition::getStatus, (short) 1)
                                .orderByAsc(SysPosition::getId)
                                .last("LIMIT 1")
                );

        if (byName != null) {

            return byName;
        }

        String level =
                OrgPositionLevel.resolveLevel(title);

        if (level == null) {

            return null;
        }

        return sysPositionMapper.selectOne(
                new LambdaQueryWrapper<SysPosition>()
                        .eq(SysPosition::getLevel, level)
                        .eq(SysPosition::getStatus, (short) 1)
                        .orderByAsc(SysPosition::getId)
                        .last("LIMIT 1")
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

        return OrgPositionLevel.isValidTitle(
                position
        );
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

    /**
     * =========================================================
     * 部门成员覆盖同步的临时上下文
     * =========================================================
     */
    private static final class DepartmentRoster {

        private final String departmentName;

        private Long departmentId;

        private int rowCount;

        private final Set<Long> keptUserIds =
                new LinkedHashSet<>();

        private DepartmentRoster(
                String departmentName
        ) {

            this.departmentName =
                    departmentName;
        }

        private String getDepartmentName() {

            return departmentName;
        }

        private Long getDepartmentId() {

            return departmentId;
        }

        private void setDepartmentId(
                Long departmentId
        ) {

            this.departmentId =
                    departmentId;
        }

        private int getRowCount() {

            return rowCount;
        }

        private void incrementRowCount() {

            this.rowCount++;
        }

        private Set<Long> getKeptUserIds() {

            return keptUserIds;
        }
    }
}
