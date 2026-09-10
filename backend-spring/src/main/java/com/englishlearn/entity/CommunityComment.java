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
import java.time.LocalDateTime;

/**
 * 社区评论 community_comment（F008）。
 */
@Entity
@Table(name = "community_comment")
public class CommunityComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    public Integer commentId;

    @Column(name = "post_id")
    public Integer postId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    public User author;

    @Column(name = "parent_id")
    public Integer parentId;

    @Column(name = "content", columnDefinition = "TEXT")
    public String content;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;
}