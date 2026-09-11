package com.englishlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * 对话会话表 conversation_session（SRS 表 3）。
 */
@Entity
@Table(name = "conversation_session")
public class ConversationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    public Integer sessionId;

    @Column(name = "user_id")
    public Integer userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "scene_id")
    public Scene scene;

    @Column(name = "mode", length = 20)
    public String mode;

    @Column(name = "start_time")
    public LocalDateTime startTime;

    @Column(name = "end_time")
    public LocalDateTime endTime;

    @Column(name = "duration_sec")
    public Integer durationSec;

    @Column(name = "session_status", length = 20)
    public String sessionStatus;

    /** AI 小结 JSON 串 */
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    public String aiSummary;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    public LocalDateTime updateTime;
}