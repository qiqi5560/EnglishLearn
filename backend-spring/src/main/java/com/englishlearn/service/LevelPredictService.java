package com.englishlearn.service;

import com.englishlearn.common.JsonUtil;
import com.englishlearn.entity.AssessmentRecord;
import com.englishlearn.entity.ConversationSession;
import com.englishlearn.entity.LearningPlan;
import com.englishlearn.entity.SysConfig;
import com.englishlearn.entity.User;
import com.englishlearn.ml.SoftmaxRegression;
import com.englishlearn.repository.AssessmentRecordRepository;
import com.englishlearn.repository.ConversationMessageRepository;
import com.englishlearn.repository.ConversationSessionRepository;
import com.englishlearn.repository.LearningPlanRepository;
import com.englishlearn.repository.SysConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 口语水平预测服务。
 *
 * <p>以「会话」为样本粒度，从 {@link AssessmentRecord} 四维评分与会话行为（时长、发言数）
 * 构造特征，用 Softmax 回归预测用户所属的口语水平档位（初级 / 中级 / 高级），
 * 并映射回系统既有的 CEFR 等级（A1~C2）。
 *
 * <p>标签由历史评测分加权分箱自动生成，无需人工标注；训练样本不足时自动降级为规则分箱，
 * 保证任何数据量下接口都返回合理结果。
 */
@Service
public class LevelPredictService {

    private static final Logger log = LoggerFactory.getLogger(LevelPredictService.class);

    /** CEFR 等级顺序，用于计算等级距离 */
    public static final List<String> CEFR_ORDER = List.of("A1", "A2", "B1", "B2", "C1", "C2");

    /** 三档水平：初级 / 中级 / 高级 */
    public static final List<String> BANDS = List.of("初级", "中级", "高级");

    public static final List<String> FEATURE_NAMES = List.of(
            "pron", "fluency", "reaction", "natural", "durationSec", "userMsgCount", "bias");

    private static final String MODEL_KEY = "ml_level_model";
    private static final String FEATURE_VERSION = "v1";
    private static final int MIN_SAMPLES = 20;
    private static final int RECENT_SESSIONS = 5;
    private static final int DIM = 7;

    /** 四维分权重，必须与 DialogueService 中 result.total 的计算保持一致 */
    private static final double W_PRON = 0.35;
    private static final double W_FLUENCY = 0.25;
    private static final double W_REACTION = 0.2;
    private static final double W_NATURAL = 0.2;

    private static final double[] SCORE_WEIGHTS = {W_PRON, W_FLUENCY, W_REACTION, W_NATURAL};

    private final ConversationSessionRepository sessionRepository;
    private final ConversationMessageRepository messageRepository;
    private final AssessmentRecordRepository assessmentRepository;
    private final LearningPlanRepository planRepository;
    private final SysConfigRepository configRepository;

    private volatile SoftmaxRegression model;

    public LevelPredictService(ConversationSessionRepository sessionRepository,
                               ConversationMessageRepository messageRepository,
                               AssessmentRecordRepository assessmentRepository,
                               LearningPlanRepository planRepository,
                               SysConfigRepository configRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.assessmentRepository = assessmentRepository;
        this.planRepository = planRepository;
        this.configRepository = configRepository;
    }

    /** 水平预测结果 */
    public record LevelPrediction(String band, String level, double[] probs, double confidence,
                                  double score, String source, int sampleCount,
                                  Map<String, Object> features) {

        public Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("band", band);
            out.put("level", level);
            out.put("score", round1(score));
            out.put("confidence", round1(confidence * 100));
            List<Double> ps = new ArrayList<>(probs.length);
            for (double p : probs) {
                ps.add(round1(p * 100));
            }
            out.put("probs", ps);
            out.put("bands", BANDS);
            out.put("source", source);
            out.put("sampleCount", sampleCount);
            out.put("features", features);
            return out;
        }
    }

    /** 单个会话的聚合特征 */
    private record SessionFeature(double[] vector, double score) {}

    // ------------------------------------------------------------------ 预测

    /** 预测用户当前口语水平；无任何练习数据时依次回退到学习计划等级、A2 */
    public LevelPrediction predict(User user) {
        List<ConversationSession> sessions =
                sessionRepository.findTop20ByUserIdOrderByStartTimeDesc(user.userId);

        List<double[]> recent = new ArrayList<>();
        for (ConversationSession session : sessions) {
            if (recent.size() >= RECENT_SESSIONS) {
                break;
            }
            SessionFeature feature = aggregate(session);
            if (feature != null) {
                recent.add(feature.vector());
            }
        }

        if (recent.isEmpty()) {
            return fallbackFromPlan(user);
        }

        double[] x = average(recent);
        double score = scoreOf(x);

        SoftmaxRegression trained = model();
        double[] probs;
        String source;
        if (trained != null) {
            probs = trained.predictProba(x);
            source = "model";
        } else {
            probs = ruleProbs(bandIndexOf(score));
            source = "rule";
        }

        int bandIdx = argMax(probs);
        Map<String, Object> features = new LinkedHashMap<>();
        features.put("pron", round1(x[0] * 100));
        features.put("fluency", round1(x[1] * 100));
        features.put("reaction", round1(x[2] * 100));
        features.put("natural", round1(x[3] * 100));
        features.put("avgDurationSec", (int) Math.round(x[4] * 600));
        features.put("avgUserMessages", round1(x[5] * 10));
        features.put("sessions", recent.size());

        return new LevelPrediction(BANDS.get(bandIdx), cefrOf(score), probs, probs[bandIdx],
                score, source, recent.size(), features);
    }

    private LevelPrediction fallbackFromPlan(User user) {
        LearningPlan plan = planRepository.findByUserIdAndPlanStatus(user.userId, "active").orElse(null);
        String level = plan != null && plan.levelCurrent != null && !plan.levelCurrent.isBlank()
                ? plan.levelCurrent.trim().toUpperCase(Locale.ROOT)
                : "A2";
        if (cefrIndex(level) < 0) {
            level = "A2";
        }
        int bandIdx = bandIndexOf(level);
        double[] probs = ruleProbs(bandIdx);
        Map<String, Object> features = new LinkedHashMap<>();
        features.put("note", "尚无练习数据，取入学测评等级");
        return new LevelPrediction(BANDS.get(bandIdx), level, probs, probs[bandIdx],
                typicalScore(level), plan != null ? "plan" : "default", 0, features);
    }

    /** 聚合单个会话：四维分均值 + 时长 + 用户发言数，全部归一化到 0~1，末位为偏置 */
    private SessionFeature aggregate(ConversationSession session) {
        List<AssessmentRecord> records =
                assessmentRepository.findBySessionIdOrderByAssessId(session.sessionId);
        if (records == null || records.isEmpty()) {
            return null;
        }
        double pron = records.stream().mapToDouble(a -> nz(a.pronScore)).average().orElse(0);
        double fluency = records.stream().mapToDouble(a -> nz(a.fluencyScore)).average().orElse(0);
        double reaction = records.stream().mapToDouble(a -> nz(a.reactionScore)).average().orElse(0);
        double natural = records.stream().mapToDouble(a -> nz(a.naturalScore)).average().orElse(0);

        int durationSec = session.durationSec == null ? 0 : Math.max(0, session.durationSec);
        long userMessages = messageRepository.countBySessionIdAndSpeaker(session.sessionId, "user");

        double[] x = new double[DIM];
        x[0] = clamp01(pron / 100.0);
        x[1] = clamp01(fluency / 100.0);
        x[2] = clamp01(reaction / 100.0);
        x[3] = clamp01(natural / 100.0);
        x[4] = Math.min(durationSec / 600.0, 1.0);
        x[5] = Math.min(userMessages / 10.0, 1.0);
        x[6] = 1.0;

        double score = pron * W_PRON + fluency * W_FLUENCY + reaction * W_REACTION + natural * W_NATURAL;
        return new SessionFeature(x, score);
    }

    // ------------------------------------------------------------------ 训练

    /**
     * 训练水平预测模型。样本为「有评测记录的会话」，标签由综合评分分箱自动生成。
     * 注意：不加事务，避免长时间占用 SQLite 写锁。
     */
    public Map<String, Object> train() {
        Map<String, Object> result = new LinkedHashMap<>();

        List<AssessmentRecord> all = assessmentRepository.findAll();
        Map<Integer, List<AssessmentRecord>> bySession = new LinkedHashMap<>();
        for (AssessmentRecord record : all) {
            bySession.computeIfAbsent(record.sessionId, k -> new ArrayList<>()).add(record);
        }

        List<ConversationSession> sessions = sessionRepository.findAllById(bySession.keySet());
        List<Integer> sessionIds = sessions.stream().map(s -> s.sessionId).toList();
        Map<Integer, Long> userMessages = new HashMap<>();
        if (!sessionIds.isEmpty()) {
            for (Object[] row : messageRepository.countUserMessages(sessionIds)) {
                userMessages.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
            }
        }

        List<double[]> xs = new ArrayList<>();
        List<Integer> ys = new ArrayList<>();
        for (ConversationSession session : sessions) {
            SessionFeature feature = aggregate(session);
            if (feature == null) {
                continue;
            }
            xs.add(feature.vector());
            ys.add(bandIndexOf(feature.score()));
        }

        int sampleCount = xs.size();
        result.put("sampleCount", sampleCount);
        result.put("distribution", distributionOf(ys));

        if (sampleCount < MIN_SAMPLES) {
            result.put("trained", false);
            result.put("reason", "训练样本不足：需要至少 " + MIN_SAMPLES
                    + " 条有评测记录的会话，当前仅 " + sampleCount + " 条");
            return result;
        }
        long distinct = ys.stream().distinct().count();
        if (distinct < BANDS.size()) {
            result.put("trained", false);
            result.put("reason", "训练样本仅覆盖 " + distinct + " 个水平档位，需要三档齐全才能训练");
            return result;
        }

        SoftmaxRegression trained = new SoftmaxRegression(BANDS.size(), DIM);
        trained.fit(xs, ys);
        model = trained;
        persist(trained, sampleCount);

        result.put("trained", true);
        result.put("accuracy", round2(trained.getAccuracy() * 100));
        result.put("trainCount", trained.getTrainCount());
        result.put("testCount", trained.getTestCount());
        result.put("trainedAt", LocalDateTime.now().toString());
        return result;
    }

    /** 模型状态，供管理端展示 */
    public Map<String, Object> modelStatus() {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> payload = loadPayload();
        SoftmaxRegression trained = model();
        out.put("ready", trained != null);
        out.put("source", trained != null ? "model" : "rule");
        out.put("bands", BANDS);
        out.put("minSamples", MIN_SAMPLES);
        if (payload != null) {
            out.put("trainedAt", payload.get("trainedAt"));
            out.put("sampleCount", payload.get("sampleCount"));
            out.put("featureNames", payload.get("featureNames"));
        }
        if (trained != null) {
            out.put("accuracy", round2(trained.getAccuracy() * 100));
            out.put("trainCount", trained.getTrainCount());
            out.put("testCount", trained.getTestCount());
        }
        return out;
    }

    // ------------------------------------------------------- CEFR / 档位工具

    /** CEFR 等级下标，未知返回 -1 */
    public static int cefrIndex(String level) {
        if (level == null) {
            return -1;
        }
        return CEFR_ORDER.indexOf(level.trim().toUpperCase(Locale.ROOT));
    }

    /** 综合评分 → CEFR 等级（沿用系统既有分箱：A1&lt;40、A2&lt;55、B1&lt;70、B2&lt;80、C1&lt;90、C2） */
    public static String cefrOf(double score) {
        if (score < 40) {
            return "A1";
        }
        if (score < 55) {
            return "A2";
        }
        if (score < 70) {
            return "B1";
        }
        if (score < 80) {
            return "B2";
        }
        if (score < 90) {
            return "C1";
        }
        return "C2";
    }

    /** 综合评分 → 三档下标（初级 &lt;55、中级 &lt;80、高级 ≥80） */
    public static int bandIndexOf(double score) {
        if (score < 55) {
            return 0;
        }
        if (score < 80) {
            return 1;
        }
        return 2;
    }

    /** CEFR 等级 → 三档下标：A1/A2=初级，B1/B2=中级，C1/C2=高级 */
    public static int bandIndexOf(String level) {
        int index = cefrIndex(level);
        if (index < 0) {
            return 0;
        }
        return Math.min(index / 2, BANDS.size() - 1);
    }

    /** CEFR 等级 → 三档中文名 */
    public static String bandOf(String level) {
        return BANDS.get(bandIndexOf(level));
    }

    /**
     * 预测等级与候选等级的匹配度，取值 0~1。
     * 候选等级支持单个（B1）或区间（A2-B1）写法；无法解析时返回中性值 0.5。
     */
    public static double levelMatch(String predicted, String candidate) {
        int p = cefrIndex(predicted);
        if (p < 0 || candidate == null || candidate.isBlank()) {
            return 0.5;
        }
        int best = Integer.MAX_VALUE;
        for (String part : candidate.split("[-~—至]")) {
            int index = cefrIndex(part.trim());
            if (index >= 0) {
                best = Math.min(best, Math.abs(p - index));
            }
        }
        return best == Integer.MAX_VALUE ? 0.5 : distanceScore(best);
    }

    private static double distanceScore(int diff) {
        return switch (diff) {
            case 0 -> 1.0;
            case 1 -> 0.6;
            case 2 -> 0.35;
            case 3 -> 0.2;
            default -> 0.1;
        };
    }

    private static double typicalScore(String level) {
        return switch (level) {
            case "A1" -> 30.0;
            case "A2" -> 47.0;
            case "B1" -> 62.0;
            case "B2" -> 74.0;
            case "C1" -> 84.0;
            default -> 92.0;
        };
    }

    private static double[] ruleProbs(int bandIndex) {
        double[] probs = new double[BANDS.size()];
        for (int i = 0; i < probs.length; i++) {
            probs[i] = 0.05;
        }
        probs[bandIndex] = 1.0 - 0.05 * (probs.length - 1);
        return probs;
    }

    private Map<String, Object> distributionOf(List<Integer> ys) {
        Map<String, Object> dist = new LinkedHashMap<>();
        for (int i = 0; i < BANDS.size(); i++) {
            int band = i;
            dist.put(BANDS.get(i), ys.stream().filter(y -> y == band).count());
        }
        return dist;
    }

    // ------------------------------------------------------------ 模型持久化

    private SoftmaxRegression model() {
        if (model != null) {
            return model;
        }
        synchronized (this) {
            if (model != null) {
                return model;
            }
            try {
                Map<String, Object> payload = loadPayload();
                if (payload == null || !FEATURE_VERSION.equals(payload.get("version"))) {
                    return null;
                }
                model = SoftmaxRegression.fromMap(payload);
                log.info("已加载口语水平预测模型，样本 {} 条", payload.get("sampleCount"));
            } catch (Exception e) {
                log.warn("加载口语水平预测模型失败，转为规则模式", e);
                model = null;
            }
            return model;
        }
    }

    private Map<String, Object> loadPayload() {
        SysConfig config = configRepository.findByKey(MODEL_KEY).orElse(null);
        if (config == null || config.value == null || config.value.isBlank()) {
            return null;
        }
        Map<String, Object> payload = JsonUtil.parseMap(config.value);
        if (payload == null) {
            return null;
        }
        Object weights = payload.get("weights");
        if (weights == null) {
            return null;
        }
        return payload;
    }

    private void persist(SoftmaxRegression trained, int sampleCount) {
        SysConfig config = configRepository.findByKey(MODEL_KEY).orElseGet(() -> {
            SysConfig created = new SysConfig();
            created.key = MODEL_KEY;
            created.remark = "口语水平预测模型（Softmax 回归）权重";
            return created;
        });
        Map<String, Object> payload = trained.toMap();
        payload.put("version", FEATURE_VERSION);
        payload.put("trainedAt", LocalDateTime.now().toString());
        payload.put("sampleCount", sampleCount);
        payload.put("bands", BANDS);
        payload.put("featureNames", FEATURE_NAMES);
        config.value = JsonUtil.toJson(payload);
        configRepository.save(config);
    }

    // ------------------------------------------------------------------ 工具

    private static double[] average(List<double[]> vectors) {
        double[] out = new double[DIM];
        for (double[] v : vectors) {
            for (int i = 0; i < DIM; i++) {
                out[i] += v[i];
            }
        }
        for (int i = 0; i < DIM; i++) {
            out[i] /= vectors.size();
        }
        return out;
    }

    private static double scoreOf(double[] x) {
        return (x[0] * SCORE_WEIGHTS[0] + x[1] * SCORE_WEIGHTS[1]
                + x[2] * SCORE_WEIGHTS[2] + x[3] * SCORE_WEIGHTS[3]) * 100;
    }

    private static int argMax(double[] values) {
        int best = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[best]) {
                best = i;
            }
        }
        return best;
    }

    private static double nz(Double value) {
        return value == null ? 0.0 : value;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private static double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
