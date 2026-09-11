package com.englishlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * 用户搭子关系表 user_partner（当前用户自建的学习搭子，单向关系）。
 */
@Entity
@Table(name = "user_partner",
        uniqueConstraints = @UniqueConstraint(name = "uq_partner_user", columnNames = {"user_id", "partner_user_id"}))
public class UserPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Integer id;

    @Column(name = "user_id", nullable = false)
    public Integer userId;

    @Column(name = "partner_user_id", nullable = false)
    public Integer partnerUserId;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;
}