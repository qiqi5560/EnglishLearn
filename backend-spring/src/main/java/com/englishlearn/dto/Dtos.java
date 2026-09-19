package com.englishlearn.dto;

import com.englishlearn.common.TimeUtil;
import com.englishlearn.entity.CommunityComment;
import com.englishlearn.entity.CommunityPost;
import com.englishlearn.entity.ConversationMessage;
import com.englishlearn.entity.ConversationSession;
import com.englishlearn.entity.DailyTask;
import com.englishlearn.entity.LearningPlan;
import com.englishlearn.entity.LearningResource;
import com.englishlearn.entity.Scene;
import com.englishlearn.entity.User;
import com.englishlearn.common.JsonUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ORM → 前端 JSON 序列化（字段 camelCase，与原 FastAPI serializers.py 对齐）。
 */
public final class Dtos {

    private Dtos() {
    }

    private static Map<String, Object> map() {
        return new LinkedHashMap<>();
    }

    public static Map<String, Object> userToDict(User user, String level) {
        Map<String, Object> m = map();
        m.put("userId", user.userId);
        m.put("phone", user.phone);
        m.put("nickname", user.nickname);
        m.put("avatarUrl", user.avatarUrl);
        m.put("ageGroup", user.ageGroup);
        m.put("role", user.userRole);
        m.put("guardianId", user.guardianId);
        m.put("status", user.status);
        m.put("level", level);
        m.put("registerTime", TimeUtil.iso(user.registerTime));
        m.put("lastLoginTime", TimeUtil.iso(user.lastLoginTime));
        m.put("banUntil", TimeUtil.iso(user.banUntil));
        m.put("banReason", user.banReason);
        m.put("punished", user.banUntil != null && user.banUntil.isAfter(java.time.LocalDateTime.now()));
        return m;
    }

    public static Map<String, Object> sceneToDict(Scene scene) {
        Map<String, Object> m = map();
        m.put("id", scene.sceneId);
        m.put("name", scene.sceneName);
        m.put("category", scene.sceneCategory);
        m.put("desc", scene.sceneDesc);
        m.put("level", scene.levelScope);
        Map<String, Object> roleSetting = JsonUtil.parseMap(scene.roleSetting);
        m.put("role", roleSetting != null && roleSetting.get("role") != null ? String.valueOf(roleSetting.get("role")) : "");
        m.put("roleSetting", roleSetting);
        m.put("coverUrl", scene.coverUrl);
        m.put("status", scene.status);
        return m;
    }

    public static Map<String, Object> resourceToDict(LearningResource r) {
        Map<String, Object> m = map();
        m.put("id", r.resourceId);
        m.put("title", r.title);
        m.put("type", r.resType);
        m.put("category", r.category);
        m.put("level", r.level);
        m.put("mediaUrl", r.mediaUrl);
        m.put("durationSec", r.durationSec);
        m.put("status", r.status);
        return m;
    }

    public static Map<String, Object> messageToDict(ConversationMessage msg) {
        Map<String, Object> m = map();
        m.put("id", msg.messageId);
        m.put("speaker", msg.speaker);
        m.put("contentEn", msg.contentEn);
        m.put("contentZh", msg.contentZh);
        m.put("audioUrl", msg.audioUrl);
        m.put("time", TimeUtil.iso(msg.msgTime));
        return m;
    }

    public static Map<String, Object> sessionToDict(ConversationSession s, List<ConversationMessage> messages) {
        Map<String, Object> m = map();
        m.put("sessionId", s.sessionId);
        m.put("sceneId", s.scene != null ? s.scene.sceneId : null);
        m.put("sceneName", s.scene != null ? s.scene.sceneName : "");
        m.put("mode", s.mode);
        m.put("status", s.sessionStatus);
        m.put("startTime", TimeUtil.iso(s.startTime));
        m.put("endTime", TimeUtil.iso(s.endTime));
        m.put("durationSec", s.durationSec);
        m.put("messages", messages.stream().map(Dtos::messageToDict).toList());
        return m;
    }

    public static Map<String, Object> planToDict(LearningPlan p) {
        Map<String, Object> m = map();
        m.put("planId", p.planId);
        m.put("targetGoal", p.targetGoal);
        m.put("levelStart", p.levelStart);
        m.put("levelCurrent", p.levelCurrent);
        m.put("planContent", p.planContent);
        m.put("planStart", TimeUtil.iso(p.planStart));
        m.put("planStatus", p.planStatus);
        m.put("updateTime", TimeUtil.iso(p.updateTime));
        return m;
    }

    public static Map<String, Object> taskToDict(DailyTask t) {
        Map<String, Object> m = map();
        m.put("taskId", t.taskId);
        m.put("type", t.taskType);
        m.put("title", t.title);
        m.put("durationMin", t.durationMin);
        m.put("sceneId", t.sceneId);
        m.put("resourceId", t.resourceId);
        m.put("done", t.done != null && t.done != 0);
        m.put("taskDate", TimeUtil.iso(t.taskDate));
        return m;
    }

    public static Map<String, Object> postToDict(CommunityPost p, boolean liked) {
        Map<String, Object> m = map();
        m.put("id", p.postId);
        m.put("author", p.author != null && p.author.nickname != null ? p.author.nickname : "用户");
        m.put("authorId", p.author != null ? p.author.userId : null);
        m.put("title", p.title);
        m.put("content", p.content);
        m.put("topic", p.topic);
        m.put("likes", p.likes);
        m.put("comments", p.commentCount);
        m.put("status", p.status);
        m.put("isTop", Boolean.TRUE.equals(p.isTop));
        m.put("liked", liked);
        m.put("createTime", TimeUtil.iso(p.createTime));
        return m;
    }

    public static Map<String, Object> commentToDict(CommunityComment c) {
        Map<String, Object> m = map();
        m.put("id", c.commentId);
        m.put("postId", c.postId);
        m.put("author", c.author != null && c.author.nickname != null ? c.author.nickname : "用户");
        m.put("content", c.content);
        m.put("createTime", TimeUtil.iso(c.createTime));
        return m;
    }
}