package com.student.studentscoresystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * =========================================================
 * 困难学生名单导入权限判定
 * =========================================================
 *
 * 谁可以导入困难学生名单：
 *
 * 1. 系统管理员（运维需要）；
 *
 * 2. 「勤工助学部」的部长 / 副部长
 *      sys_user_department.status = 1
 *      且 position 为 部长 / 副部长
 *      且所属部门为「勤工助学部」并被启用。
 *
 * 3. 「档案部」的部长
 *      sys_user_department.status = 1
 *      且 position 为 部长
 *      且所属部门为「档案部」并被启用。
 *
 * 说明：
 *
 * 1. 不新增角色、不修改现有角色体系；
 * 2. 完全复用户籍 / 部门既有数据模型
 *    （sys_user_department + department），
 *    与档案部判定（ScoreManagerPermissionService.isArchiveLeader）保持一致；
 * 3. 干事不具备该权限；
 * 4. 前端只依据本判定控制按钮显示，
 *    真正的拦截始终由后端接口完成。
 * =========================================================
 */
@Component
public class PovertyImportPermissionService {

    /**
     * 勤工助学部名称
     */
    private static final String WORK_STUDY_DEPARTMENT_NAME =
            "勤工助学部";

    /**
     * 档案部名称
     */
    private static final String ARCHIVE_DEPARTMENT_NAME =
            "档案部";

    /**
     * 部门 -> 可导入困难学生名单的岗位
     *
     * 勤工助学部：部长 / 副部长
     * 档案部    ：部长
     *
     * 后续如需调整，只改这张映射表，不动其它判定逻辑。
     */
    private static final Map<String, List<String>> IMPORT_POSITIONS_BY_DEPARTMENT =
            Map.of(
                    WORK_STUDY_DEPARTMENT_NAME,
                    List.of("部长", "副部长"),
                    ARCHIVE_DEPARTMENT_NAME,
                    List.of("部长")
            );

    /**
     * 可导入困难学生名单的岗位（各平台并集，用于 SQL 预过滤）
     */
    private static final List<String> IMPORT_POSITIONS =
            IMPORT_POSITIONS_BY_DEPARTMENT
                    .values()
                    .stream()
                    .flatMap(List::stream)
                    .distinct()
                    .toList();


    private final ScoreManagerPermissionService scoreManagerPermissionService;

    private final SysUserDepartmentMapper userDepartmentMapper;

    private final DepartmentMapper departmentMapper;


    public PovertyImportPermissionService(
            ScoreManagerPermissionService scoreManagerPermissionService,
            SysUserDepartmentMapper userDepartmentMapper,
            DepartmentMapper departmentMapper
    ) {

        this.scoreManagerPermissionService =
                scoreManagerPermissionService;

        this.userDepartmentMapper =
                userDepartmentMapper;

        this.departmentMapper =
                departmentMapper;
    }

    /**
     * 当前用户是否可以导入困难学生名单
     */
    public boolean canImportPovertyList(Long userId) {

        if (userId == null) {

            return false;
        }


        if (scoreManagerPermissionService.isAdmin(userId)) {

            return true;
        }


        List<SysUserDepartment> relations =
                userDepartmentMapper.selectList(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getUserId,
                                        userId
                                )
                                .eq(
                                        SysUserDepartment::getStatus,
                                        (short) 1
                                )
                                .in(
                                        SysUserDepartment::getPosition,
                                        IMPORT_POSITIONS
                                )
                );


        if (relations == null || relations.isEmpty()) {

            return false;
        }


        for (SysUserDepartment relation : relations) {

            if (relation == null || relation.getDepartmentId() == null) {

                continue;
            }


            String position =
                    relation.getPosition();


            if (position == null) {

                continue;
            }


            /*
             * 岗位二次校验：
             * 避免调用方传入异常数据时误放行。
             */
            if (!IMPORT_POSITIONS.contains(position.trim())) {

                continue;
            }


            Department department =
                    departmentMapper.selectById(
                            relation.getDepartmentId()
                    );


            if (department == null || department.getName() == null) {

                continue;
            }


            /*
             * 部门必须启用
             */
            if (!Short.valueOf((short) 1).equals(department.getStatus())) {

                continue;
            }


            /*
             * 按部门取可导入岗位：
             *
             * 勤工助学部：部长 / 副部长
             * 档案部    ：部长
             */
            List<String> allowedPositions =
                    IMPORT_POSITIONS_BY_DEPARTMENT.get(
                            department.getName().trim()
                    );


            if (allowedPositions == null) {

                continue;
            }


            if (allowedPositions.contains(position.trim())) {

                return true;
            }
        }


        return false;
    }

}
