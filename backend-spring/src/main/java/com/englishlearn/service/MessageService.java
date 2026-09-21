package com.englishlearn.service;

import com.englishlearn.common.ApiException;
import com.englishlearn.common.TimeUtil;
import com.englishlearn.entity.User;
import com.englishlearn.entity.UserMessage;
import com.englishlearn.repository.UserMessageRepository;
import com.englishlearn.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 站内私信服务：会话列表、消息历史、发送、已读标记、未读计数。
 * 会话列表采用「一次扫描最近消息 + 内存按 peer 聚合 + 批量查用户与未读数」，固定 3 次查询避免 N+1。
 */
@Service
public class MessageService {

    private static final int MAX_CONTENT = 500;
    private static final int RECENT_SCAN = 200;

    private final UserMessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageService(UserMessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    public List<Map<String, Object>> conversations(User user) {
        List<UserMessage> recent = messageRepository.recentOfUser(user.userId, PageRequest.of(0, RECENT_SCAN));
        Map<Integer, UserMessage> latest = new LinkedHashMap<>();
        for (UserMessage m : recent) {
            Integer peer = user.userId.equals(m.senderId) ? m.receiverId : m.senderId;
            if (peer == null) {
                continue;
            }
            // recent 已按时间倒序，首次出现即该会话的最新一条
            latest.putIfAbsent(peer, m);
        }
        Set<Integer> peerIds = latest.keySet();
        Map<Integer, Long> unread = new HashMap<>();
        if (!peerIds.isEmpty()) {
            for (Object[] row : messageRepository.countUnreadGroupBySender(user.userId, peerIds)) {
                if (row[0] == null) {
                    continue;
                }
                unread.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
            }
        }
        Map<Integer, User> peers = peerIds.isEmpty() ? Map.of()
                : userRepository.findAllById(peerIds).stream()
                        .collect(Collectors.toMap(u -> u.userId, Function.identity(), (a, b) -> a));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Integer, UserMessage> e : latest.entrySet()) {
            User peer = peers.get(e.getKey());
            UserMessage last = e.getValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("peerId", e.getKey());
            m.put("peerName", peer != null && peer.nickname != null ? peer.nickname : "用户");
            m.put("peerAvatar", peer != null ? peer.avatarUrl : null);
            m.put("lastMessage", last.content);
            m.put("lastMine", user.userId.equals(last.senderId));
            m.put("lastTime", TimeUtil.iso(last.createTime));
            m.put("unread", unread.getOrDefault(e.getKey(), 0L));
            list.add(m);
        }
        return list;
    }

    @Transactional
    public Map<String, Object> history(User user, Integer peerId, Integer limit) {
        User peer = requirePeer(user, peerId);
        messageRepository.markConversationRead(user.userId, peer.userId);
        int size = limit == null || limit < 1 ? 50 : Math.min(limit, 100);
        List<UserMessage> rows = new ArrayList<>(
                messageRepository.conversation(user.userId, peer.userId, PageRequest.of(0, size)));
        Collections.reverse(rows);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("peer", peerPayload(peer));
        data.put("list", rows.stream().map(m -> messagePayload(m, user.userId)).toList());
        return data;
    }

    @Transactional
    public Map<String, Object> send(User user, Integer peerId, String content) {
        User peer = requirePeer(user, peerId);
        if (content == null || content.isBlank()) {
            throw new ApiException(422, "消息内容不能为空");
        }
        String text = content.strip();
        if (text.length() > MAX_CONTENT) {
            throw new ApiException(422, "私信最多 " + MAX_CONTENT + " 字");
        }
        UserMessage m = new UserMessage();
        m.senderId = user.userId;
        m.receiverId = peer.userId;
        m.content = text;
        m.readFlag = 0;
        return messagePayload(messageRepository.save(m), user.userId);
    }

    @Transactional
    public int markRead(User user, Integer peerId) {
        User peer = requirePeer(user, peerId);
        return messageRepository.markConversationRead(user.userId, peer.userId);
    }

    public long unreadCount(User user) {
        return messageRepository.countByReceiverIdAndReadFlag(user.userId, 0);
    }

    private User requirePeer(User user, Integer peerId) {
        if (peerId == null) {
            throw new ApiException(422, "缺少会话对象");
        }
        if (peerId.equals(user.userId)) {
            throw new ApiException(422, "不能与自己发起会话");
        }
        return userRepository.findById(peerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    private Map<String, Object> peerPayload(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", u.userId);
        m.put("nickname", u.nickname != null ? u.nickname : "用户");
        m.put("avatarUrl", u.avatarUrl);
        return m;
    }

    private Map<String, Object> messagePayload(UserMessage m, Integer me) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", m.messageId);
        row.put("senderId", m.senderId);
        row.put("receiverId", m.receiverId);
        row.put("content", m.content);
        row.put("mine", m.senderId.equals(me));
        row.put("read", m.readFlag != null && m.readFlag != 0);
        row.put("createTime", TimeUtil.iso(m.createTime));
        return row;
    }
}
