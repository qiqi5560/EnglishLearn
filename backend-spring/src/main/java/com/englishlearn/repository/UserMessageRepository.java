package com.englishlearn.repository;

import com.englishlearn.entity.UserMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * 站内私信仓储：会话聚合依赖「单查询 + 内存分组」，避免 SQLite 上的复杂子查询与 N+1。
 */
public interface UserMessageRepository extends JpaRepository<UserMessage, Integer> {

    /** 当前用户参与的最近消息（用于聚合成会话列表） */
    @Query("select m from UserMessage m where m.senderId = :me or m.receiverId = :me order by m.createTime desc")
    List<UserMessage> recentOfUser(@Param("me") Integer me, Pageable pageable);

    /** 与某个用户的双向会话历史（倒序取，由调用方翻转） */
    @Query("select m from UserMessage m where (m.senderId = :a and m.receiverId = :b) or (m.senderId = :b and m.receiverId = :a) order by m.createTime desc")
    List<UserMessage> conversation(@Param("a") Integer a, @Param("b") Integer b, Pageable pageable);

    long countByReceiverIdAndReadFlag(Integer receiverId, Integer readFlag);

    /** 批量统计各发送者的未读数（senders 为空时调用方需跳过，避免 in () 语法错误） */
    @Query("select m.senderId, count(m) from UserMessage m where m.receiverId = :me and m.readFlag = 0 and m.senderId in :senders group by m.senderId")
    List<Object[]> countUnreadGroupBySender(@Param("me") Integer me, @Param("senders") Collection<Integer> senders);

    @Modifying
    @Query("update UserMessage m set m.readFlag = 1 where m.receiverId = :me and m.senderId = :peer and m.readFlag = 0")
    int markConversationRead(@Param("me") Integer me, @Param("peer") Integer peer);
}
