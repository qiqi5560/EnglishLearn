package com.englishlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * 内容分享记录 share_record：用户把帖子 / 场景 / 素材 / 学习成果分享到站外渠道的留痕，用于统计。
 * channel：weibo / xiaohongshu / wechat / copy（不接入任何开放平台 SDK，只做复制链接与渠道标记）。
 */
@Entity
@Table(name = "share_record",
        indexes = {
                @Index(name = "idx_share_user", columnList = "user_id, create_time"),
                @Index(name = "idx_share_channel", columnList = "channel")
        })
public class ShareRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "share_id")
    public Integer shareId;

    @Column(name = "user_id", nullable = false)
    public Integer userId;

    /** post / scene / resource / achievement */
    @Column(name = "content_type", length = 20)
    public String contentType;

    @Column(name = "content_id")
    public Integer contentId;

    @Column(name = "channel", length = 20)
    public String channel;

    @Column(name = "share_url", length = 500)
    public String shareUrl;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;
}
