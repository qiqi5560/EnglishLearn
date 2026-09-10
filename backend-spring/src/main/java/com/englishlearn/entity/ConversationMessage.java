package com.englishlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 对话消息表 conversation_message（SRS 表 4）。
 */
@Entity
@Table(name = "conversation_message")
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    public Integer messageId;

    @Column(name = "session_id")
    public Integer sessionId;

    @Column(name = "speaker", length = 10, nullable = false)
    public String speaker;

    @Column(name = "content_en", columnDefinition = "TEXT", nullable = false)
    public String contentEn;

    @Column(name = "content_zh", columnDefinition = "TEXT")
    public String contentZh;

    @Column(name = "audio_url", length = 255)
    public String audioUrl;

    @Column(name = "msg_time")
    public LocalDateTime msgTime;
}