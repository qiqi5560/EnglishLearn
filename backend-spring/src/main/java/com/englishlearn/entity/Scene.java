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
 * 对话场景表 scene（SRS 表 2）。
 */
@Entity
@Table(name = "scene")
public class Scene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scene_id")
    public Integer sceneId;

    @Column(name = "scene_name", length = 50, nullable = false)
    public String sceneName;

    @Column(name = "scene_category", length = 20)
    public String sceneCategory;

    @Column(name = "scene_desc", length = 255)
    public String sceneDesc;

    @Column(name = "level_scope", length = 20)
    public String levelScope;

    /** JSON 串：{"role": "...", "script": "..."} */
    @Column(name = "role_setting", columnDefinition = "TEXT")
    public String roleSetting;

    @Column(name = "cover_url", length = 255)
    public String coverUrl;

    @Column(name = "status")
    public Integer status;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    public LocalDateTime updateTime;
}