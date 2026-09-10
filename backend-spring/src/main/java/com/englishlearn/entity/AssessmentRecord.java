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
 * 评测记录表 assessment_record（SRS 表 5）。
 */
@Entity
@Table(name = "assessment_record")
public class AssessmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assess_id")
    public Integer assessId;

    @Column(name = "session_id")
    public Integer sessionId;

    @Column(name = "message_id")
    public Integer messageId;

    @Column(name = "pron_score")
    public Double pronScore;

    @Column(name = "fluency_score")
    public Double fluencyScore;

    @Column(name = "reaction_score")
    public Double reactionScore;

    @Column(name = "natural_score")
    public Double naturalScore;

    @Column(name = "grammar_feedback", columnDefinition = "TEXT")
    public String grammarFeedback;

    /** JSON 串：list of {word, phoneme, note} */
    @Column(name = "phoneme_issues", columnDefinition = "TEXT")
    public String phonemeIssues;

    @Column(name = "better_expression", columnDefinition = "TEXT")
    public String betterExpression;

    @Column(name = "assess_time")
    public LocalDateTime assessTime;

    @CreationTimestamp
    @Column(name = "create_time")
    public LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    public LocalDateTime updateTime;
}