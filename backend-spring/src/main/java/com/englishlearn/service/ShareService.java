package com.englishlearn.service;

import com.englishlearn.common.TimeUtil;
import com.englishlearn.entity.ShareRecord;
import com.englishlearn.entity.User;
import com.englishlearn.repository.ShareRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 内容分享服务：不接入任何开放平台 SDK，只做「复制链接 + 渠道标记 + 留痕统计」。
 * 渠道：weibo / xiaohongshu / wechat / copy。
 */
@Service
public class ShareService {

    private static final Set<String> CHANNELS = Set.of("weibo", "xiaohongshu", "wechat", "copy");
    private static final Set<String> CONTENT_TYPES = Set.of("post", "scene", "resource", "achievement");

    /** 前端站点基础路径（哈希路由） */
    private static final String SITE_PREFIX = "/#/";

    private final ShareRecordRepository shareRepository;

    public ShareService(ShareRecordRepository shareRepository) {
        this.shareRepository = shareRepository;
    }

    @Transactional
    public Map<String, Object> record(User user, String contentType, Integer contentId, String channel, String url) {
        String type = contentType == null || contentType.isBlank() ? "post" : contentType.strip();
        String ch = channel == null || channel.isBlank() ? "copy" : channel.strip();
        String shareUrl = url;
        if (shareUrl == null || shareUrl.isBlank()) {
            shareUrl = SITE_PREFIX + defaultPath(type, contentId);
        }
        ShareRecord record = new ShareRecord();
        record.userId = user.userId;
        record.contentType = CONTENT_TYPES.contains(type) ? type : "post";
        record.contentId = contentId;
        record.channel = CHANNELS.contains(ch) ? ch : "copy";
        record.shareUrl = shareUrl.length() > 500 ? shareUrl.substring(0, 500) : shareUrl;
        shareRepository.save(record);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("shareUrl", record.shareUrl);
        data.put("channel", record.channel);
        data.put("total", shareRepository.countByUserId(user.userId));
        return data;
    }

    public List<Map<String, Object>> myShares(User user) {
        return shareRepository.findTop20ByUserIdOrderByCreateTimeDesc(user.userId).stream()
                .map(this::payload).toList();
    }

    private String defaultPath(String contentType, Integer contentId) {
        if (contentId == null) {
            return "community";
        }
        return switch (contentType) {
            case "scene" -> "scene/" + contentId;
            case "resource" -> "resource/" + contentId;
            case "achievement" -> "profile";
            default -> "community/post/" + contentId;
        };
    }

    private Map<String, Object> payload(ShareRecord r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.shareId);
        m.put("contentType", r.contentType);
        m.put("contentId", r.contentId);
        m.put("channel", r.channel);
        m.put("shareUrl", r.shareUrl);
        m.put("createTime", TimeUtil.iso(r.createTime));
        return m;
    }
}
