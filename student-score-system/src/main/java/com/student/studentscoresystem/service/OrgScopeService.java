package com.student.studentscoresystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.entity.Department;
import com.student.studentscoresystem.mapper.DepartmentMapper;
import com.student.studentscoresystem.utils.OrgPositionLevel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 组织管理范围（数据隔离）。
 *
 * 规则：
 *
 * 1. 部门负责人只能管理本部门；
 * 2. 小社团负责人只能管理本小社团；
 * 3. 大社团负责人管理本大社团及其下属所有小社团；
 * 4. 任何组织负责人都不能越权到其它组织。
 *
 * 因此「管理范围」= 自己担任正/副职的组织
 *              + 若该组织是大社团，则附加其所有下级组织。
 */
@Service
public class OrgScopeService {

    private final DepartmentMapper departmentMapper;

    public OrgScopeService(
            DepartmentMapper departmentMapper
    ) {

        this.departmentMapper = departmentMapper;
    }

    /**
     * 展开管理范围
     *
     * @param ownOrganizationIds 用户担任负责人的组织 ID
     * @return 展开后的组织 ID（含大社团的下级小社团）
     */
    public List<Long> expandManagedOrganizationIds(
            Collection<Long> ownOrganizationIds
    ) {

        List<Long> result =
                new ArrayList<>();

        if (ownOrganizationIds == null
                || ownOrganizationIds.isEmpty()) {

            return result;
        }

        List<Department> all =
                departmentMapper.selectList(
                        new LambdaQueryWrapper<>()
                );

        if (all == null || all.isEmpty()) {

            return result;
        }

        Map<Long, Department> byId =
                new HashMap<>();

        Map<Long, List<Long>> childrenByParent =
                new HashMap<>();

        for (Department department : all) {

            if (department == null
                    || department.getId() == null) {

                continue;
            }

            byId.put(
                    department.getId(),
                    department
            );

            if (department.getParentId() != null) {

                childrenByParent
                        .computeIfAbsent(
                                department.getParentId(),
                                key -> new ArrayList<>()
                        )
                        .add(
                                department.getId()
                        );
            }
        }

        Set<Long> managed =
                new LinkedHashSet<>();

        for (Long organizationId : ownOrganizationIds) {

            if (organizationId == null
                    || !byId.containsKey(organizationId)) {

                continue;
            }

            managed.add(
                    organizationId
            );

            Department organization =
                    byId.get(
                            organizationId
                    );

            /*
             * 只有大社团才会向下覆盖小社团
             */
            if (organization.getDepartmentType() != null
                    && organization.getDepartmentType()
                    == OrgPositionLevel.TYPE_BIG_CLUB) {

                List<Long> children =
                        childrenByParent.get(
                                organizationId
                        );

                if (children != null) {

                    managed.addAll(
                            children
                    );
                }
            }
        }

        result.addAll(
                managed
        );

        return result;
    }

    /**
     * 判断「自己负责的组织」是否覆盖目标组织
     *
     * @param ownOrganizationIds 用户担任负责人的组织 ID
     * @param targetOrganizationId 目标组织 ID
     */
    public boolean covers(
            Collection<Long> ownOrganizationIds,
            Long targetOrganizationId
    ) {

        if (targetOrganizationId == null) {

            return false;
        }

        return expandManagedOrganizationIds(
                ownOrganizationIds
        ).contains(
                targetOrganizationId
        );
    }
}
