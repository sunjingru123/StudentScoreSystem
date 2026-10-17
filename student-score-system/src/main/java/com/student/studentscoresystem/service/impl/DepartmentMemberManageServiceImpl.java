package com.student.studentscoresystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.student.studentscoresystem.service.DepartmentMemberManageService;
import com.student.studentscoresystem.vo.DepartmentMemberDetailVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 部门成员管理实现。
 *
 * 关键约定：
 *
 * 1. sys_user_department.department_id 指向 department.id；
 * 2. sys_user_position.department_id 指向 sys_department.id；
 * 3. 每个学生都有一条「学生」岗位（department_id = 系统管理），
 *    移出部门时只能清理该部门的「干事 / 副部长 / 部长」岗位，
 *    绝对不能删除「学生」岗位。
 */
@Service
public class DepartmentMemberManageServiceImpl
        implements DepartmentMemberManageService {

    private static final List<String> LEADER_POSITIONS =
            List.of("干事", "副部长", "部长");

    private final SysUserMapper userMapper;

    private final DepartmentMapper departmentMapper;

    private final SysDepartmentMapper sysDepartmentMapper;

    private final SysUserDepartmentMapper userDepartmentMapper;

    private final SysUserPositionMapper userPositionMapper;

    private final SysPositionMapper sysPositionMapper;

    public DepartmentMemberManageServiceImpl(
            SysUserMapper userMapper,
            DepartmentMapper departmentMapper,
            SysDepartmentMapper sysDepartmentMapper,
            SysUserDepartmentMapper userDepartmentMapper,
            SysUserPositionMapper userPositionMapper,
            SysPositionMapper sysPositionMapper
    ) {

        this.userMapper =
                userMapper;

        this.departmentMapper =
                departmentMapper;

        this.sysDepartmentMapper =
                sysDepartmentMapper;

        this.userDepartmentMapper =
                userDepartmentMapper;

        this.userPositionMapper =
                userPositionMapper;

        this.sysPositionMapper =
                sysPositionMapper;
    }

    @Override
    public List<DepartmentMemberDetailVO> listMembers(
            Long departmentId
    ) {

        if (departmentId == null) {

            return new ArrayList<>();
        }

        List<SysUserDepartment> relations =
                userDepartmentMapper.selectList(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getDepartmentId,
                                        departmentId
                                )
                                .eq(
                                        SysUserDepartment::getStatus,
                                        (short) 1
                                )
                                .orderByAsc(
                                        SysUserDepartment::getId
                                )
                );

        if (relations == null || relations.isEmpty()) {

            return new ArrayList<>();
        }

        /*
         * 批量预取用户信息，避免逐条查询（N+1）。
         */

        Set<Long> userIds =
                new LinkedHashSet<>();

        for (SysUserDepartment relation : relations) {

            if (relation != null
                    && relation.getUserId() != null) {

                userIds.add(
                        relation.getUserId()
                );
            }
        }

        Map<Long, SysUser> userMap =
                new HashMap<>();

        if (!userIds.isEmpty()) {

            List<SysUser> users =
                    userMapper.selectBatchIds(
                            userIds
                    );

            if (users != null) {

                for (SysUser user : users) {

                    if (user != null && user.getId() != null) {

                        userMap.put(
                                user.getId(),
                                user
                        );
                    }
                }
            }
        }

        List<DepartmentMemberDetailVO> result =
                new ArrayList<>();

        for (SysUserDepartment relation : relations) {

            if (relation == null) {

                continue;
            }

            DepartmentMemberDetailVO vo =
                    new DepartmentMemberDetailVO();

            vo.setId(
                    relation.getId()
            );

            vo.setUserId(
                    relation.getUserId()
            );

            vo.setPosition(
                    relation.getPosition()
            );

            vo.setStatus(
                    relation.getStatus()
            );

            vo.setJoinTime(
                    relation.getJoinTime()
            );

            SysUser user =
                    relation.getUserId() == null
                            ? null
                            : userMap.get(
                            relation.getUserId()
                    );

            if (user != null) {

                vo.setStudentNo(
                        user.getStudentNo()
                );

                vo.setRealName(
                        user.getRealName()
                );

                vo.setClassName(
                        user.getClassName()
                );
            }

            result.add(vo);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMember(
            Long departmentId,
            Long userId,
            String position
    ) {

        if (departmentId == null) {

            throw new IllegalArgumentException(
                    "部门不能为空"
            );
        }

        if (userId == null) {

            throw new IllegalArgumentException(
                    "请选择学生"
            );
        }

        String normalizedPosition =
                normalizePosition(
                        position
                );

        Department department =
                departmentMapper.selectById(
                        departmentId
                );

        if (department == null) {

            throw new IllegalArgumentException(
                    "部门不存在"
            );
        }

        SysUser user =
                userMapper.selectById(
                        userId
                );

        if (user == null) {

            throw new IllegalArgumentException(
                    "学生不存在"
            );
        }

        SysDepartment sysDepartment =
                findSysDepartmentByName(
                        department.getName()
                );

        if (sysDepartment == null) {

            throw new IllegalArgumentException(
                    "部门《"
                            + department.getName()
                            + "》尚未同步到岗位体系，请先在部门管理中重新保存该部门"
            );
        }

        SysPosition sysPosition =
                findPositionByName(
                        normalizedPosition
                );

        if (sysPosition == null) {

            throw new IllegalArgumentException(
                    "系统中不存在有效岗位："
                            + normalizedPosition
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        SysUserDepartment relation =
                userDepartmentMapper.selectOne(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getUserId,
                                        userId
                                )
                                .eq(
                                        SysUserDepartment::getDepartmentId,
                                        departmentId
                                )
                                .last("LIMIT 1")
                );

        if (relation == null) {

            relation =
                    new SysUserDepartment();

            relation.setUserId(
                    userId
            );

            relation.setDepartmentId(
                    departmentId
            );

            relation.setPosition(
                    normalizedPosition
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
                    normalizedPosition
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

        upsertUserPosition(
                userId,
                sysDepartment.getId(),
                sysPosition.getId(),
                now
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long memberId) {

        if (memberId == null) {

            throw new IllegalArgumentException(
                    "成员不存在"
            );
        }

        SysUserDepartment relation =
                userDepartmentMapper.selectById(
                        memberId
                );

        if (relation == null) {

            throw new IllegalArgumentException(
                    "成员不存在或已被移出"
            );
        }

        relation.setStatus(
                (short) 0
        );

        userDepartmentMapper.updateById(
                relation
        );

        removeLeaderPositions(
                relation.getUserId(),
                resolveSysDepartmentId(
                        relation.getDepartmentId()
                )
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncRoster(
            Long departmentId,
            Collection<Long> keepUserIds
    ) {

        if (departmentId == null) {

            return 0;
        }

        Set<Long> keep =
                keepUserIds == null
                        ? new HashSet<>()
                        : new HashSet<>(
                        keepUserIds
                );

        List<SysUserDepartment> existing =
                userDepartmentMapper.selectList(
                        new LambdaQueryWrapper<SysUserDepartment>()
                                .eq(
                                        SysUserDepartment::getDepartmentId,
                                        departmentId
                                )
                                .eq(
                                        SysUserDepartment::getStatus,
                                        (short) 1
                                )
                );

        if (existing == null || existing.isEmpty()) {

            return 0;
        }

        Long sysDepartmentId =
                resolveSysDepartmentId(
                        departmentId
                );

        int removed = 0;

        for (SysUserDepartment relation : existing) {

            if (relation == null
                    || relation.getUserId() == null) {

                continue;
            }

            if (keep.contains(
                    relation.getUserId()
            )) {

                continue;
            }

            relation.setStatus(
                    (short) 0
            );

            userDepartmentMapper.updateById(
                    relation
            );

            removeLeaderPositions(
                    relation.getUserId(),
                    sysDepartmentId
            );

            removed++;
        }

        return removed;
    }

    /**
     * 新增或更新部门岗位关系。
     */
    private void upsertUserPosition(
            Long userId,
            Long sysDepartmentId,
            Long positionId,
            LocalDateTime now
    ) {

        SysUserPosition userPosition =
                userPositionMapper.selectOne(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .eq(
                                        SysUserPosition::getUserId,
                                        userId
                                )
                                .eq(
                                        SysUserPosition::getDepartmentId,
                                        sysDepartmentId
                                )
                                .last("LIMIT 1")
                );

        if (userPosition == null) {

            userPosition =
                    new SysUserPosition();

            userPosition.setUserId(
                    userId
            );

            userPosition.setDepartmentId(
                    sysDepartmentId
            );

            userPosition.setPositionId(
                    positionId
            );

            userPosition.setCreateTime(
                    now
            );

            userPositionMapper.insert(
                    userPosition
            );

        } else {

            userPosition.setPositionId(
                    positionId
            );

            userPositionMapper.updateById(
                    userPosition
            );
        }
    }

    /**
     * 清理某学生在指定部门下的「干事 / 副部长 / 部长」岗位，
     * 保留其「学生」岗位。
     */
    private void removeLeaderPositions(
            Long userId,
            Long sysDepartmentId
    ) {

        if (userId == null || sysDepartmentId == null) {

            return;
        }

        List<Long> leaderPositionIds =
                findLeaderPositionIds();

        if (leaderPositionIds.isEmpty()) {

            return;
        }

        userPositionMapper.delete(
                new LambdaQueryWrapper<SysUserPosition>()
                        .eq(
                                SysUserPosition::getUserId,
                                userId
                        )
                        .eq(
                                SysUserPosition::getDepartmentId,
                                sysDepartmentId
                        )
                        .in(
                                SysUserPosition::getPositionId,
                                leaderPositionIds
                        )
        );
    }

    private Long resolveSysDepartmentId(Long departmentId) {

        if (departmentId == null) {

            return null;
        }

        Department department =
                departmentMapper.selectById(
                        departmentId
                );

        if (department == null) {

            return null;
        }

        SysDepartment sysDepartment =
                findSysDepartmentByName(
                        department.getName()
                );

        return sysDepartment == null
                ? null
                : sysDepartment.getId();
    }

    private String normalizePosition(String position) {

        String value =
                position == null
                        ? ""
                        : position.trim();

        if (value.isEmpty()) {

            throw new IllegalArgumentException(
                    "职位不能为空"
            );
        }

        if (!LEADER_POSITIONS.contains(
                value
        )) {

            throw new IllegalArgumentException(
                    "职位只能是：干事、副部长、部长"
            );
        }

        return value;
    }

    private SysDepartment findSysDepartmentByName(
            String name
    ) {

        if (name == null || name.trim().isEmpty()) {

            return null;
        }

        return sysDepartmentMapper.selectOne(
                new LambdaQueryWrapper<SysDepartment>()
                        .eq(
                                SysDepartment::getName,
                                name
                        )
                        .last("LIMIT 1")
        );
    }

    private SysPosition findPositionByName(
            String name
    ) {

        return sysPositionMapper.selectOne(
                new LambdaQueryWrapper<SysPosition>()
                        .eq(
                                SysPosition::getName,
                                name
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
    }

    private List<Long> findLeaderPositionIds() {

        List<SysPosition> positions =
                sysPositionMapper.selectList(
                        new LambdaQueryWrapper<SysPosition>()
                                .in(
                                        SysPosition::getName,
                                        LEADER_POSITIONS
                                )
                );

        List<Long> ids =
                new ArrayList<>();

        if (positions == null) {

            return ids;
        }

        for (SysPosition position : positions) {

            if (position != null
                    && position.getId() != null) {

                ids.add(
                        position.getId()
                );
            }
        }

        return ids;
    }

}
