package com.englishlearn.service;

import com.englishlearn.common.TimeUtil;
import com.englishlearn.entity.User;
import com.englishlearn.entity.UserNotification;
import com.englishlearn.repository.UserNotificationRepository;
import com.englishlearn.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 站内系统通知服务：本轮仅承载社区互动（帖子被点赞 / 被评论 / 评论被回复）。
 * 由 CommunityService 在互动成功后调用 notifyInteraction(...)，取消点赞时调用 revokeInteraction(...) 撤掉噪声。
 */
@Service
public class NotificationService {

    public static final String TYPE_LIKE = "like";
    public static final String TYPE_COMMENT = "comment";
    public static final String TYPE_REPLY = "reply";

    private static final int MAX_CONTENT = 200;

    private final UserNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(UserNotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * 写入一条互动通知。同一人对同一目标的同类互动只保留最新一条（先删旧再插新，保证列表排序新鲜）。
     */
    @Transactional
    public void notifyInteraction(Integer targetUserId, String type, Integer actorId,
                                  String targetType, Integer targetId, String content) {
        if (targetUserId == null || actorId == null || targetUserId.equals(actorId)) {
            return;
        }
        if (targetId == null) {
            return;
        }
        String text = content == null ? "" : content.strip();
        if (text.length() > MAX_CONTENT) {
            text = text.substring(0, MAX_CONTENT);
        }
        UserNotification existing = notificationRepository
                .findFirstByUserIdAndTypeAndActorIdAndTargetId(targetUserId, type, actorId, targetId)
                .orElse(null);
        if (existing != null) {
            notificationRepository.delete(existing);
        }
        UserNotification n = new UserNotification();
        n.userId = targetUserId;
        n.actorId = actorId;
        n.type = type;
        n.targetType = targetType;
        n.targetId = targetId;
        n.content = text;
        n.readFlag = 0;
        notificationRepository.save(n);
    }

    /** 撤回互动通知（如取消点赞） */
    @Transactional
    public void revokeInteraction(Integer targetUserId, String type, Integer actorId, Integer targetId) {
        if (targetUserId == null || actorId == null || targetId == null) {
            return;
        }
        notificationRepository.deleteByUserIdAndTypeAndActorIdAndTargetId(targetUserId, type, actorId, targetId);
    }

    public Map<String, Object> list(User user, String type) {
        List<UserNotification> rows;
        if (type == null || type.isBlank()) {
            rows = notificationRepository.findTop100ByUserIdOrderByCreateTimeDesc(user.userId);
        } else {
            rows = notificationRepository.findTop100ByUserIdAndTypeOrderByCreateTimeDesc(user.userId, type.strip());
        }
        Set<Integer> actorIds = new LinkedHashSet<>();
        for (UserNotification n : rows) {
            if (n.actorId != null) {
                actorIds.add(n.actorId);
            }
        }
        Map<Integer, User> actors = actorIds.isEmpty() ? Map.of()
                : userRepository.findAllById(actorIds).stream()
                        .collect(Collectors.toMap(u -> u.userId, Function.identity(), (a, b) -> a));
        List<Map<String, Object>> list = new ArrayList<>();
        for (UserNotification n : rows) {
            list.add(payload(n, actors));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("unread", unreadCount(user));
        return data;
    }

    public long unreadCount(User user) {
        return notificationRepository.countByUserIdAndReadFlag(user.userId, 0);
    }

    @Transactional
    public void markRead(User user, List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            notificationRepository.markAllRead(user.userId);
        } else {
            notificationRepository.markReadByIds(user.userId, ids);
        }
    }

    private Map<String, Object> payload(UserNotification n, Map<Integer, User> actors) {
        User actor = n.actorId == null ? null : actors.get(n.actorId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.notificationId);
        m.put("type", n.type);
        m.put("actorId", n.actorId);
        m.put("actorName", actor != null && actor.nickname != null ? actor.nickname : "用户");
        m.put("actorAvatar", actor != null ? actor.avatarUrl : null);
        m.put("targetType", n.targetType);
        m.put("targetId", n.targetId);
        m.put("content", n.content);
        m.put("read", n.readFlag != null && n.readFlag != 0);
        m.put("createTime", TimeUtil.iso(n.createTime));
        return m;
    }
}
