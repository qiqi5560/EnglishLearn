package com.englishlearn.service;

import com.englishlearn.common.JsonUtil;
import com.englishlearn.dto.Dtos;
import com.englishlearn.entity.ConversationSession;
import com.englishlearn.entity.LearningResource;
import com.englishlearn.entity.Scene;
import com.englishlearn.entity.SysConfig;
import com.englishlearn.entity.User;
import com.englishlearn.repository.ConversationSessionRepository;
import com.englishlearn.repository.DailyTaskRepository;
import com.englishlearn.repository.LearningResourceRepository;
import com.englishlearn.repository.SceneRepository;
import com.englishlearn.repository.SysConfigRepository;
import com.englishlearn.service.LevelPredictService.LevelPrediction;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 个性化推荐服务。
 *
 * <p>三种策略按系统配置（system_config.recommend.content / collab / model）加权融合：
 * <ul>
 *   <li>content —— 候选项 CEFR 等级与预测水平的匹配度；</li>
 *   <li>collab —— Item-Based 协同过滤（余弦相似度），基于「用户-场景」隐式反馈矩阵；</li>
 *   <li>model —— 模型预测的三档概率加权匹配度。</li>
 * </ul>
 * 融合后叠加业务微调：难度略高加分、今日已有降权、反复练习降权。
 */
@Service
public class RecommendService {

    private static final String SYSTEM_CONFIG_KEY = "system_config";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    /** 三档水平的代表等级，用于把概率分布折算成等级匹配度 */
    private static final List<String> BAND_REPRESENTATIVE = List.of("A2", "B1", "C1");

    private final SceneRepository sceneRepository;
    private final LearningResourceRepository resourceRepository;
    private final ConversationSessionRepository sessionRepository;
    private final DailyTaskRepository taskRepository;
    private final SysConfigRepository configRepository;

    private volatile CachedMatrix matrixCache;

    public RecommendService(SceneRepository sceneRepository,
                            LearningResourceRepository resourceRepository,
                            ConversationSessionRepository sessionRepository,
                            DailyTaskRepository taskRepository,
                            SysConfigRepository configRepository) {
        this.sceneRepository = sceneRepository;
        this.resourceRepository = resourceRepository;
        this.sessionRepository = sessionRepository;
        this.taskRepository = taskRepository;
        this.configRepository = configRepository;
    }

    /** 带分项得分的候选，用于排序与解释推荐理由 */
    private record Scored<T>(T item, double score, double content, double collab, double model) {}

    /** 协同过滤矩阵缓存：用户-物品反馈 + 物品相似度 */
    private record CachedMatrix(long expireAt,
                                Map<Integer, Map<Integer, Double>> userItems,
                                Map<Integer, Map<Integer, Double>> itemSimilarity) {}

    // ------------------------------------------------------------------ 场景

    /** 个性化场景推荐；用户无数据时退化为全局热度 */
    public List<Map<String, Object>> recommendScenes(User user, LevelPrediction prediction, int limit) {
        List<Scored<Scene>> ranked = rankScenes(user, prediction);
        return ranked.stream().limit(limit)
                .map(scored -> withReason(Dtos.sceneToDict(scored.item()), scored))
                .toList();
    }

    /** 供学习计划生成使用：返回排序后的场景实体 */
    public List<Scene> topScenes(User user, LevelPrediction prediction, int limit) {
        return rankScenes(user, prediction).stream().limit(limit).map(s -> s.item()).toList();
    }

    private List<Scored<Scene>> rankScenes(User user, LevelPrediction prediction) {
        List<Scene> scenes = sceneRepository.findByStatusOrderBySceneId(1);
        if (scenes.isEmpty()) {
            return List.of();
        }
        Map<Integer, Double> collab = collabSceneScores(user);
        Set<Integer> todayScenes = todaySceneIds(user);
        Map<Integer, Long> practiced = practicedSceneCount(user);

        List<Scored<Scene>> scored = new ArrayList<>();
        for (Scene scene : scenes) {
            double content = LevelPredictService.levelMatch(prediction.level(), scene.levelScope);
            double collabScore = collab.getOrDefault(scene.sceneId, 0.0);
            double model = modelScore(prediction, scene.levelScope);
            double score = fuse(content, collabScore, model);
            if (isSlightlyHarder(prediction.level(), scene.levelScope)) {
                score += 0.05;
            }
            if (todayScenes.contains(scene.sceneId)) {
                score *= 0.3;
            }
            if (practiced.getOrDefault(scene.sceneId, 0L) >= 3) {
                score *= 0.7;
            }
            scored.add(new Scored<>(scene, score, content, collabScore, model));
        }
        scored.sort(Comparator.<Scored<Scene>>comparingDouble(s -> s.score()).reversed()
                .thenComparing(s -> s.item().sceneId));
        return scored;
    }

    // ------------------------------------------------------------------ 资源

    public List<Map<String, Object>> recommendResources(User user, LevelPrediction prediction, int limit) {
        List<Scored<LearningResource>> ranked = rankResources(user, prediction);
        return ranked.stream().limit(limit)
                .map(scored -> withReason(Dtos.resourceToDict(scored.item()), scored))
                .toList();
    }

    public List<LearningResource> topResources(User user, LevelPrediction prediction, int limit) {
        return rankResources(user, prediction).stream().limit(limit).map(s -> s.item()).toList();
    }

    private List<Scored<LearningResource>> rankResources(User user, LevelPrediction prediction) {
        List<LearningResource> resources = resourceRepository.findByStatusOrderByResourceId(1);
        if (resources.isEmpty()) {
            return List.of();
        }
        List<Scene> scenes = sceneRepository.findByStatusOrderBySceneId(1);
        Map<Integer, Double> sceneScores = collabSceneScores(user);

        List<Scored<LearningResource>> scored = new ArrayList<>();
        for (LearningResource resource : resources) {
            double content = LevelPredictService.levelMatch(prediction.level(), resource.level);
            double collabScore = resourceCollab(resource, scenes, sceneScores);
            double model = modelScore(prediction, resource.level);
            double score = fuse(content, collabScore, model);
            if (isSlightlyHarder(prediction.level(), resource.level)) {
                score += 0.05;
            }
            scored.add(new Scored<>(resource, score, content, collabScore, model));
        }
        scored.sort(Comparator.<Scored<LearningResource>>comparingDouble(s -> s.score()).reversed()
                .thenComparing(s -> s.item().resourceId));
        return scored;
    }

    /** 资源没有直接交互数据，用「与用户偏好的场景相关联」折算协同分 */
    private double resourceCollab(LearningResource resource, List<Scene> scenes, Map<Integer, Double> sceneScores) {
        double best = 0.0;
        for (Scene scene : scenes) {
            if (isRelated(scene, resource)) {
                best = Math.max(best, sceneScores.getOrDefault(scene.sceneId, 0.0));
            }
        }
        return best;
    }

    private boolean isRelated(Scene scene, LearningResource resource) {
        if (scene.sceneCategory != null && scene.sceneCategory.equals(resource.category)) {
            return true;
        }
        if (resource.level != null && scene.levelScope != null
                && resource.level.equalsIgnoreCase(scene.levelScope)) {
            return true;
        }
        return resource.title != null && scene.sceneName != null && resource.title.contains(scene.sceneName);
    }

    // ------------------------------------------------------------------ 任务

    /** 建议练习任务（非已生成的每日任务），前端可直接开始练习 */
    public List<Map<String, Object>> recommendTasks(User user, LevelPrediction prediction, int limit) {
        int minutes = minutesFor(prediction.band());
        List<Map<String, Object>> tasks = new ArrayList<>();

        List<Scored<Scene>> scenes = rankScenes(user, prediction);
        for (Scored<Scene> scored : scenes.stream().limit(2).toList()) {
            Scene scene = scored.item();
            Map<String, Object> task = taskDict("场景对话", scene.sceneName + " · " + minutes + " 分钟",
                    minutes, scene.sceneId, null, scored.score());
            tasks.add(withReason(task, scored.content(), scored.collab(), scored.model(),
                    LevelPredictService.levelMatch(prediction.level(), scene.levelScope),
                    isSlightlyHarder(prediction.level(), scene.levelScope)));
        }

        List<Scored<LearningResource>> resources = rankResources(user, prediction);
        for (Scored<LearningResource> scored : resources.stream().limit(2).toList()) {
            LearningResource resource = scored.item();
            String type = "精听".equals(resource.resType) ? "精听" : "跟读";
            Map<String, Object> task = taskDict(type, resource.title, minutes, null,
                    resource.resourceId, scored.score());
            tasks.add(withReason(task, scored.content(), scored.collab(), scored.model(),
                    LevelPredictService.levelMatch(prediction.level(), resource.level),
                    isSlightlyHarder(prediction.level(), resource.level)));
        }

        tasks.add(taskDict("单词", "高频词汇复习 20 个", 8, null, null, 0.4));
        return tasks.stream().limit(limit).toList();
    }

    /** 各水平档位的建议单次练习时长（分钟） */
    public static int minutesFor(String band) {
        if (band == null) {
            return 10;
        }
        return switch (band) {
            case "初级" -> 8;
            case "高级" -> 15;
            default -> 12;
        };
    }

    /** 当前生效的推荐策略权重，供前端展示 */
    public Map<String, Object> strategyStatus() {
        double[] weights = strategyWeights();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("content", weights[0] > 0);
        out.put("collab", weights[1] > 0);
        out.put("model", weights[2] > 0);
        return out;
    }

    // ---------------------------------------------------------- 协同过滤矩阵

    private Map<Integer, Double> collabSceneScores(User user) {
        CachedMatrix cached = matrix();
        Map<Integer, Double> rated = cached.userItems().getOrDefault(user.userId, Map.of());

        Map<Integer, Double> scores = new LinkedHashMap<>();
        if (rated.isEmpty()) {
            // 冷启动：用全局热度（各场景被练习的量）作为分数
            Map<Integer, Double> hot = new HashMap<>();
            for (Map<Integer, Double> row : cached.userItems().values()) {
                row.forEach((sceneId, value) -> hot.merge(sceneId, value, Double::sum));
            }
            hot.forEach((sceneId, value) -> scores.put(sceneId, value));
            return normalize(scores);
        }

        for (Map.Entry<Integer, Map<Integer, Double>> entry : cached.itemSimilarity().entrySet()) {
            Integer sceneId = entry.getKey();
            Map<Integer, Double> similarities = entry.getValue();
            double numerator = 0.0;
            double denominator = 0.0;
            for (Map.Entry<Integer, Double> ratedEntry : rated.entrySet()) {
                double similarity = similarities.getOrDefault(ratedEntry.getKey(), 0.0);
                numerator += similarity * ratedEntry.getValue();
                denominator += Math.abs(similarity);
            }
            scores.put(sceneId, denominator > 0 ? numerator / denominator : 0.0);
        }
        return normalize(scores);
    }

    private CachedMatrix matrix() {
        CachedMatrix cached = matrixCache;
        long now = System.currentTimeMillis();
        if (cached != null && cached.expireAt > now) {
            return cached;
        }
        synchronized (this) {
            cached = matrixCache;
            if (cached != null && cached.expireAt > now) {
                return cached;
            }
            CachedMatrix built = buildMatrix(now + CACHE_TTL_MS);
            matrixCache = built;
            return built;
        }
    }

    /** 构建「用户-场景」隐式反馈矩阵与物品相似度矩阵（规模极小，实时计算） */
    private CachedMatrix buildMatrix(long expireAt) {
        Map<Integer, Map<Integer, Double>> userItems = new HashMap<>();
        for (ConversationSession session : sessionRepository.findAll()) {
            if (session.userId == null || session.scene == null) {
                continue;
            }
            Map<Integer, Double> row = userItems.computeIfAbsent(session.userId, k -> new HashMap<>());
            row.merge(session.scene.sceneId, 1.0, Double::sum);
        }
        // 隐式反馈取值：1 + log(练习次数)，避免刷量场景权重过高
        for (Map<Integer, Double> row : userItems.values()) {
            row.replaceAll((sceneId, count) -> 1.0 + Math.log(count));
        }

        List<Integer> items = userItems.values().stream()
                .flatMap(row -> row.keySet().stream())
                .distinct()
                .sorted()
                .toList();

        Map<Integer, Double> norms = new HashMap<>();
        for (Map<Integer, Double> row : userItems.values()) {
            row.forEach((sceneId, value) -> norms.merge(sceneId, value * value, Double::sum));
        }

        Map<Integer, Map<Integer, Double>> similarity = new LinkedHashMap<>();
        for (Integer i : items) {
            Map<Integer, Double> row = new LinkedHashMap<>();
            for (Integer j : items) {
                if (i.equals(j)) {
                    row.put(j, 1.0);
                    continue;
                }
                double dot = 0.0;
                for (Map<Integer, Double> userRow : userItems.values()) {
                    Double a = userRow.get(i);
                    Double b = userRow.get(j);
                    if (a != null && b != null) {
                        dot += a * b;
                    }
                }
                double denominator = Math.sqrt(norms.getOrDefault(i, 0.0)) * Math.sqrt(norms.getOrDefault(j, 0.0));
                row.put(j, denominator > 0 ? dot / denominator : 0.0);
            }
            similarity.put(i, row);
        }
        return new CachedMatrix(expireAt, userItems, similarity);
    }

    private Map<Integer, Double> normalize(Map<Integer, Double> scores) {
        double max = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        Map<Integer, Double> out = new LinkedHashMap<>();
        if (max <= 0) {
            scores.forEach((key, value) -> out.put(key, 0.0));
            return out;
        }
        scores.forEach((key, value) -> out.put(key, value / max));
        return out;
    }

    // ---------------------------------------------------------------- 打分融合

    private double fuse(double content, double collab, double model) {
        double[] weights = strategyWeights();
        double total = weights[0] + weights[1] + weights[2];
        if (total <= 0) {
            return content;
        }
        return (content * weights[0] + collab * weights[1] + model * weights[2]) / total;
    }

    /** 读取 system_config.recommend 三个开关作为策略权重 */
    private double[] strategyWeights() {
        double[] weights = {1.0, 1.0, 1.0};
        SysConfig config = configRepository.findByKey(SYSTEM_CONFIG_KEY).orElse(null);
        if (config == null || config.value == null) {
            return weights;
        }
        Map<String, Object> parsed = JsonUtil.parseMap(config.value);
        if (parsed == null || !(parsed.get("recommend") instanceof Map<?, ?> recommend)) {
            return weights;
        }
        weights[0] = flag(recommend.get("content"));
        weights[1] = flag(recommend.get("collab"));
        weights[2] = flag(recommend.get("model"));
        return weights;
    }

    private static double flag(Object value) {
        if (value instanceof Boolean bool) {
            return bool ? 1.0 : 0.0;
        }
        return value == null ? 1.0 : 0.0;
    }

    /** 三档概率加权后的等级匹配度 */
    private double modelScore(LevelPrediction prediction, String candidateLevel) {
        double[] probs = prediction.probs();
        double sum = 0.0;
        for (int i = 0; i < probs.length && i < BAND_REPRESENTATIVE.size(); i++) {
            sum += probs[i] * LevelPredictService.levelMatch(BAND_REPRESENTATIVE.get(i), candidateLevel);
        }
        return sum;
    }

    /** 候选难度是否恰好比预测水平高一档（最近发展区） */
    private boolean isSlightlyHarder(String predictedLevel, String candidateLevel) {
        int predicted = LevelPredictService.cefrIndex(predictedLevel);
        if (predicted < 0 || candidateLevel == null || candidateLevel.isBlank()) {
            return false;
        }
        for (String part : candidateLevel.split("[-~—至]")) {
            int index = LevelPredictService.cefrIndex(part.trim());
            if (index == predicted + 1) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- 辅助信息

    private Set<Integer> todaySceneIds(User user) {
        return taskRepository.findByUserIdAndTaskDateOrderByTaskId(user.userId, LocalDate.now()).stream()
                .map(task -> task.sceneId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<Integer, Long> practicedSceneCount(User user) {
        Map<Integer, Long> counts = new HashMap<>();
        for (ConversationSession session : sessionRepository.findTop20ByUserIdOrderByStartTimeDesc(user.userId)) {
            if (session.scene != null) {
                counts.merge(session.scene.sceneId, 1L, Long::sum);
            }
        }
        return counts;
    }

    private Map<String, Object> taskDict(String type, String title, int minutes,
                                         Integer sceneId, Integer resourceId, double score) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("type", type);
        task.put("title", title);
        task.put("durationMin", minutes);
        task.put("sceneId", sceneId);
        task.put("resourceId", resourceId);
        task.put("score", Math.round(score * 100) / 100.0);
        return task;
    }

    private Map<String, Object> withReason(Map<String, Object> dict, Scored<?> scored) {
        return withReason(dict, scored.content(), scored.collab(), scored.model(), scored.content(), false);
    }

    private Map<String, Object> withReason(Map<String, Object> dict, double content, double collab,
                                           double model, double match, boolean harder) {
        String reason = harder ? "进阶挑战" : dominantReason(content, collab, model);
        dict.put("reason", reason);
        dict.put("match", Math.round(match * 100) / 100.0);
        dict.put("strategies", Map.of(
                "content", Math.round(content * 100) / 100.0,
                "collab", Math.round(collab * 100) / 100.0,
                "model", Math.round(model * 100) / 100.0));
        return dict;
    }

    private static String dominantReason(double content, double collab, double model) {
        if (collab >= content && collab >= model) {
            return "猜你喜欢";
        }
        if (model >= content && model >= collab) {
            return "水平匹配";
        }
        return "难度匹配";
    }
}
