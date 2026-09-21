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
 * 用户表 users（SRS 表 1）。
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    public Integer userId;

    @Column(name = "phone", length = 20, nullable = false, unique = true)
    public String phone;

    @Column(name = "password_hash", length = 128)
    public String passwordHash;

    @Column(name = "nickname", length = 50)
    public String nickname;

    @Column(name = "avatar_url", length = 255)
    public String avatarUrl;

    /** 个性签名（他人主页展示） */
    @Column(name = "bio", length = 100)
    public String bio;

    @Column(name = "age_group", length = 20)
    public String ageGroup;

    @Column(name = "user_role", length = 20)
    public String userRole;

    @Column(name = "guardian_id")
    public Integer guardianId;

    @Column(name = "status")
    public Integer status;

    /** 发帖处罚截止时间：为空或早于当前时间表示未受处罚（管理后台社区治理） */
    @Column(name = "ban_until")
    public LocalDateTime banUntil;

    /** 处罚原因，展示给用户 */
    @Column(name = "ban_reason", length = 255)
    public String banReason;

    @Column(name = "register_time")
    public LocalDateTime registerTime;

    @Column(name = "last_login_time")
    public LocalDateTime lastLoginTime;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    public LocalDateTime updateTime;
}