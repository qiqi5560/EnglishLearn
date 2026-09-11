package com.englishlearn.dto;

/**
 * 对话会话请求体。
 */
public final class DialogueDtos {

    private DialogueDtos() {
    }

    public record SessionCreateIn(Integer sceneId, String mode) {}

    public record MessageIn(String content) {}
}