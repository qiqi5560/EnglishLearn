package com.englishlearn.service;

import com.englishlearn.common.ApiException;
import com.englishlearn.common.JsonUtil;
import com.englishlearn.dto.AdminDtos;
import com.englishlearn.dto.Dtos;
import com.englishlearn.entity.CommunityPost;
import com.englishlearn.entity.LearningPlan;
import com.englishlearn.entity.LearningResource;
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
import com.englishlearn.repository.SceneRepository;
import com.englishlearn.repository.StudyRecordRepository;
import com.englishlearn.repository.SysConfigRepository;
import com.englishlearn.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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

    public AdminService(UserRepository userRepository,
                        SceneRepository sceneRepository,
                        LearningResourceRepository resourceRepository,
                        CommunityPostRepository postRepository,
                        CommunityCommentRepository commentRepository,
                        PostLikeRepository likeRepository,
                        ConversationSessionRepository sessionRepository,
                        StudyRecordRepository studyRecordRepository,
                        LearningPlanRepository planRepository,
                        SysConfigRepository configRepository) {
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
        return Dtos.sceneToDict(sceneRepository.save(s));
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

    // ============================================================
    // 用户管理
    // ============================================================

    public Map<String, Object> listUsers(String keyword, String role, int page, int pageSize) {
        Page<User> result = userRepository.search(keyword, role, PageRequest.of(page - 1, pageSize));
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