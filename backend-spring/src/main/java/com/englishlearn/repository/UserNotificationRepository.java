package com.englishlearn.repository;

import com.englishlearn.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 站内系统通知仓储（社区互动：点赞 / 评论 / 回复）。
 */
public interface UserNotificationRepository extends JpaRepository<UserNotification, Integer> {

    List<UserNotification> findTop100ByUserIdOrderByCreateTimeDesc(Integer userId);

    List<UserNotification> findTop100ByUserIdAndTypeOrderByCreateTimeDesc(Integer userId, String type);

    long countByUserIdAndReadFlag(Integer userId, Integer readFlag);

    Optional<UserNotification> findFirstByUserIdAndTypeAndActorIdAndTargetId(Integer userId, String type,
                                                                             Integer actorId, Integer targetId);

    @Modifying
    @Query("update UserNotification n set n.readFlag = 1 where n.userId = :userId and n.readFlag = 0")
    int markAllRead(@Param("userId") Integer userId);

    @Modifying
    @Query("update UserNotification n set n.readFlag = 1 where n.userId = :userId and n.notificationId in :ids")
    int markReadByIds(@Param("userId") Integer userId, @Param("ids") Collection<Integer> ids);

    /** 取消点赞等场景：撤掉对应的互动通知，避免噪声 */
    long deleteByUserIdAndTypeAndActorIdAndTargetId(Integer userId, String type, Integer actorId, Integer targetId);
}
