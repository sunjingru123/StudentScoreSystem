package com.student.studentscoresystem.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.NoticeMessageAddDTO;
import com.student.studentscoresystem.entity.NoticeMessage;
import com.student.studentscoresystem.service.INoticeMessageService;
import com.student.studentscoresystem.annotation.RequireRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
            @RequestBody NoticeMessageAddDTO dto,
            HttpServletRequest request
    ){


        NoticeMessage message =
                new NoticeMessage();


        message.setTitle(
                dto.getTitle()
        );


        message.setContent(
                dto.getContent()
        );


        message.setSenderId(currentUserId(request));


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
            @PathVariable Long userId,
            HttpServletRequest request,
            HttpServletResponse response
    ){

        Long currentUserId = currentUserId(request);
        if (currentUserId == null || !currentUserId.equals(userId)) {
            response.setStatus(403);
            return Result.fail("无权访问该用户消息");
        }



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
            @PathVariable Long id,
            HttpServletRequest request,
            HttpServletResponse response
    ){



        NoticeMessage message =

                noticeMessageService.getById(id);



        if(message == null){

            return Result.fail(
                    "消息不存在"
            );

        }

        if (!java.util.Objects.equals(message.getReceiverId(), currentUserId(request))) {
            response.setStatus(403);
            return Result.fail("无权操作该消息");
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
            @PathVariable Long id,
            HttpServletRequest request,
            HttpServletResponse response
    ){



        NoticeMessage message = noticeMessageService.getById(id);
        if (message == null) {
            return Result.fail("消息不存在");
        }
        if (!java.util.Objects.equals(message.getReceiverId(), currentUserId(request))) {
            response.setStatus(403);
            return Result.fail("无权操作该消息");
        }
        boolean result = noticeMessageService.removeById(id);



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
    @RequireRole("管理员")
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

    private Long currentUserId(HttpServletRequest request) {
        Object value = request.getAttribute("userId");
        if (value == null) return null;
        try { return Long.valueOf(value.toString()); }
        catch (NumberFormatException ex) { return null; }
    }
}
