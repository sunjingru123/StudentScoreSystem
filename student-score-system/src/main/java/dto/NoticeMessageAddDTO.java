package com.student.studentscoresystem.dto;


import lombok.Data;


@Data
public class NoticeMessageAddDTO {


    private String title;


    private String content;


    private Long senderId;


    private Long receiverId;


}