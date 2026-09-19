package com.englishlearn.service;

import com.englishlearn.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 管理员操作审计：进程内保留最近若干条写操作，供后台「系统概览」追溯。
 * 只记录关键动作（资源 / 帖子 / 用户 / 配置 / 名句），不落库，重启即清空。
 */
@Service
public class AuditLogService {

    private static final int MAX_ENTRIES = 200;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConcurrentLinkedDeque<Map<String, Object>> entries = new ConcurrentLinkedDeque<>();

    /** 记录一条管理员操作 */
    public void record(User admin, String module, String action, String detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("time", LocalDateTime.now().format(FMT));
        m.put("operator", admin != null && admin.nickname != null ? admin.nickname : "管理员");
        m.put("operatorId", admin == null ? null : admin.userId);
        m.put("module", module);
        m.put("action", action);
        m.put("detail", detail == null ? "" : detail);
        entries.addFirst(m);
        while (entries.size() > MAX_ENTRIES) {
            entries.pollLast();
        }
    }

    /** 最近的操作记录（默认 30 条） */
    public List<Map<String, Object>> recent(int limit) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> e : entries) {
            if (out.size() >= limit) break;
            out.add(e);
        }
        return out;
    }

    /**
     * 按模块筛选操作记录。module 为空表示全部；
     * 由于 deque 已是「最新在前」，这里直接顺序取前 limit 条即可。
     */
    public List<Map<String, Object>> list(String module, int limit) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> e : entries) {
            if (out.size() >= limit) break;
            if (module != null && !module.isBlank() && !module.equals(e.get("module"))) continue;
            out.add(e);
        }
        return out;
    }

    /** 各模块的操作条数，供后台筛选下拉展示 */
    public List<Map<String, Object>> moduleStats() {
        Map<String, Integer> counter = new LinkedHashMap<>();
        for (Map<String, Object> e : entries) {
            String m = String.valueOf(e.get("module"));
            counter.merge(m, 1, Integer::sum);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        counter.forEach((name, count) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", name);
            item.put("count", count);
            out.add(item);
        });
        return out;
    }

    public int size() {
        return entries.size();
    }
}
