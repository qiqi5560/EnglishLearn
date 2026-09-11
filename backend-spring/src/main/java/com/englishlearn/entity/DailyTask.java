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
 * 每日任务 daily_task（F001 执行闭环扩展表）。
 */
@Entity
@Table(name = "daily_task")
public class DailyTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    public Integer taskId;

    @Column(name = "user_id")
    public Integer userId;

    @Column(name = "plan_id")
    public Integer planId;

    @Column(name = "task_type", length = 20)
    public String taskType;

    @Column(name = "title", length = 100, nullable = false)
    public String title;

    @Column(name = "duration_min")
    public Integer durationMin;

    @Column(name = "scene_id")
    public Integer sceneId;

    @Column(name = "resource_id")
    public Integer resourceId;

    /** 0/1，与 SQLAlchemy Integer 列一致 */
    @Column(name = "done")
    public Integer done;

    @Column(name = "task_date")
    public LocalDate taskDate;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;
}