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
 * 社区帖子 community_post（F008）。
 */
@Entity
@Table(name = "community_post")
public class CommunityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    public Integer postId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    public User author;

    @Column(name = "title", length = 100, nullable = false)
    public String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    public String content;

    @Column(name = "topic", length = 30)
    public String topic;

    @Column(name = "likes")
    public Integer likes;

    @Column(name = "comment_count")
    public Integer commentCount;

    @Column(name = "status")
    public Integer status;

    @Column(name = "is_top")
    public Boolean isTop;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    public LocalDateTime updateTime;
}