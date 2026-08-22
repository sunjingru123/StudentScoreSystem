package com.student.studentscoresystem.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.NoticeMessageAddDTO;
import com.student.studentscoresystem.entity.NoticeMessage;
import com.student.studentscoresystem.service.INoticeMessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/noticeMessage")
public class NoticeMessageController {



    private final INoticeMessageService noticeMessageService;



    public NoticeMessageController(
            INoticeMessageService noticeMessageService
    ){

        this.noticeMessageService =
                noticeMessageService;

    }





    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result<Void> send(
            @RequestBody NoticeMessageAddDTO dto
    ){


        NoticeMessage message =
                new NoticeMessage();


        message.setTitle(
                dto.getTitle()
        );


        message.setContent(
                dto.getContent()
        );


        message.setSenderId(
                dto.getSenderId()
        );


        message.setReceiverId(
                dto.getReceiverId()
        );


        message.setReadStatus(0);


        noticeMessageService.save(message);



        return Result.success(null);

    }






    /**
     * 查询我的消息
     */
    @GetMapping("/my/{userId}")
    public Result<List<NoticeMessage>> my(
            @PathVariable Long userId
    ){



        List<NoticeMessage> list =

                noticeMessageService.list(

                        new LambdaQueryWrapper<NoticeMessage>()

                                .eq(
                                        NoticeMessage::getReceiverId,
                                        userId
                                )

                                .orderByDesc(
                                        NoticeMessage::getCreateTime
                                )

                );



        return Result.success(list);


    }






    /**
     * 标记已读
     */
    @PutMapping("/read/{id}")
    public Result<Void> read(
            @PathVariable Long id
    ){



        NoticeMessage message =

                noticeMessageService.getById(id);



        if(message == null){

            return Result.fail(
                    "消息不存在"
            );

        }



        message.setReadStatus(1);



        noticeMessageService.updateById(
                message
        );



        return Result.success(null);

    }







    /**
     * 删除消息
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable Long id
    ){



        boolean result =

                noticeMessageService.removeById(
                        id
                );



        if(!result){

            return Result.fail(
                    "消息不存在"
            );

        }



        return Result.success(null);


    }

    /**
     * 查询全部消息
     */
    @GetMapping("/list")
    public Result<List<NoticeMessage>> list(){


        List<NoticeMessage> list =
                noticeMessageService.list(
                        new LambdaQueryWrapper<NoticeMessage>()
                                .orderByDesc(
                                        NoticeMessage::getCreateTime
                                )
                );


        return Result.success(list);

    }
}