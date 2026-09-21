package com.englishlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * 用户关注关系 user_follow（follower 关注 followee，公开可见，与单向的「学习搭子」user_partner 区分）。
 */
@Entity
@Table(name = "user_follow",
        uniqueConstraints = @UniqueConstraint(name = "uq_follow_pair", columnNames = {"follower_id", "followee_id"}),
        indexes = {
                @Index(name = "idx_follow_follower", columnList = "follower_id"),
                @Index(name = "idx_follow_followee", columnList = "followee_id")
        })
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    public Integer followId;

    /** 发起关注的人 */
    @Column(name = "follower_id", nullable = false)
    public Integer followerId;

    /** 被关注的人 */
    @Column(name = "followee_id", nullable = false)
    public Integer followeeId;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;
}
