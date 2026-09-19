package com.englishlearn.dto;

/**
 * 管理后台请求体。
 */
public final class AdminDtos {

    private AdminDtos() {
    }

    public record SceneUpdateIn(String sceneName, String sceneCategory, String sceneDesc,
                                String levelScope, String role, String script,
                                String coverUrl, Integer status) {}

    /** 场景新增（管理员维护场景库，保存后所有用户立即可见） */
    public record SceneIn(String sceneName, String sceneCategory, String sceneDesc,
                          String levelScope, String role, String script, String coverUrl, Integer status) {}

    /** 发帖处罚：days 为处罚天数（1 天起），reason 为处罚原因 */
    public record BanIn(Integer days, String reason) {}

    public record ResourceIn(String title, String type, String category, String level,
                             String mediaUrl, Integer durationSec, Integer status) {}

    public record ResourceUpdateIn(String title, String type, String category, String level,
                                   String mediaUrl, Integer durationSec, Integer status) {}

    public record PostReviewIn(Integer status, Boolean isTop) {}

    public record UserUpdateIn(String nickname, Integer status, String userRole) {}

    /** 管理员重置用户密码；password 为空时重置为默认 123456 */
    public record ResetPasswordIn(String password) {}

    /** 名句素材新增 / 更新（管理员维护内置素材库） */
    public record QuoteIn(String title, String source, String category, String level,
                          String textEn, String textZh) {}
}