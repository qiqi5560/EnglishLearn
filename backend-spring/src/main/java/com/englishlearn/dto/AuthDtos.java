package com.englishlearn.dto;

/**
 * 账号与鉴权请求体（字段与原 Pydantic 模型一致）。
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record SendCodeIn(String phone) {}

    public record LoginIn(String phone, String code, String password, String nickname, String ageGroup) {}

    public record RegisterIn(String phone, String password, String nickname, String ageGroup) {}

    public record AdminLoginIn(String phone, String password) {}
}