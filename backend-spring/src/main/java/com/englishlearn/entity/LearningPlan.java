package com.englishlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * 学习方案 learning_plan（SRS 表 6）。
 */
@Entity
@Table(name = "learning_plan")
public class LearningPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    public Integer planId;

    @Column(name = "user_id", unique = true)
    public Integer userId;

    @Column(name = "target_goal", length = 50)
    public String targetGoal;

    @Column(name = "level_start", length = 10)
    public String levelStart;

    @Column(name = "level_current", length = 10)
    public String levelCurrent;

    @Column(name = "plan_content", columnDefinition = "TEXT")
    public String planContent;

    @Column(name = "plan_start")
    public LocalDate planStart;

    @Column(name = "plan_status", length = 20)
    public String planStatus;

    @UpdateTimestamp
    @Column(name = "update_time")
    public LocalDateTime updateTime;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;
}