package com.englishlearn.dto;

import java.util.List;

/**
 * 学习方案与测评请求体。
 */
public final class PlanDtos {

    private PlanDtos() {
    }

    public record EntranceAnswer(int id, String text) {}

    public record EntranceTestIn(List<EntranceAnswer> answers, String targetGoal) {}

    public record GeneratePlanIn(String targetGoal, Integer dailyMinutes) {}

    public record TaskUpdateIn(Boolean done) {}
}