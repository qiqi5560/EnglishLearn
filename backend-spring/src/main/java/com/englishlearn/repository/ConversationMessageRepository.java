package com.englishlearn.repository;

import com.englishlearn.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Integer> {

    @Query("select m from ConversationMessage m where m.sessionId = :sessionId " +
            "order by m.msgTime asc, m.messageId asc")
    List<ConversationMessage> findBySessionIdOrdered(@Param("sessionId") Integer sessionId);

    List<ConversationMessage> findTop6BySessionIdOrderByMessageIdDesc(Integer sessionId);
}