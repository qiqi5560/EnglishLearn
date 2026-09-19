package com.englishlearn.service;

import com.englishlearn.common.ApiException;
import com.englishlearn.common.JsonUtil;
import com.englishlearn.dto.AdminDtos;
import com.englishlearn.dto.Dtos;
import com.englishlearn.entity.CommunityComment;
import com.englishlearn.entity.CommunityPost;
import com.englishlearn.entity.LearningPlan;
import com.englishlearn.entity.LearningResource;
import com.englishlearn.entity.QuoteMaterial;
import com.englishlearn.entity.Scene;
import com.englishlearn.entity.StudyRecord;
import com.englishlearn.entity.SysConfig;
import com.englishlearn.entity.User;
import com.englishlearn.repository.CommunityCommentRepository;
import com.englishlearn.repository.CommunityPostRepository;
import com.englishlearn.repository.ConversationSessionRepository;
import com.englishlearn.repository.LearningPlanRepository;
import com.englishlearn.repository.LearningResourceRepository;
import com.englishlearn.repository.PostLikeRepository;
import com.englishlearn.repository.QuoteMaterialRepository;
import com.englishlearn.repository.SceneRepository;
import com.englishlearn.repository.StudyRecordRepository;
import com.englishlearn.repository.SysConfigRepository;
import com.englishlearn.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台服务：数据看板、场景/资源/帖子/用户/配置管理（全部需要管理员权限）。
 */
@Service
public class AdminService {

    private static final String CONFIG_KEY = "system_config";

    private final UserRepository userRepository;
    private final SceneRepository sceneRepository;
    private final LearningResourceRepository resourceRepository;
    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;
    private final PostLikeRepository likeRepository;
    private final ConversationSessionRepository sessionRepository;
    private final StudyRecordRepository studyRecordRepository;
    private final LearningPlanRepository planRepository;
    private final SysConfigRepository configRepository;
    private final QuoteMaterialRepository quoteMaterialRepository;
    private final AuditLogService auditLog;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository,
                        SceneRepository sceneRepository,
                        LearningResourceRepository resourceRepository,
                        CommunityPostRepository postRepository,
                        CommunityCommentRepository commentRepository,
                        PostLikeRepository likeRepository,
                        ConversationSessionRepository sessionRepository,
                        StudyRecordRepository studyRecordRepository,
                        LearningPlanRepository planRepository,
                        SysConfigRepository configRepository,
                        QuoteMaterialRepository quoteMaterialRepository,
                        AuditLogService auditLog,
                        BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.sceneRepository = sceneRepository;
        this.resourceRepository = resourceRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.sessionRepository = sessionRepository;
        this.studyRecordRepository = studyRecordRepository;
        this.planRepository = planRepository;
        this.configRepository = configRepository;
        this.quoteMaterialRepository = quoteMaterialRepository;
        this.auditLog = auditLog;
        this.passwordEncoder = passwordEncoder;
    }

    // ============================================================
    // 数据看板
    // ============================================================

    public Map<String, Object> dashboard() {
        long userTotal = userRepository.count();
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(7);

        long todaySessions = sessionRepository.countByStartTimeBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        long pendingRes = resourceRepository.countByStatus(0);
        long pendingPosts = postRepository.countByStatus(0);

        List<StudyRecord> records = studyRecordRepository.findByLearnDateGreaterThanEqualOrderByLearnDate(start);
        Map<LocalDate, Integer> byDay = new HashMap<>();
        for (StudyRecord r : records) {
            if (r.learnDate != null) {
                byDay.merge(r.learnDate, 1, Integer::sum);
            }
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<String> dates = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = start.plusDays(i);
            dates.add(d.format(fmt));
            values.add(byDay.getOrDefault(d, 0));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stats", List.of(
                Map.of("label", "总用户数", "value", String.format("%,d", userTotal), "color", "var(--primary)"),
                Map.of("label", "今日练习会话", "value", String.format("%,d", todaySessions), "color", "var(--success)"),
                Map.of("label", "待审核资源", "value", String.valueOf(pendingRes), "color", "var(--warning)"),
                Map.of("label", "待审核帖子", "value", String.valueOf(pendingPosts), "color", "var(--danger)")));
        data.put("trend", Map.of("dates", dates, "values", values));
        data.put("todos", List.of(
                Map.of("type", "资源审核", "desc", "新增素材待审核", "count", pendingRes),
                Map.of("type", "社区审核", "desc", "用户举报帖子待处理", "count", pendingPosts)));
        return data;
    }

    // ============================================================
    // 场景管理
    // ============================================================

    public List<Map<String, Object>> listScenes(String keyword) {
        List<Scene> all = sceneRepository.findAll(Sort.by(Sort.Direction.ASC, "sceneId"));
        String kw = keyword == null ? null : keyword.trim();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Scene s : all) {
            if (kw == null || kw.isEmpty() || (s.sceneName != null && s.sceneName.contains(kw))) {
                list.add(Dtos.sceneToDict(s));
            }
        }
        return list;
    }

    @Transactional
    public Map<String, Object> updateScene(Integer sceneId, AdminDtos.SceneUpdateIn body) {
        Scene s = sceneRepository.findById(sceneId).orElse(null);
        if (s == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "场景不存在");
        }
        if (body.sceneName() != null) s.sceneName = body.sceneName();
        if (body.sceneCategory() != null) s.sceneCategory = body.sceneCategory();
        if (body.sceneDesc() != null) s.sceneDesc = body.sceneDesc();
        if (body.levelScope() != null) s.levelScope = body.levelScope();
        if (body.coverUrl() != null) s.coverUrl = body.coverUrl();
        if (body.status() != null) s.status = body.status();
        // 对话角色与脚本以 JSON 形式存 role_setting，任一字段变化时整体重写
        if (body.role() != null || body.script() != null) {
            Map<String, Object> setting = new java.util.LinkedHashMap<>();
            setting.put("role", blankTo(body.role(), s.sceneName + "对话伙伴"));
            setting.put("script", blankTo(body.script(), s.sceneName + "情境对话练习"));
            s.roleSetting = JsonUtil.toJson(setting);
        }
        return Dtos.sceneToDict(sceneRepository.save(s));
    }

    /** 新增对话场景：保存后立即进入场景库（status=1），所有用户下次进入练习即可看到 */
    @Transactional
    public Map<String, Object> createScene(AdminDtos.SceneIn body) {
        if (body.sceneName() == null || body.sceneName().isBlank()) {
            throw new ApiException(422, "场景名称不能为空");
        }
        Scene s = new Scene();
        s.sceneName = body.sceneName().strip();
        s.sceneCategory = blankTo(body.sceneCategory(), "生活");
        s.sceneDesc = body.sceneDesc();
        s.levelScope = blankTo(body.levelScope(), "A1");
        s.coverUrl = body.coverUrl();
        s.roleSetting = JsonUtil.toJson(Map.of(
                "role", blankTo(body.role(), "对话伙伴"),
                "script", blankTo(body.script(), s.sceneName + "情境对话练习")));
        s.status = (body.status() != null && (body.status() == 0 || body.status() == 1)) ? body.status() : 1;
        return Dtos.sceneToDict(sceneRepository.save(s));
    }

    /**
     * 删除对话场景。若该场景已有用户练习记录（会话引用），直接删除会破坏历史数据，
     * 此时提示管理员改用「下架」。
     */
    @Transactional
    public void deleteScene(Integer sceneId) {
        Scene s = sceneRepository.findById(sceneId).orElse(null);
        if (s == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "场景不存在");
        }
        long used = sessionRepository.countBySceneId(sceneId);
        if (used > 0) {
            throw new ApiException(422, "该场景已有 " + used + " 条练习记录，建议改为「下架」以保留历史数据");
        }
        sceneRepository.delete(s);
    }

    private static String blankTo(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.strip();
    }

    // ============================================================
    // 资源管理
    // ============================================================

    public Map<String, Object> listResources(String keyword, int page, int pageSize) {
        Page<LearningResource> result = resourceRepository.adminSearch(keyword, PageRequest.of(page - 1, pageSize));
        return Map.of(
                "list", result.getContent().stream().map(Dtos::resourceToDict).toList(),
                "total", result.getTotalElements());
    }

    @Transactional
    public Map<String, Object> createResource(AdminDtos.ResourceIn body, User admin) {
        LearningResource item = new LearningResource();
        item.title = body.title();
        item.resType = (body.type() == null || body.type().isBlank()) ? "剧集" : body.type();
        item.category = (body.category() == null || body.category().isBlank()) ? "生活" : body.category();
        item.level = (body.level() == null || body.level().isBlank()) ? "B1" : body.level();
        item.mediaUrl = body.mediaUrl();
        item.durationSec = body.durationSec();
        item.status = (body.status() != null && (body.status() == 0 || body.status() == 1)) ? body.status() : 1;
        item.uploaderId = admin.userId;
        return Dtos.resourceToDict(resourceRepository.save(item));
    }

    @Transactional
    public Map<String, Object> updateResource(Integer resourceId, AdminDtos.ResourceUpdateIn body) {
        LearningResource item = resourceRepository.findById(resourceId).orElse(null);
        if (item == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "资源不存在");
        }
        if (body.title() != null) item.title = body.title();
        if (body.type() != null) item.resType = body.type();
        if (body.category() != null) item.category = body.category();
        if (body.level() != null) item.level = body.level();
        if (body.mediaUrl() != null) item.mediaUrl = body.mediaUrl();
        if (body.durationSec() != null) item.durationSec = body.durationSec();
        if (body.status() != null) item.status = body.status();
        return Dtos.resourceToDict(resourceRepository.save(item));
    }

    @Transactional
    public void deleteResource(Integer resourceId) {
        LearningResource item = resourceRepository.findById(resourceId).orElse(null);
        if (item == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "资源不存在");
        }
        resourceRepository.delete(item);
    }

    // ============================================================
    // 社区管理
    // ============================================================

    public Map<String, Object> listPosts(Integer status, int page, int pageSize) {
        Page<CommunityPost> result = postRepository.adminSearch(status, PageRequest.of(page - 1, pageSize));
        return Map.of(
                "list", result.getContent().stream().map(p -> Dtos.postToDict(p, false)).toList(),
                "total", result.getTotalElements());
    }

    @Transactional
    public Map<String, Object> reviewPost(Integer postId, AdminDtos.PostReviewIn body) {
        CommunityPost post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "帖子不存在");
        }
        if (body.status() != null) post.status = body.status();
        if (body.isTop() != null) post.isTop = body.isTop();
        return Dtos.postToDict(postRepository.save(post), false);
    }

    @Transactional
    public void deletePost(Integer postId) {
        CommunityPost post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "帖子不存在");
        }
        commentRepository.deleteAll(commentRepository.findByPostIdOrderByCommentIdAsc(postId));
        likeRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    /** 删除单条评论（社区治理：清理违规评论并回退帖子的评论数） */
    @Transactional
    public void deleteComment(Integer commentId) {
        CommunityComment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "评论不存在");
        }
        commentRepository.delete(comment);
        CommunityPost post = postRepository.findById(comment.postId).orElse(null);
        if (post != null) {
            post.commentCount = Math.max(0, (post.commentCount == null ? 0 : post.commentCount) - 1);
            postRepository.save(post);
        }
    }

    // ============================================================
    // 用户管理
    // ============================================================

    /**
     * 用户列表。punished 为空表示全部，true 只看处罚中，false 只看正常用户，
     * 过滤在数据库侧完成，避免前端只筛当前页造成漏查。
     */
    public Map<String, Object> listUsers(String keyword, String role, Boolean punished, int page, int pageSize) {
        Integer punishedFlag = punished == null ? null : (punished ? 1 : 0);
        Page<User> result = userRepository.search(keyword, role, punishedFlag, LocalDateTime.now(),
                PageRequest.of(page - 1, pageSize));
        List<Integer> userIds = result.getContent().stream().map(u -> u.userId).toList();
        Map<Integer, String> plans = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (LearningPlan p : planRepository.findByPlanStatusAndUserIdIn("active", userIds)) {
                plans.put(p.userId, p.levelCurrent);
            }
        }
        return Map.of(
                "list", result.getContent().stream().map(u -> Dtos.userToDict(u, plans.get(u.userId))).toList(),
                "total", result.getTotalElements());
    }

    @Transactional
    public Map<String, Object> updateUser(Integer userId, AdminDtos.UserUpdateIn body, User admin) {
        User target = userRepository.findById(userId).orElse(null);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        if (target.userId.equals(admin.userId) && body.status() != null && body.status() == 0) {
            throw new ApiException(422, "不能停用自己");
        }
        if (body.nickname() != null) {
            String nick = body.nickname().strip();
            target.nickname = nick.isEmpty() ? target.nickname : nick;
        }
        if (body.status() != null) target.status = body.status();
        if (body.userRole() != null) {
            if (target.userId.equals(admin.userId) && !"admin".equals(body.userRole())) {
                throw new ApiException(422, "不能修改自己的角色");
            }
            target.userRole = body.userRole();
        }
        return Dtos.userToDict(userRepository.save(target), null);
    }

    /** 管理员重置用户密码：不传则重置为默认 123456 */
    @Transactional
    public void resetPassword(Integer userId, String password) {
        User target = userRepository.findById(userId).orElse(null);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        String raw = (password == null || password.isBlank()) ? "123456" : password.trim();
        if (raw.length() < 6) {
            throw new ApiException(422, "密码长度不能少于 6 位");
        }
        target.passwordHash = passwordEncoder.encode(raw);
        userRepository.save(target);
    }

    /**
     * 发帖处罚：禁止该用户在指定天数内发帖与评论（登录与学习不受影响）。
     * 处罚信息写入 users.ban_until / ban_reason，用户端据此提示。
     */
    @Transactional
    public Map<String, Object> banUser(Integer userId, AdminDtos.BanIn body, User admin) {
        User target = userRepository.findById(userId).orElse(null);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        if (target.userId.equals(admin.userId)) {
            throw new ApiException(422, "不能处罚自己");
        }
        if ("admin".equals(target.userRole)) {
            throw new ApiException(422, "不能处罚管理员账号");
        }
        int days = (body == null || body.days() == null) ? 1 : body.days();
        if (days < 1 || days > 365) {
            throw new ApiException(422, "处罚天数需在 1 ~ 365 天之间");
        }
        String reason = (body.reason() == null || body.reason().isBlank()) ? "恶意发帖" : body.reason().strip();
        // 已在处罚期内则从原截止时间顺延，避免重复处罚缩短时长
        LocalDateTime base = (target.banUntil != null && target.banUntil.isAfter(LocalDateTime.now()))
                ? target.banUntil : LocalDateTime.now();
        target.banUntil = base.plusDays(days);
        target.banReason = reason;
        return Dtos.userToDict(userRepository.save(target), null);
    }

    /** 解除发帖处罚 */
    @Transactional
    public Map<String, Object> unbanUser(Integer userId) {
        User target = userRepository.findById(userId).orElse(null);
        if (target == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        target.banUntil = null;
        target.banReason = null;
        return Dtos.userToDict(userRepository.save(target), null);
    }

    // ============================================================
    // 名句素材管理（内置素材库维护）
    // ============================================================

    public List<Map<String, Object>> listQuotes(String keyword, Integer builtin) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Map<String, Object>> out = new ArrayList<>();
        for (QuoteMaterial q : quoteMaterialRepository.findAll(Sort.by(Sort.Direction.ASC, "quoteId"))) {
            if (builtin != null && (q.builtin == null ? 0 : q.builtin) != builtin) {
                continue;
            }
            if (!kw.isEmpty()) {
                String title = q.title == null ? "" : q.title.toLowerCase();
                String source = q.source == null ? "" : q.source.toLowerCase();
                String text = q.textEn == null ? "" : q.textEn.toLowerCase();
                if (!title.contains(kw) && !source.contains(kw) && !text.contains(kw)) {
                    continue;
                }
            }
            out.add(quoteToDict(q));
        }
        return out;
    }

    @Transactional
    public Map<String, Object> createQuote(AdminDtos.QuoteIn body) {
        if (body.textEn() == null || body.textEn().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "英文正文不能为空");
        }
        QuoteMaterial q = new QuoteMaterial();
        q.title = (body.title() == null || body.title().isBlank())
                ? abbreviate(body.textEn()) : body.title().trim();
        q.source = body.source();
        q.category = (body.category() == null || body.category().isBlank()) ? "英语美句" : body.category();
        q.level = (body.level() == null || body.level().isBlank()) ? "B1" : body.level();
        q.textEn = body.textEn().trim();
        q.textZh = body.textZh();
        q.builtin = 1;
        q.ownerId = null;
        return quoteToDict(quoteMaterialRepository.save(q));
    }

    @Transactional
    public Map<String, Object> updateQuote(Integer quoteId, AdminDtos.QuoteIn body) {
        QuoteMaterial q = quoteMaterialRepository.findById(quoteId).orElse(null);
        if (q == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "名句不存在");
        }
        if (body.title() != null && !body.title().isBlank()) q.title = body.title().trim();
        if (body.source() != null) q.source = body.source();
        if (body.category() != null && !body.category().isBlank()) q.category = body.category();
        if (body.level() != null && !body.level().isBlank()) q.level = body.level();
        if (body.textEn() != null && !body.textEn().isBlank()) q.textEn = body.textEn().trim();
        if (body.textZh() != null) q.textZh = body.textZh();
        return quoteToDict(quoteMaterialRepository.save(q));
    }

    @Transactional
    public void deleteQuote(Integer quoteId) {
        QuoteMaterial q = quoteMaterialRepository.findById(quoteId).orElse(null);
        if (q == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "名句不存在");
        }
        quoteMaterialRepository.delete(q);
    }

    private static Map<String, Object> quoteToDict(QuoteMaterial q) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("quoteId", q.quoteId);
        m.put("title", q.title);
        m.put("source", q.source);
        m.put("category", q.category);
        m.put("level", q.level);
        m.put("textEn", q.textEn);
        m.put("textZh", q.textZh);
        m.put("builtin", q.builtin != null && q.builtin == 1);
        m.put("ownerId", q.ownerId);
        m.put("wordCount", q.textEn == null ? 0 : q.textEn.trim().split("\\s+").length);
        return m;
    }

    private static String abbreviate(String text) {
        String t = text.trim().replaceAll("\\s+", " ");
        return t.length() <= 24 ? t : t.substring(0, 24) + "…";
    }

    // ============================================================
    // 系统配置
    // ============================================================

    private Map<String, Object> defaultConfig() {
        Map<String, Object> speech = new LinkedHashMap<>();
        speech.put("slowSpeed", 0.8);
        speech.put("normalSpeed", 1.2);
        Map<String, Object> recommend = new LinkedHashMap<>();
        recommend.put("content", true);
        recommend.put("collab", true);
        recommend.put("model", true);
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("content", true);
        audit.put("manual", true);
        Map<String, Object> def = new LinkedHashMap<>();
        def.put("speech", speech);
        def.put("recommend", recommend);
        def.put("audit", audit);
        return def;
    }

    @Transactional
    public Map<String, Object> getConfigs() {
        SysConfig cfg = configRepository.findByKey(CONFIG_KEY).orElse(null);
        Map<String, Object> stored = (cfg != null && cfg.value != null) ? JsonUtil.parseMap(cfg.value) : null;
        if (stored == null) {
            stored = new LinkedHashMap<>();
        }
        return mergeConfig(defaultConfig(), stored);
    }

    @Transactional
    public Map<String, Object> updateConfigs(Map<String, Object> body) {
        SysConfig cfg = configRepository.findByKey(CONFIG_KEY).orElse(null);
        if (cfg == null) {
            cfg = new SysConfig();
            cfg.key = CONFIG_KEY;
            cfg.value = JsonUtil.toJson(defaultConfig());
            cfg.remark = "系统全局配置";
            cfg = configRepository.save(cfg);
        }
        Map<String, Object> old = cfg.value != null ? JsonUtil.parseMap(cfg.value) : null;
        if (old == null) {
            old = new LinkedHashMap<>();
        }
        Map<String, Object> merged = new LinkedHashMap<>(defaultConfig());
        merged.putAll(old);
        for (String group : new ArrayList<>(merged.keySet())) {
            Object incoming = body == null ? null : body.get(group);
            if (incoming instanceof Map) {
                Map<String, Object> cur = merged.get(group) instanceof Map
                        ? new LinkedHashMap<>((Map<String, Object>) merged.get(group))
                        : new LinkedHashMap<>();
                cur.putAll((Map<String, Object>) incoming);
                merged.put(group, cur);
            }
        }
        cfg.value = JsonUtil.toJson(merged);
        configRepository.save(cfg);
        return mergeConfig(defaultConfig(), merged);
    }

    private Map<String, Object> mergeConfig(Map<String, Object> def, Map<String, Object> stored) {
        Map<String, Object> merged = new LinkedHashMap<>(def);
        merged.putAll(stored);
        for (String group : def.keySet()) {
            Object storedGroup = stored.get(group);
            if (storedGroup instanceof Map) {
                Map<String, Object> g = new LinkedHashMap<>((Map<String, Object>) def.get(group));
                g.putAll((Map<String, Object>) storedGroup);
                merged.put(group, g);
            }
        }
        return merged;
    }
}