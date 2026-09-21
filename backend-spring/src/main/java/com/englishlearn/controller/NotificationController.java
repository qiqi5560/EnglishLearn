package com.englishlearn.controller;

import com.englishlearn.common.ApiResponse;
import com.englishlearn.dto.SocialDtos;
import com.englishlearn.entity.User;
import com.englishlearn.security.AuthFacade;
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
 * 站内系统通知接口（/notifications）：本轮仅社区互动（点赞 / 评论 / 回复）。
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final AuthFacade authFacade;
    private final NotificationService notificationService;

    public NotificationController(AuthFacade authFacade, NotificationService notificationService) {
        this.authFacade = authFacade;
        this.notificationService = notificationService;
    }

    /** type 为空查全部；可选 like / comment / reply */
    @GetMapping("/list")
    public ApiResponse list(@RequestParam(required = false) String type) {
        User user = authFacade.requireUser();
        return ApiResponse.ok(notificationService.list(user, type));
    }

    /** ids 为空或缺失表示全部标记已读 */
    @PostMapping("/read")
    public ApiResponse read(@RequestBody(required = false) SocialDtos.ReadNotificationIn body) {
        User user = authFacade.requireUser();
        notificationService.markRead(user, body == null ? null : body.ids());
        return ApiResponse.ok(null, "已读");
    }

    @GetMapping("/unread")
    public ApiResponse unread() {
        User user = authFacade.requireUser();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("notification", notificationService.unreadCount(user));
        return ApiResponse.ok(data);
    }
}
