package com.englishlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * 站内系统通知 user_notification：本轮仅承载社区互动（帖子被点赞 / 被评论 / 评论被回复）。
 * type：like / comment / reply；targetType：post / comment；readFlag：0 未读 / 1 已读。
 */
@Entity
@Table(name = "user_notification",
        indexes = {
                @Index(name = "idx_notification_user_read", columnList = "user_id, is_read, create_time")
        })
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    public Integer notificationId;

    /** 通知接收者 */
    @Column(name = "user_id", nullable = false)
    public Integer userId;

    /** 触发者（点赞/评论的人） */
    @Column(name = "actor_id")
    public Integer actorId;

    @Column(name = "type", length = 20, nullable = false)
    public String type;

    @Column(name = "target_type", length = 20)
    public String targetType;

    @Column(name = "target_id")
    public Integer targetId;

    /** 通知正文摘要，如「张三 赞了你的帖子《xxx》」 */
    @Column(name = "content", length = 255)
    public String content;

    @Column(name = "is_read", nullable = false)
    public Integer readFlag;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;
}
