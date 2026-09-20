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

    /** 某会话中指定发言人的消息数（水平预测：用户是否开口、开口多少） */
    long countBySessionIdAndSpeaker(Integer sessionId, String speaker);

    /** 批量统计多个会话的用户发言数，返回 [sessionId, count]（避免 N+1 查询） */
    @Query("select m.sessionId, count(m) from ConversationMessage m " +
            "where m.sessionId in :sessionIds and m.speaker = 'user' group by m.sessionId")
    List<Object[]> countUserMessages(@Param("sessionIds") List<Integer> sessionIds);
}