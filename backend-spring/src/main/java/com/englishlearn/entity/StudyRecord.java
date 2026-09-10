package com.englishlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * 学习记录表 study_record（SRS 表 8）。
 */
@Entity
@Table(name = "study_record")
public class StudyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    public Integer recordId;

    @Column(name = "user_id")
    public Integer userId;

    @Column(name = "session_id")
    public Integer sessionId;

    @Column(name = "action_type", length = 30)
    public String actionType;

    @Column(name = "duration_min")
    public Integer durationMin;

    @Column(name = "score")
    public Double score;

    @Column(name = "learn_date")
    public LocalDate learnDate;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;
}