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

    /** 统计区间内的会话（管理后台：日活动量 / 用户使用报表） */
    List<ConversationSession> findByStartTimeGreaterThanEqual(LocalDateTime start);

    /** 某场景下的会话数量（管理后台：删除场景前的引用校验） */
    @Query("select count(s) from ConversationSession s where s.scene.sceneId = :sceneId")
    long countBySceneId(@Param("sceneId") Integer sceneId);

    /** 用户最近的会话（水平预测：构造用户特征向量） */
    List<ConversationSession> findTop20ByUserIdOrderByStartTimeDesc(Integer userId);

    /** 统计区间内的会话（指标量化：完成率 / 跳出率） */
    List<ConversationSession> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    /** 某用户在某天之后的会话（指标量化：任务点击判定） */
    @Query("select s from ConversationSession s where s.userId = :userId and s.startTime >= :start")
    List<ConversationSession> findByUserIdAndStartTimeAfter(@Param("userId") Integer userId,
                                                            @Param("start") LocalDateTime start);
}