package com.englishlearn.dto;

/**
 * 社区请求体。
 */
public final class CommunityDtos {

    private CommunityDtos() {
    }

    public record PostCreateIn(String title, String content, String topic) {}

    public record CommentIn(String content) {}
}