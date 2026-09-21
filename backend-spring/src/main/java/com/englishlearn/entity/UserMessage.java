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
 * 站内私信 user_message：两个用户之间的一对一消息。
 * readFlag：0 未读 / 1 已读（与 DailyTask.done 的 0/1 风格保持一致）。
 */
@Entity
@Table(name = "user_message",
        indexes = {
                @Index(name = "idx_message_receiver_read", columnList = "receiver_id, is_read"),
                @Index(name = "idx_message_pair_time", columnList = "sender_id, receiver_id, create_time")
        })
public class UserMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    public Integer messageId;

    @Column(name = "sender_id", nullable = false)
    public Integer senderId;

    @Column(name = "receiver_id", nullable = false)
    public Integer receiverId;

    @Column(name = "content", length = 500, nullable = false)
    public String content;

    @Column(name = "is_read", nullable = false)
    public Integer readFlag;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;
}
