package com.englishlearn.llm;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * LLM 调用指标采集（进程内内存态）：供管理后台「算力监控」实时读取。
 * 记录调用次数、成功率、耗时分布、按方法维度统计与最近调用明细。
 */
@Component
public class LlmMetrics {

    /** 明细保留窗口（分钟）：超出窗口的调用不再计入时间轴 */
    private static final int WINDOW_MINUTES = 30;
    /** 明细最多保留条数 */
    private static final int MAX_CALLS = 600;
    /** 时间轴分桶数（每桶 1 分钟） */
    private static final int BUCKETS = 30;

    public record Call(long at, String method, String provider, long latencyMs, boolean ok, String note) {}

    private final AtomicLong total = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong totalLatency = new AtomicLong();
    private final AtomicLong maxLatency = new AtomicLong();
    private final Map<String, AtomicLong> methodCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> methodLatency = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> methodFailed = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<Call> calls = new ConcurrentLinkedDeque<>();

    /** 统一埋点入口：包裹一次 LLM 调用，成功与失败都会记录耗时 */
    public <T> T measure(String method, String provider, Supplier<T> action) {
        long start = System.nanoTime();
        try {
            T value = action.get();
            record(method, provider, (System.nanoTime() - start) / 1_000_000L, true, null);
            return value;
        } catch (RuntimeException e) {
            record(method, provider, (System.nanoTime() - start) / 1_000_000L, false, e.getClass().getSimpleName());
            throw e;
        }
    }

    private void record(String method, String provider, long latencyMs, boolean ok, String note) {
        total.incrementAndGet();
        totalLatency.addAndGet(latencyMs);
        maxLatency.accumulateAndGet(latencyMs, Math::max);
        if (!ok) {
            failed.incrementAndGet();
            methodFailed.computeIfAbsent(method, k -> new AtomicLong()).incrementAndGet();
        }
        methodCount.computeIfAbsent(method, k -> new AtomicLong()).incrementAndGet();
        methodLatency.computeIfAbsent(method, k -> new AtomicLong()).addAndGet(latencyMs);

        calls.addLast(new Call(System.currentTimeMillis(), method, provider, latencyMs, ok, note));
        prune();
    }

    private void prune() {
        long floor = System.currentTimeMillis() - WINDOW_MINUTES * 60_000L;
        while (!calls.isEmpty() && (calls.size() > MAX_CALLS || calls.peekFirst().at() < floor)) {
            calls.pollFirst();
        }
    }

    /** 快照：总量、成功率、耗时、按方法维度、时间轴与最近明细 */
    public Map<String, Object> snapshot() {
        prune();
        long totalCalls = total.get();
        long failedCalls = failed.get();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalCalls", totalCalls);
        data.put("failedCalls", failedCalls);
        data.put("successCalls", totalCalls - failedCalls);
        data.put("successRate", totalCalls == 0 ? 100.0 : round1((totalCalls - failedCalls) * 100.0 / totalCalls));
        data.put("avgLatencyMs", totalCalls == 0 ? 0L : totalLatency.get() / totalCalls);
        data.put("maxLatencyMs", maxLatency.get());
        data.put("windowCalls", calls.size());

        List<Map<String, Object>> methods = new ArrayList<>();
        for (Map.Entry<String, AtomicLong> e : methodCount.entrySet()) {
            long count = e.getValue().get();
            long fail = methodFailed.getOrDefault(e.getKey(), new AtomicLong()).get();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("method", e.getKey());
            m.put("count", count);
            m.put("failed", fail);
            m.put("avgLatencyMs", count == 0 ? 0L : methodLatency.getOrDefault(e.getKey(), new AtomicLong()).get() / count);
            methods.add(m);
        }
        methods.sort(Comparator.comparingLong(m -> -((Long) m.get("count"))));
        data.put("methods", methods);

        data.put("timeline", timeline());

        List<Map<String, Object>> recent = new ArrayList<>();
        List<Call> all = new ArrayList<>(calls);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        for (int i = all.size() - 1; i >= 0 && recent.size() < 20; i--) {
            Call c = all.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("time", LocalDateTime.ofInstant(Instant.ofEpochMilli(c.at()), ZoneId.systemDefault()).format(fmt));
            m.put("method", c.method());
            m.put("provider", c.provider());
            m.put("latencyMs", c.latencyMs());
            m.put("ok", c.ok());
            m.put("note", c.note() == null ? "" : c.note());
            recent.add(m);
        }
        data.put("recent", recent);
        return data;
    }

    /** 最近 30 分钟按分钟分桶的调用量 */
    private Map<String, Object> timeline() {
        long now = System.currentTimeMillis();
        long bucketMs = 60_000L;
        long base = now - (BUCKETS - 1) * bucketMs;
        long[] counts = new long[BUCKETS];
        for (Call c : calls) {
            long idx = (c.at() - base) / bucketMs;
            if (idx >= 0 && idx < BUCKETS) {
                counts[(int) idx] += 1;
            }
        }
        List<String> labels = new ArrayList<>(BUCKETS);
        List<Long> values = new ArrayList<>(BUCKETS);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        for (int i = 0; i < BUCKETS; i++) {
            labels.add(LocalDateTime.ofInstant(Instant.ofEpochMilli(base + i * bucketMs), ZoneId.systemDefault()).format(fmt));
            values.add(counts[i]);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("labels", labels);
        m.put("values", values);
        return m;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
