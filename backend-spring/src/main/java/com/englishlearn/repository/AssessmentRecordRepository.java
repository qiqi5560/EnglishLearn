package com.englishlearn.repository;

import com.englishlearn.entity.AssessmentRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssessmentRecordRepository extends JpaRepository<AssessmentRecord, Integer> {

    List<AssessmentRecord> findBySessionIdOrderByAssessId(Integer sessionId);

    AssessmentRecord findFirstBySessionIdAndMessageIdOrderByAssessIdDesc(Integer sessionId, Integer messageId);

    @Query("select a from AssessmentRecord a, ConversationSession s " +
            "where a.sessionId = s.sessionId and s.userId = :userId " +
            "order by a.assessId desc")
    List<AssessmentRecord> latest(@Param("userId") Integer userId, Pageable pageable);

    /** 无会话的评测（名句跟读等）：直接按 user_id 取最近记录 */
    List<AssessmentRecord> findTop20ByUserIdOrderByAssessIdDesc(Integer userId);
}