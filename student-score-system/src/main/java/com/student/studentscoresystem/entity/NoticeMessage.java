package com.student.studentscoresystem.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@TableName("notice_message")
public class NoticeMessage {


    @TableId(type = IdType.AUTO)
    private Long id;


    private String title;


    private String content;


    private Long senderId;


    private Long receiverId;


    private Integer readStatus;


    private LocalDateTime createTime;


}