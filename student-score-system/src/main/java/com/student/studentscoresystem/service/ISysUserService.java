package com.student.studentscoresystem.service;

import com.student.studentscoresystem.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.student.studentscoresystem.vo.SysUserVO;

public interface ISysUserService extends IService<SysUser> {
    /**
     * 【新增】分页查询学生列表，并自动计算每个人的分数（解决前端加载卡顿）
     */
    Page<SysUserVO> getStudentPageWithScores(long pageNum, long pageSize, String keyword);
}