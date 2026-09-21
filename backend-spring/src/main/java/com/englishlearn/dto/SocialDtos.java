package com.englishlearn.dto;

import java.util.List;

/**
 * 社交功能请求体：站内信、通知已读、内容分享。
 */
public final class SocialDtos {

    private SocialDtos() {
    }

    public record SendMessageIn(Integer peerId, String content) {}

    public record ReadMessageIn(Integer peerId) {}

    /** ids 为空表示全部标记已读 */
    public record ReadNotificationIn(List<Integer> ids) {}

    public record ShareIn(String contentType, Integer contentId, String channel, String url) {}
}
