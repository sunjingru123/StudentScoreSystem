package com.student.studentscoresystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUserDepartment;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserDepartmentMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * =========================================================
 * 学生成绩维护权限判定
 * =========================================================
 *
 * 谁可以查看全部学生成绩、删除学生身上的加减分：
 *
 * 1. 系统管理员
 *      岗位 sys_position.name = 管理员
 *
 * 2. 档案部部长 / 副部长
 *      sys_user_department.status = 1
 *      且 position 为 部长 / 副部长
 *      且所属部门为「档案部」并被启用
 *
 * 说明：
 *
 * 1. 这里不新增任何角色，完全复用户籍 / 档案部既有数据模型
 *    （sys_user_position + sys_user_department + department）。
 * 2. 只用于成绩维护类接口，不改变登录鉴权与现有角色体系。
 * 3. 组合权限（管理员 或 档案部正副部长）无法用 @RequireRole 表达，
 *    因此调用方需要在方法内使用本服务进行判定。
 * =========================================================
 */
@Component
public class ScoreManagerPermissionService {

    /**
     * 管理员岗位名称
     */
    private static final String ADMIN_POSITION_NAME = "管理员";

    /**
     * 档案部名称
     */
    private static final String ARCHIVE_DEPARTMENT_NAME = "档案部";

    /**
     * 档案部负责人岗位
     */
    private static final List<String> ARCHIVE_LEADER_POSITIONS =
            List.of("部长", "副部长");

    /**
     * 高级辅导员岗位名称
     */
    private static final String SENIOR_COUNSELOR_POSITION_NAME =
            "高级辅导员";


    private final SysUserPositionMapper userPositionMapper;

    private final SysPositionMapper positionMapper;

    private final SysUserDepartmentMapper userDepartmentMapper;

    private final DepartmentMapper departmentMapper;


    public ScoreManagerPermissionService(
            SysUserPositionMapper userPositionMapper,
            SysPositionMapper positionMapper,
            SysUserDepartmentMapper userDepartmentMapper,
            DepartmentMapper departmentMapper
    ) {

        this.userPositionMapper = userPositionMapper;

        this.positionMapper = positionMapper;

        this.userDepartmentMapper = userDepartmentMapper;

        this.departmentMapper = departmentMapper;

    }


    /**
     * 是否为系统管理员
     */
    public boolean isAdmin(Long userId) {

        if (userId == null) {

            return false;
        }


        List<SysUserPosition> relations =
                userPositionMapper.selectList(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .eq(
                                        SysUserPosition::getUserId,
                                        userId
                                )
                );


        if (relations == null || relations.isEmpty()) {

            return false;
        }


        for (SysUserPosition relation : relations) {

            if (relation == null || relation.getPositionId() == null) {

                continue;
            }


            SysPosition position =
                    positionMapper.selectById(
                            relation.getPositionId()
                    );


            if (position == null || position.getName() == null) {

                continue;
            }


            /*
             * 岗位必须启用
             */
            if (position.getStatus() != null && position.getStatus() != 1) {

                continue;
            }


            if (ADMIN_POSITION_NAME.equals(position.getName())) {

                return true;
            }
        }


        return false;
    }


    /**
     * 是否为档案部部长 / 副部长
     */
    public boolean isArchiveLeader(Long userId) {

        if (userId == null) {

            return false;
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
                                        ARCHIVE_LEADER_POSITIONS
                                )
                );


        if (relations == null || relations.isEmpty()) {

            return false;
        }


        for (SysUserDepartment relation : relations) {

            if (relation == null || relation.getDepartmentId() == null) {

                continue;
            }


            /*
             * 岗位二次校验：
             *
             * SQL 中已经按 部长 / 副部长 过滤，
             * 这里再校验一次，避免调用方传入异常数据时误放行。
             */
            if (!ARCHIVE_LEADER_POSITIONS.contains(relation.getPosition())) {

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


            if (ARCHIVE_DEPARTMENT_NAME.equals(department.getName())) {

                return true;
            }
        }


        return false;
    }


    /**
     * 是否可以维护全部学生成绩
     *
     * = 管理员 或 档案部部长 / 副部长
     */
    public boolean canManageStudentScores(Long userId) {

        if (userId == null) {

            return false;
        }


        /*
         * 管理员先判定，
         * 命中后不再查询部门关系。
         */
        if (isAdmin(userId)) {

            return true;
        }


        if (isArchiveLeader(userId)) {

            return true;
        }


        /*
         * 高级辅导员 / 档案部负责辅导员同样需要查看学生成绩，
         * 以便审核档案部提交的成绩删除申请。
         */
        if (isSeniorCounselor(userId)) {

            return true;
        }


        return isArchiveDepartmentCounselor(userId);
    }


    /**
     * =====================================================
     * 是否为「高级辅导员」
     * =====================================================
     *
     * 高级辅导员是辅导员端的一个岗位：
     *   1. 可以导出全年级 / 各部门 / 各班的加减分汇总；
     *   2. 可以审核档案部成员提交的成绩删除申请，
     *      并直接执行删除。
     *
     * 判定依据完全复用现有 sys_user_position + sys_position，
     * 不新增任何角色表或字段。
     */
    public boolean isSeniorCounselor(Long userId) {

        if (userId == null) {

            return false;
        }


        List<SysUserPosition> relations =
                userPositionMapper.selectList(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .eq(
                                        SysUserPosition::getUserId,
                                        userId
                                )
                );


        if (relations == null || relations.isEmpty()) {

            return false;
        }


        for (SysUserPosition relation : relations) {

            if (relation == null || relation.getPositionId() == null) {

                continue;
            }


            SysPosition position =
                    positionMapper.selectById(
                            relation.getPositionId()
                    );


            if (position == null || position.getName() == null) {

                continue;
            }


            if (position.getStatus() != null && position.getStatus() != 1) {

                continue;
            }


            if (SENIOR_COUNSELOR_POSITION_NAME.equals(
                    position.getName().trim()
            )) {

                return true;
            }
        }


        return false;
    }


    /**
     * =====================================================
     * 是否为「档案部负责辅导员」
     * =====================================================
     *
     * 判定依据：department.teacher_id = 当前用户，
     * 且部门名称为「档案部」且已启用。
     *
     * 与「高级辅导员」一起构成成绩删除申请的审核人集合，
     * 这些审核人同时可以直接删除学生成绩。
     */
    public boolean isArchiveDepartmentCounselor(Long userId) {

        if (userId == null) {

            return false;
        }


        List<Department> departments =
                departmentMapper.selectList(
                        new LambdaQueryWrapper<Department>()
                                .eq(
                                        Department::getTeacherId,
                                        userId
                                )
                                .eq(
                                        Department::getName,
                                        ARCHIVE_DEPARTMENT_NAME
                                )
                );


        if (departments == null || departments.isEmpty()) {

            return false;
        }


        for (Department department : departments) {

            if (department == null) {

                continue;
            }


            if (Short.valueOf((short) 1).equals(department.getStatus())) {

                return true;
            }
        }


        return false;
    }


    /**
     * =====================================================
     * 是否可以直接删除学生成绩（无需审核）
     * =====================================================
     *
     * 管理员 / 高级辅导员 / 档案部负责辅导员。
     *
     * 档案部部长 / 副部长不再直接删除，
     * 而是通过「提交删除申请 -> 审核通过」流程执行。
     */
    public boolean canDeleteScoreDirectly(Long userId) {

        if (userId == null) {

            return false;
        }


        return isAdmin(userId)
                || isSeniorCounselor(userId)
                || isArchiveDepartmentCounselor(userId);
    }


    /**
     * =====================================================
     * 是否可以审核成绩删除申请
     * =====================================================
     *
     * 与 canDeleteScoreDirectly 保持一致：
     * 审核人即可以直接删除的辅导员 / 管理员。
     */
    public boolean canReviewScoreDeleteApply(Long userId) {

        return canDeleteScoreDirectly(userId);
    }


    /**
     * =====================================================
     * 是否可以提交成绩删除申请
     * =====================================================
     *
     * 仅档案部部长 / 副部长（干事不能提交）。
     */
    public boolean canSubmitScoreDeleteApply(Long userId) {

        return isArchiveLeader(userId);
    }

}
