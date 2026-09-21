package com.englishlearn.service;

import com.englishlearn.entity.ConversationSession;
import com.englishlearn.entity.LearningPlan;
import com.englishlearn.entity.StudyRecord;
import com.englishlearn.entity.User;
import com.englishlearn.llm.LlmMetrics;
import com.englishlearn.repository.CommunityCommentRepository;
import com.englishlearn.repository.CommunityPostRepository;
import com.englishlearn.repository.ConversationMessageRepository;
import com.englishlearn.repository.ConversationSessionRepository;
import com.englishlearn.repository.LearningPlanRepository;
import com.englishlearn.repository.LearningResourceRepository;
import com.englishlearn.repository.QuoteMaterialRepository;
import com.englishlearn.repository.ReadingDocRepository;
import com.englishlearn.repository.SceneRepository;
import com.englishlearn.repository.StudyRecordRepository;
import com.englishlearn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台洞察服务：用户使用报表、日活动量、算力监控、系统概览。
 */
@Service
public class AdminInsightService {

    private static final DateTimeFormatter MD = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserRepository userRepository;
    private final ConversationSessionRepository sessionRepository;
    private final StudyRecordRepository studyRecordRepository;
    private final LearningPlanRepository planRepository;
    private final SceneRepository sceneRepository;
    private final LearningResourceRepository resourceRepository;
    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;
    private final ConversationMessageRepository messageRepository;
    private final QuoteMaterialRepository quoteRepository;
    private final ReadingDocRepository docRepository;
    private final LlmMetrics llmMetrics;
    private final AuditLogService auditLog;

    private final String llmProviderName;
    private final String ollamaBaseUrl;
    private final String ollamaModel;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(1))
            .build();
    /** Ollama 可达性缓存：避免每次打开页面都阻塞探测 */
    private volatile boolean reachableCache = false;
    private volatile long reachableCheckedAt = 0L;

    public AdminInsightService(UserRepository userRepository,
                               ConversationSessionRepository sessionRepository,
                               StudyRecordRepository studyRecordRepository,
                               LearningPlanRepository planRepository,
                               SceneRepository sceneRepository,
                               LearningResourceRepository resourceRepository,
                               CommunityPostRepository postRepository,
                               CommunityCommentRepository commentRepository,
                               ConversationMessageRepository messageRepository,
                               QuoteMaterialRepository quoteRepository,
                               ReadingDocRepository docRepository,
                               LlmMetrics llmMetrics,
                               AuditLogService auditLog,
                               @Value("${llm.provider:mock}") String llmProviderName,
                               @Value("${llm.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
                               @Value("${llm.ollama.model:qwen2.5:1.5b-instruct}") String ollamaModel) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.studyRecordRepository = studyRecordRepository;
        this.planRepository = planRepository;
        this.sceneRepository = sceneRepository;
        this.resourceRepository = resourceRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.messageRepository = messageRepository;
        this.quoteRepository = quoteRepository;
        this.docRepository = docRepository;
        this.llmMetrics = llmMetrics;
        this.auditLog = auditLog;
        this.llmProviderName = llmProviderName;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.ollamaModel = ollamaModel;
    }

    // ============================================================
    // 用户使用报表：按用户维度聚合练习量、时长、得分
    // ============================================================

    public Map<String, Object> usageReport(int days, String keyword) {
        int span = Math.max(1, Math.min(days, 365));
        LocalDate from = LocalDate.now().minusDays(span - 1L);
        LocalDateTime since = from.atStartOfDay();

        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.ASC, "userId"));
        List<ConversationSession> sessions = sessionRepository.findByStartTimeGreaterThanEqual(since);
        List<StudyRecord> records = studyRecordRepository.findByLearnDateGreaterThanEqualOrderByLearnDate(from);
        List<Integer> ids = users.stream().map(u -> u.userId).toList();
        Map<Integer, String> levels = new HashMap<>();
        if (!ids.isEmpty()) {
            for (LearningPlan p : planRepository.findByPlanStatusAndUserIdIn("active", ids)) {
                levels.put(p.userId, p.levelCurrent);
            }
        }

        Map<Integer, long[]> agg = new HashMap<>();   // [sessionCount, durationSec]
        Map<Integer, long[]> studyAgg = new HashMap<>(); // [recordCount, scoreSum(x10), scoreCount]
        for (ConversationSession s : sessions) {
            if (s.userId == null) continue;
            long[] a = agg.computeIfAbsent(s.userId, k -> new long[2]);
            a[0] += 1;
            a[1] += s.durationSec == null ? 0 : s.durationSec;
        }
        for (StudyRecord r : records) {
            if (r.userId == null) continue;
            long[] a = studyAgg.computeIfAbsent(r.userId, k -> new long[3]);
            a[0] += 1;
            if (r.score != null) {
                a[1] += Math.round(r.score * 10);
                a[2] += 1;
            }
        }

        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Map<String, Object>> rows = new ArrayList<>();
        long activeUsers = 0;
        long totalSessions = 0;
        long totalDurationSec = 0;
        long scoreSum = 0;
        long scoreCount = 0;
        long punishedUsers = 0;
        LocalDateTime now = LocalDateTime.now();
        for (User u : users) {
            long[] a = agg.getOrDefault(u.userId, new long[2]);
            long[] b = studyAgg.getOrDefault(u.userId, new long[3]);
            boolean active = a[0] > 0 || b[0] > 0;
            if (active) activeUsers++;
            if (u.banUntil != null && u.banUntil.isAfter(now)) punishedUsers++;
            totalSessions += a[0];
            totalDurationSec += a[1];
            scoreSum += b[1];
            scoreCount += b[2];
            if (!kw.isEmpty()) {
                String nick = u.nickname == null ? "" : u.nickname.toLowerCase();
                String phone = u.phone == null ? "" : u.phone;
                if (!nick.contains(kw) && !phone.contains(kw)) continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", u.userId);
            m.put("nickname", u.nickname);
            m.put("phone", u.phone);
            m.put("role", u.userRole);
            m.put("level", levels.get(u.userId));
            m.put("status", u.status);
            m.put("sessionCount", a[0]);
            m.put("durationMin", a[1] / 60);
            m.put("studyCount", b[0]);
            m.put("avgScore", b[2] == 0 ? null : round1(b[1] / 10.0 / b[2]));
            m.put("lastLoginTime", u.lastLoginTime == null ? null : u.lastLoginTime.format(FULL));
            m.put("registerTime", u.registerTime == null ? null : u.registerTime.format(FULL));
            m.put("active", active);
            m.put("punished", u.banUntil != null && u.banUntil.isAfter(LocalDateTime.now()));
            m.put("banUntil", u.banUntil == null ? null : u.banUntil.format(FULL));
            m.put("banReason", u.banReason);
            rows.add(m);
        }
        rows.sort(Comparator.comparingLong(r -> -(((Number) r.get("sessionCount")).longValue() * 10
                + ((Number) r.get("studyCount")).longValue())));

        long newUsers = userRepository.findByRegisterTimeGreaterThanEqual(since).size();

        // 注册月份分布：近 12 个月每月新增注册数（用于观察增长节奏）
        Map<String, Long> monthCount = new LinkedHashMap<>();
        java.time.YearMonth thisMonth = java.time.YearMonth.now();
        for (int i = 11; i >= 0; i--) {
            monthCount.put(thisMonth.minusMonths(i).toString(), 0L);
        }
        for (User u : users) {
            if (u.registerTime == null) continue;
            String key = java.time.YearMonth.from(u.registerTime).toString();
            monthCount.computeIfPresent(key, (k, v) -> v + 1);
        }
        Map<String, Object> monthly = new LinkedHashMap<>();
        monthly.put("months", new ArrayList<>(monthCount.keySet()));
        monthly.put("values", new ArrayList<>(monthCount.values()));
        monthly.put("peak", monthCount.values().stream().mapToLong(Long::longValue).max().orElse(0L));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("days", span);
        summary.put("totalUsers", users.size());
        summary.put("activeUsers", activeUsers);
        summary.put("newUsers", newUsers);
        summary.put("totalSessions", totalSessions);
        summary.put("totalDurationMin", totalDurationSec / 60);
        summary.put("avgScore", scoreCount == 0 ? 0.0 : round1(scoreSum / 10.0 / scoreCount));
        summary.put("activeRate", users.isEmpty() ? 0.0 : round1(activeUsers * 100.0 / users.size()));
        summary.put("punishedUsers", punishedUsers);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", summary);
        data.put("monthly", monthly);
        data.put("list", rows);
        return data;
    }

    // ============================================================
    // 日活动量：近 N 天活跃用户 / 新增注册 / 会话数 / 学习记录
    // ============================================================

    public Map<String, Object> activity(int days) {
        int span = Math.max(3, Math.min(days, 90));
        LocalDate from = LocalDate.now().minusDays(span - 1L);
        LocalDateTime since = from.atStartOfDay();

        List<ConversationSession> sessions = sessionRepository.findByStartTimeGreaterThanEqual(since);
        List<StudyRecord> records = studyRecordRepository.findByLearnDateGreaterThanEqualOrderByLearnDate(from);
        List<User> newUsers = userRepository.findByRegisterTimeGreaterThanEqual(since);

        Map<LocalDate, long[]> byDay = new HashMap<>(); // [sessions, durationSec, records, durationMin, newUsers]
        Map<LocalDate, java.util.Set<Integer>> actives = new HashMap<>();

        for (ConversationSession s : sessions) {
            if (s.startTime == null) continue;
            LocalDate d = s.startTime.toLocalDate();
            long[] a = byDay.computeIfAbsent(d, k -> new long[5]);
            a[0] += 1;
            a[1] += s.durationSec == null ? 0 : s.durationSec;
            if (s.userId != null) actives.computeIfAbsent(d, k -> new java.util.HashSet<>()).add(s.userId);
        }
        for (StudyRecord r : records) {
            if (r.learnDate == null) continue;
            long[] a = byDay.computeIfAbsent(r.learnDate, k -> new long[5]);
            a[2] += 1;
            a[3] += r.durationMin == null ? 0 : r.durationMin;
            if (r.userId != null) actives.computeIfAbsent(r.learnDate, k -> new java.util.HashSet<>()).add(r.userId);
        }
        for (User u : newUsers) {
            if (u.registerTime == null) continue;
            LocalDate d = u.registerTime.toLocalDate();
            byDay.computeIfAbsent(d, k -> new long[5])[4] += 1;
        }

        List<String> dates = new ArrayList<>(span);
        List<Long> activeValues = new ArrayList<>(span);
        List<Long> sessionValues = new ArrayList<>(span);
        List<Long> recordValues = new ArrayList<>(span);
        List<Long> newUserValues = new ArrayList<>(span);
        List<Long> durationValues = new ArrayList<>(span);

        List<Map<String, Object>> rows = new ArrayList<>(span);
        long peakActive = 0;
        long sumActive = 0;
        for (int i = 0; i < span; i++) {
            LocalDate d = from.plusDays(i);
            long[] a = byDay.getOrDefault(d, new long[5]);
            long active = actives.getOrDefault(d, java.util.Set.of()).size();
            long durationMin = a[1] / 60 + a[3];
            dates.add(d.format(MD));
            activeValues.add(active);
            sessionValues.add(a[0]);
            recordValues.add(a[2]);
            newUserValues.add(a[4]);
            durationValues.add(durationMin);
            peakActive = Math.max(peakActive, active);
            sumActive += active;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", d.toString());
            m.put("activeUsers", active);
            m.put("newUsers", a[4]);
            m.put("sessions", a[0]);
            m.put("studyRecords", a[2]);
            m.put("durationMin", durationMin);
            rows.add(m);
        }
        java.util.Collections.reverse(rows);

        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("dates", dates);
        trend.put("activeUsers", activeValues);
        trend.put("sessions", sessionValues);
        trend.put("studyRecords", recordValues);
        trend.put("newUsers", newUserValues);
        trend.put("durationMin", durationValues);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("days", span);
        summary.put("avgActive", round1((double) sumActive / span));
        summary.put("peakActive", peakActive);
        summary.put("todayActive", activeValues.isEmpty() ? 0 : activeValues.get(activeValues.size() - 1));
        summary.put("todaySessions", sessionValues.isEmpty() ? 0 : sessionValues.get(sessionValues.size() - 1));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", summary);
        data.put("trend", trend);
        data.put("list", rows);
        return data;
    }

    // ============================================================
    // 算力监控：Provider 状态 + LLM 调用指标
    // ============================================================

    public Map<String, Object> compute() {
        Map<String, Object> provider = new LinkedHashMap<>();
        provider.put("provider", llmProviderName);
        provider.put("model", "ollama".equalsIgnoreCase(llmProviderName) ? ollamaModel : "mock-rules");
        provider.put("baseUrl", ollamaBaseUrl);
        provider.put("reachable", "ollama".equalsIgnoreCase(llmProviderName) && probeOllama());
        provider.put("degraded", "ollama".equalsIgnoreCase(llmProviderName) && !probeOllama());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("provider", provider);
        data.putAll(llmMetrics.snapshot());
        data.put("checkedAt", LocalDateTime.now().format(FULL));
        return data;
    }

    /** 探测 Ollama 是否可达（结果缓存 15 秒，失败即视为不可达） */
    private boolean probeOllama() {
        long now = System.currentTimeMillis();
        if (now - reachableCheckedAt < 15_000L) {
            return reachableCache;
        }
        boolean ok = false;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(trimSlash(ollamaBaseUrl) + "/api/tags"))
                    .timeout(Duration.ofMillis(1200))
                    .GET()
                    .build();
            ok = httpClient.send(req, HttpResponse.BodyHandlers.discarding()).statusCode() < 400;
        } catch (Exception ignored) {
            ok = false;
        }
        reachableCache = ok;
        reachableCheckedAt = now;
        return ok;
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) return "http://localhost:11434";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    // ============================================================
    // 系统概览：运行时状态 + 数据规模 + 最近管理员操作
    // ============================================================

    public Map<String, Object> systemOverview() {
        Runtime rt = Runtime.getRuntime();
        long heapUsed = rt.totalMemory() - rt.freeMemory();
        long heapMax = rt.maxMemory();

        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("uptimeSec", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        runtime.put("heapUsedMb", heapUsed / 1024 / 1024);
        runtime.put("heapMaxMb", heapMax / 1024 / 1024);
        runtime.put("heapUsedPercent", round1(heapUsed * 100.0 / heapMax));
        runtime.put("threads", ManagementFactory.getThreadMXBean().getThreadCount());
        runtime.put("processors", rt.availableProcessors());
        runtime.put("javaVersion", System.getProperty("java.version"));
        runtime.put("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        runtime.put("startedAt", LocalDateTime.now()
                .minusSeconds(ManagementFactory.getRuntimeMXBean().getUptime() / 1000).format(FULL));

        long dbBytes = 0L;
        File dbFile = new File("data/app.db");
        if (dbFile.exists()) {
            dbBytes = dbFile.length();
        }

        Map<String, Object> database = new LinkedHashMap<>();
        database.put("file", dbFile.getAbsolutePath());
        database.put("sizeMb", round1(dbBytes / 1024.0 / 1024.0));
        database.put("tables", List.of(
                tableRow("用户 users", userRepository.count()),
                tableRow("场景 scene", sceneRepository.count()),
                tableRow("学习资源 learning_resource", resourceRepository.count()),
                tableRow("社区帖子 community_post", postRepository.count()),
                tableRow("社区评论 community_comment", commentRepository.count()),
                tableRow("对话会话 conversation_session", sessionRepository.count()),
                tableRow("对话消息 conversation_message", messageRepository.count()),
                tableRow("学习记录 study_record", studyRecordRepository.count()),
                tableRow("学习方案 learning_plan", planRepository.count()),
                tableRow("名句素材 quote_material", quoteRepository.count()),
                tableRow("导入素材 reading_doc", docRepository.count())));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("runtime", runtime);
        data.put("database", database);
        data.put("audit", Map.of("total", auditLog.size(), "recent", auditLog.recent(30)));
        data.put("status", "healthy");
        return data;
    }

    private static Map<String, Object> tableRow(String name, long count) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("count", count);
        return m;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
