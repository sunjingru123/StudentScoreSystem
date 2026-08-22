package com.student.studentscoresystem.service.impl;

import com.student.studentscoresystem.entity.OperationLog;
import com.student.studentscoresystem.mapper.OperationLogMapper;
import com.student.studentscoresystem.service.IOperationLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 系统操作日志表 服务实现类
 * </p>
 *
 * @author 茹茹宝贝
 * @since 2026-08-05
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements IOperationLogService {

}
