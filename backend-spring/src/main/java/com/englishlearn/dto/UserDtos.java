package com.englishlearn.dto;

/**
 * 个人中心请求体。
 */
public final class UserDtos {

    private UserDtos() {
    }

    public record UpdateMeIn(String nickname, String avatarUrl, String bio, String ageGroup, Integer guardianId) {}

    public record PasswordIn(String oldPassword, String newPassword) {}

    public record BindGuardianIn(String guardianPhone, String code) {}

    public record AddPartnerIn(String phone) {}
}