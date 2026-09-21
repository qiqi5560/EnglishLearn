package com.englishlearn.controller;

import com.englishlearn.common.ApiResponse;
import com.englishlearn.dto.SocialDtos;
import com.englishlearn.entity.User;
import com.englishlearn.security.AuthFacade;
import com.englishlearn.service.MessageService;
import com.englishlearn.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 站内信接口（/messages）：会话列表、消息历史、发送私信、标记已读、未读数。
 */
@RestController
@RequestMapping("/messages")
public class MessageController {

    private final AuthFacade authFacade;
    private final MessageService messageService;
    private final NotificationService notificationService;

    public MessageController(AuthFacade authFacade,
                             MessageService messageService,
                             NotificationService notificationService) {
        this.authFacade = authFacade;
        this.messageService = messageService;
        this.notificationService = notificationService;
    }

    @GetMapping("/conversations")
    public ApiResponse conversations() {
        User user = authFacade.requireUser();
        return ApiResponse.ok(messageService.conversations(user));
    }

    @GetMapping("/history")
    public ApiResponse history(@RequestParam Integer peerId,
                               @RequestParam(defaultValue = "50") Integer limit) {
        User user = authFacade.requireUser();
        return ApiResponse.ok(messageService.history(user, peerId, limit));
    }

    @PostMapping("/send")
    public ApiResponse send(@RequestBody SocialDtos.SendMessageIn body) {
        User user = authFacade.requireUser();
        return ApiResponse.ok(messageService.send(user, body.peerId(), body.content()), "已发送");
    }

    @PostMapping("/read")
    public ApiResponse read(@RequestBody SocialDtos.ReadMessageIn body) {
        User user = authFacade.requireUser();
        int updated = messageService.markRead(user, body.peerId());
        return ApiResponse.ok(Map.of("updated", updated), "已读");
    }

    /** 前端轮询的红点数据源：私信未读 + 系统通知未读 */
    @GetMapping("/unread")
    public ApiResponse unread() {
        User user = authFacade.requireUser();
        long message = messageService.unreadCount(user);
        long notification = notificationService.unreadCount(user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", message);
        data.put("notification", notification);
        data.put("total", message + notification);
        return ApiResponse.ok(data);
    }
}
