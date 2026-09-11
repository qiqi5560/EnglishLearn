package com.englishlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * 学习资源表 learning_resource（SRS 表 7）。
 */
@Entity
@Table(name = "learning_resource")
public class LearningResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resource_id")
    public Integer resourceId;

    @Column(name = "title", length = 100, nullable = false)
    public String title;

    @Column(name = "res_type", length = 20)
    public String resType;

    @Column(name = "category", length = 30)
    public String category;

    @Column(name = "level", length = 10)
    public String level;

    @Column(name = "media_url", length = 255)
    public String mediaUrl;

    @Column(name = "duration_sec")
    public Integer durationSec;

    @Column(name = "uploader_id")
    public Integer uploaderId;

    @Column(name = "status")
    public Integer status;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    public LocalDateTime updateTime;
}