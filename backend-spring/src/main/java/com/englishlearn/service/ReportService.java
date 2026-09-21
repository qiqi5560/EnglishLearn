package com.englishlearn.service;

import com.englishlearn.common.JsonUtil;
import com.englishlearn.common.TimeUtil;
import com.englishlearn.entity.AssessmentRecord;
import com.englishlearn.entity.ConversationSession;
import com.englishlearn.entity.LearningPlan;
import com.englishlearn.entity.StudyRecord;
import com.englishlearn.entity.User;
import com.englishlearn.repository.AssessmentRecordRepository;
import com.englishlearn.repository.ConversationSessionRepository;
import com.englishlearn.repository.LearningPlanRepository;
import com.englishlearn.repository.StudyRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 学习报表服务（F007）：成长曲线 + 能力雷达 + 统计聚合。
 */
@Service
public class ReportService {

    private static final List<Map<String, String>> RADAR_INDICATORS = List.of(
            Map.of("name", "发音", "key", "pron_score"),
            Map.of("name", "流利度", "key", "fluency_score"),
            Map.of("name", "反应", "key", "reaction_score"),
            Map.of("name", "自然度", "key", "natural_score"));

    private final StudyRecordRepository studyRecordRepository;
    private final ConversationSessionRepository sessionRepository;
    private final AssessmentRecordRepository assessmentRepository;
    private final LearningPlanRepository planRepository;

    public ReportService(StudyRecordRepository studyRecordRepository,
                         ConversationSessionRepository sessionRepository,
                         AssessmentRecordRepository assessmentRepository,
                         LearningPlanRepository planRepository) {
        this.studyRecordRepository = studyRecordRepository;
        this.sessionRepository = sessionRepository;
        this.assessmentRepository = assessmentRepository;
        this.planRepository = planRepository;
    }

    public Map<String, Object> overview(User user) {
        LocalDate today = LocalDate.now();
        int weekIndex = today.getDayOfWeek().getValue() - 1; // Monday=0
        LocalDate since = today.minusDays((8L - 1) * 7 + weekIndex);

        List<StudyRecord> records = studyRecordRepository.findByUserIdOrderByLearnDate(user.userId);

        // 1) 成长曲线：按学习日聚合平均综合得分
        Map<LocalDate, List<Double>> grouped = new LinkedHashMap<>();
        for (StudyRecord r : records) {
            if (r.score != null) {
                grouped.computeIfAbsent(r.learnDate, k -> new ArrayList<>()).add(r.score);
            }
        }
        Map<LocalDate, Double> byDay = new LinkedHashMap<>();
        for (Map.Entry<LocalDate, List<Double>> e : grouped.entrySet()) {
            byDay.put(e.getKey(), e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0));
        }

        // 2) 累计统计：练习次数 = 已完成的对话练习 + 名句跟读次数
        int totalMinutes = records.stream().mapToInt(r -> r.durationMin == null ? 0 : r.durationMin).sum();
        long totalSessions = sessionRepository.countByUserIdAndSessionStatus(user.userId, "finished")
                + studyRecordRepository.countByUserIdAndActionType(user.userId, "reading");
        Set<LocalDate> activeDays = new LinkedHashSet<>();
        for (StudyRecord r : records) {
            if (r.learnDate != null) activeDays.add(r.learnDate);
        }
        long avgScoreNotNull = records.stream().filter(r -> r.score != null).count();
        Double avgScore = avgScoreNotNull > 0
                ? records.stream().filter(r -> r.score != null).mapToDouble(r -> r.score).average().orElse(0)
                : null;

        LearningPlan plan = planRepository.findByUserIdAndPlanStatus(user.userId, "active").orElse(null);

        // 3) 能力雷达：最近 20 次评测（对话逐句 + 名句跟读）四维均值，更平滑也更全面
        List<AssessmentRecord> merged = new ArrayList<>();
        merged.addAll(assessmentRepository.latest(user.userId, PageRequest.of(0, 20)));
        merged.addAll(assessmentRepository.findTop20ByUserIdOrderByAssessIdDesc(user.userId));
        merged.sort((a, b) -> Integer.compare(b.assessId, a.assessId));
        if (merged.size() > 20) merged = merged.subList(0, 20);
        List<Double> radarValues = List.of(
                avgDim(merged, a -> a.pronScore),
                avgDim(merged, a -> a.fluencyScore),
                avgDim(merged, a -> a.reactionScore),
                avgDim(merged, a -> a.naturalScore));

        // 4) 最近完成会话
        List<ConversationSession> recent = sessionRepository.findTop5ByUserIdAndSessionStatusOrderByEndTimeDesc(user.userId, "finished");
        List<Map<String, Object>> recentList = new ArrayList<>();
        for (ConversationSession s : recent) {
            Map<String, Object> summary = JsonUtil.parseMap(s.aiSummary);
            double total = summary != null && summary.get("total") != null
                    ? Double.parseDouble(String.valueOf(summary.get("total"))) : 0.0;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sessionId", s.sessionId);
            item.put("sceneName", s.scene != null ? s.scene.sceneName : "自由对话");
            item.put("total", total);
            item.put("endTime", TimeUtil.iso(s.endTime));
            recentList.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> growth = new LinkedHashMap<>();
        growth.put("dates", byDay.keySet().stream().map(LocalDate::toString).toList());
        growth.put("scores", new ArrayList<>(byDay.values()));
        growth.put("startDate", since.toString());
        data.put("growth", growth);

        Map<String, Object> radar = new LinkedHashMap<>();
        radar.put("indicators", RADAR_INDICATORS.stream()
                .map(i -> Map.of("name", i.get("name"), "max", 100)).toList());
        radar.put("values", radarValues);
        data.put("radar", radar);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalMinutes", totalMinutes);
        stats.put("totalSessions", (int) totalSessions);
        stats.put("activeDays", activeDays.size());
        stats.put("avgScore", avgScore == null ? null : round1(avgScore));
        stats.put("currentLevel", plan != null ? plan.levelCurrent : null);
        data.put("stats", stats);
        data.put("recentSessions", recentList);
        return data;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    /** 对一组评测的某一维求均值（空值跳过），无数据返回 0 */
    private static double avgDim(List<AssessmentRecord> records, java.util.function.Function<AssessmentRecord, Double> dim) {
        return records.stream()
                .map(dim)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
}