package com.englishlearn.dto;

/**
 * 管理后台请求体。
 */
public final class AdminDtos {

    private AdminDtos() {
    }

    public record SceneUpdateIn(String sceneName, String sceneCategory, String sceneDesc,
                                String levelScope, String coverUrl, Integer status) {}

    public record ResourceIn(String title, String type, String category, String level,
                             String mediaUrl, Integer durationSec, Integer status) {}

    public record ResourceUpdateIn(String title, String type, String category, String level,
                                   String mediaUrl, Integer durationSec, Integer status) {}

    public record PostReviewIn(Integer status, Boolean isTop) {}

    public record UserUpdateIn(String nickname, Integer status, String userRole) {}
}