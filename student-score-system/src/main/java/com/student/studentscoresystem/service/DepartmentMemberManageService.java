package com.student.studentscoresystem.service;

import com.student.studentscoresystem.vo.DepartmentMemberDetailVO;

import java.util.Collection;
import java.util.List;

/**
 * 部门成员管理（查看 / 新增 / 移出）与部门成员名单同步。
 */
public interface DepartmentMemberManageService {

    /**
     * 查询部门当前在编成员（status = 1）
     */
    List<DepartmentMemberDetailVO> listMembers(Long departmentId);

    /**
     * 新增 / 恢复一名部门成员
     */
    void addMember(
            Long departmentId,
            Long userId,
            String position
    );

    /**
     * 移出成员（软移出，同时清理该部门对应的岗位关系）
     */
    void removeMember(Long memberId);

    /**
     * 以 Excel 名单为准同步部门成员：
     * 部门内不在 keepUserIds 中的在编成员将被软移出。
     *
     * @return 被移出的人数
     */
    int syncRoster(
            Long departmentId,
            Collection<Long> keepUserIds
    );

}
