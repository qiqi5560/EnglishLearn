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
 * 用户导入的阅读素材 reading_doc（支持 txt / md / docx / 直接粘贴）。
 * 段落以 JSON 数组存放：[{"en":"...","zh":"..."}]，逐段翻译后就地回写。
 */
@Entity
@Table(name = "reading_doc")
public class ReadingDoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_id")
    public Integer docId;

    @Column(name = "owner_id")
    public Integer ownerId;

    @Column(name = "title", length = 200, nullable = false)
    public String title;

    /** 来源类型：txt / md / docx / paste */
    @Column(name = "source_type", length = 20)
    public String sourceType;

    /** 段落 JSON：[{"en":"...","zh":"..."}] */
    @Column(name = "paragraphs_json", columnDefinition = "TEXT")
    public String paragraphsJson;

    @Column(name = "paragraph_count")
    public Integer paragraphCount;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;
}
