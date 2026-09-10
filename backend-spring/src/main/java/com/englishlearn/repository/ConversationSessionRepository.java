package com.englishlearn.repository;

import com.englishlearn.entity.ConversationSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ConversationSessionRepository extends JpaRepository<ConversationSession, Integer> {

    @Query("select s from ConversationSession s where s.userId = :userId " +
            "and (:status is null or s.sessionStatus = :status) " +
            "order by s.sessionId desc")
    Page<ConversationSession> mySessions(@Param("userId") Integer userId,
                                         @Param("status") String status,
                                         Pageable pageable);

    List<ConversationSession> findTop5ByUserIdAndSessionStatusOrderByEndTimeDesc(Integer userId, String status);

    long countByUserIdAndSessionStatus(Integer userId, String status);

    long countByStartTimeBetween(LocalDateTime start, LocalDateTime end);
}