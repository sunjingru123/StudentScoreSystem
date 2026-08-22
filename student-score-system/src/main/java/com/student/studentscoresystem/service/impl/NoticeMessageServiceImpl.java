package com.student.studentscoresystem.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.student.studentscoresystem.entity.NoticeMessage;
import com.student.studentscoresystem.mapper.NoticeMessageMapper;
import com.student.studentscoresystem.service.INoticeMessageService;
import org.springframework.stereotype.Service;


@Service
public class NoticeMessageServiceImpl
        extends ServiceImpl<NoticeMessageMapper, NoticeMessage>
        implements INoticeMessageService {


}