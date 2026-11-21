package com.student.studentscoresystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.student.studentscoresystem.vo.RecycledStudentVO;

/**
 * 学生账号软删除（回收站）管理。
 *
 * 业务规则：
 *
 * 1. 只允许删除「学生」账号，管理员、辅导员及其他组织人员不可删除；
 * 2. 删除只做标记（sys_user.deleted = 1），
 *    不物理删除，历史成绩 / 加减分 / 获奖 / 活动参与 / 审核记录全部保留；
 * 3. 删除同时记录删除时间、操作管理员和删除原因，并立即失效该账号的登录会话；
 * 4. 恢复只是清除删除标记，学生重新出现在正常学生列表中。
 */
public interface StudentAccountManageService {

    /**
     * 分页查询回收站中的学生
     *
     * @param pageNum   页码（从 1 开始）
     * @param pageSize  每页条数
     * @param keyword   学号 / 姓名 / 用户名 / 班级 模糊搜索，可为空
     * @param className 班级精确匹配，可为空
     */
    Page<RecycledStudentVO> pageRecycledStudents(
            long pageNum,
            long pageSize,
            String keyword,
            String className
    );

    /**
     * 软删除一名学生
     *
     * @param studentId  学生用户 ID
     * @param reason     删除原因（必填）
     * @param operatorId 当前登录管理员 ID
     */
    void softDeleteStudent(
            Long studentId,
            String reason,
            Long operatorId
    );

    /**
     * 恢复一名已删除的学生
     *
     * @param studentId  学生用户 ID
     * @param operatorId 当前登录管理员 ID
     */
    void restoreStudent(
            Long studentId,
            Long operatorId
    );

}
