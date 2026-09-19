package com.englishlearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * 名句素材表 quote_material（名句跟读功能的素材库）。
 * builtin=1 表示系统内置（所有用户可见），builtin=0 表示用户自建（仅本人可见）。
 */
@Entity
@Table(name = "quote_material")
public class QuoteMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quote_id")
    public Integer quoteId;

    /** 标题，一般与出处同名，便于列表展示 */
    @Column(name = "title", length = 160, nullable = false)
    public String title;

    /** 出处，如《阿甘正传》 */
    @Column(name = "source", length = 120)
    public String source;

    /** 分类：电影台词 / 英语美句 / 励志名言 */
    @Column(name = "category", length = 30)
    public String category;

    @Column(name = "level", length = 10)
    public String level;

    /** 英文正文，约 50 词 */
    @Column(name = "text_en", columnDefinition = "TEXT")
    public String textEn;

    /** 中文译文（内置素材预置，用户素材可为空，由 AI 一键翻译补全） */
    @Column(name = "text_zh", columnDefinition = "TEXT")
    public String textZh;

    /** 1 内置 / 0 用户自建 */
    @Column(name = "builtin")
    public Integer builtin;

    /** 用户自建素材的归属用户；内置素材为 null */
    @Column(name = "owner_id")
    public Integer ownerId;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;
}
