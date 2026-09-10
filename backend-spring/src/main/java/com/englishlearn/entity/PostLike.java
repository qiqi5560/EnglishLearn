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
 * 点赞记录 post_like（防止重复点赞）。
 */
@Entity
@Table(name = "post_like", uniqueConstraints = @UniqueConstraint(name = "uq_like_user_post", columnNames = {"user_id", "post_id"}))
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    public Integer likeId;

    @Column(name = "post_id")
    public Integer postId;

    @Column(name = "user_id")
    public Integer userId;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;
}