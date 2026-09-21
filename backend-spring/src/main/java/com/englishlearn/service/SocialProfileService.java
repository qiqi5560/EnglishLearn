package com.englishlearn.service;

import com.englishlearn.common.ApiException;
import com.englishlearn.common.TimeUtil;
import com.englishlearn.entity.AssessmentRecord;
import com.englishlearn.entity.CommunityPost;
import com.englishlearn.entity.StudyRecord;
import com.englishlearn.entity.User;
import com.englishlearn.entity.UserFollow;
import com.englishlearn.repository.AssessmentRecordRepository;
import com.englishlearn.repository.CommunityPostRepository;
import com.englishlearn.repository.ConversationSessionRepository;
import com.englishlearn.repository.StudyRecordRepository;
import com.englishlearn.repository.UserFollowRepository;
import com.englishlearn.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 社交资料服务：他人公开主页（不含手机号等敏感字段）、学习统计、成就徽章、关注关系。
 * 统计全部实时计算，不额外建表。
 */
@Service
public class SocialProfileService {

    private static final String SESSION_FINISHED = "finished";

    private final UserRepository userRepository;
    private final UserFollowRepository followRepository;
    private final StudyRecordRepository studyRecordRepository;
    private final ConversationSessionRepository sessionRepository;
    private final AssessmentRecordRepository assessmentRepository;
    private final CommunityPostRepository postRepository;
    private final PlanService planService;

    public SocialProfileService(UserRepository userRepository,
                                UserFollowRepository followRepository,
                                StudyRecordRepository studyRecordRepository,
                                ConversationSessionRepository sessionRepository,
                                AssessmentRecordRepository assessmentRepository,
                                CommunityPostRepository postRepository,
                                PlanService planService) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.studyRecordRepository = studyRecordRepository;
        this.sessionRepository = sessionRepository;
        this.assessmentRepository = assessmentRepository;
        this.postRepository = postRepository;
        this.planService = planService;
    }

    /** 他人公开主页：资料 + 统计 + 成就 + 关注状态与计数 */
    public Map<String, Object> profile(User viewer, Integer userId) {
        User target = requireUser(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", publicUser(target));
        data.put("stats", stats(target));
        data.put("achievements", achievements(target));
        data.put("followers", followRepository.countByFolloweeId(target.userId));
        data.put("following", followRepository.countByFollowerId(target.userId));
        data.put("isSelf", viewer != null && viewer.userId.equals(target.userId));
        data.put("followingByMe", viewer != null
                && !viewer.userId.equals(target.userId)
                && followRepository.existsByFollowerIdAndFolloweeId(viewer.userId, target.userId));
        data.put("followedMe", viewer != null
                && followRepository.existsByFollowerIdAndFolloweeId(target.userId, viewer.userId));
        data.put("posts", posts(target));
        return data;
    }

    @Transactional
    public Map<String, Object> follow(User viewer, Integer userId) {
        User target = requireUser(userId);
        if (target.userId.equals(viewer.userId)) {
            throw new ApiException(422, "不能关注自己");
        }
        boolean existed = followRepository.existsByFollowerIdAndFolloweeId(viewer.userId, target.userId);
        if (!existed) {
            UserFollow rel = new UserFollow();
            rel.followerId = viewer.userId;
            rel.followeeId = target.userId;
            followRepository.save(rel);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("followingByMe", true);
        data.put("already", existed);
        data.put("followers", followRepository.countByFolloweeId(target.userId));
        return data;
    }

    @Transactional
    public Map<String, Object> unfollow(User viewer, Integer userId) {
        User target = requireUser(userId);
        long removed = followRepository.deleteByFollowerIdAndFolloweeId(viewer.userId, target.userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("followingByMe", false);
        data.put("removed", removed);
        data.put("followers", followRepository.countByFolloweeId(target.userId));
        return data;
    }

    /** 粉丝列表：关注了 target 的人 */
    public List<Map<String, Object>> followers(Integer userId, User viewer) {
        return followRepository.findTop100ByFolloweeIdOrderByFollowIdDesc(userId).stream()
                .map(rel -> userItem(rel.followerId, viewer)).toList();
    }

    /** 关注列表：target 关注的人 */
    public List<Map<String, Object>> following(Integer userId, User viewer) {
        return followRepository.findTop100ByFollowerIdOrderByFollowIdDesc(userId).stream()
                .map(rel -> userItem(rel.followeeId, viewer)).toList();
    }

    private User requireUser(Integer userId) {
        if (userId == null) {
            throw new ApiException(422, "缺少用户 ID");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    /** 公开资料：不含手机号 / 密码 / 处罚信息 */
    private Map<String, Object> publicUser(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", u.userId);
        m.put("nickname", u.nickname != null ? u.nickname : "用户");
        m.put("avatarUrl", u.avatarUrl);
        m.put("bio", u.bio);
        m.put("level", planService.currentLevelOf(planService.getActivePlan(u)));
        m.put("registerTime", TimeUtil.iso(u.registerTime));
        return m;
    }

    private Map<String, Object> userItem(Integer userId, User viewer) {
        Map<String, Object> m = new LinkedHashMap<>();
        User u = userRepository.findById(userId).orElse(null);
        m.put("userId", userId);
        m.put("nickname", u != null && u.nickname != null ? u.nickname : "用户");
        m.put("avatarUrl", u != null ? u.avatarUrl : null);
        m.put("bio", u != null ? u.bio : null);
        m.put("level", u == null ? null : planService.currentLevelOf(planService.getActivePlan(u)));
        m.put("followingByMe", viewer != null
                && followRepository.existsByFollowerIdAndFolloweeId(viewer.userId, userId));
        return m;
    }

    private Map<String, Object> stats(User u) {
        List<StudyRecord> records = studyRecordRepository.findByUserIdOrderByLearnDate(u.userId);
        Set<String> days = new LinkedHashSet<>();
        long minutes = 0;
        for (StudyRecord r : records) {
            if (r.learnDate != null) {
                days.add(r.learnDate.toString());
            }
            minutes += r.durationMin == null ? 0 : r.durationMin;
        }
        long sessions = sessionRepository.countByUserIdAndSessionStatus(u.userId, SESSION_FINISHED);
        long posts = postRepository.countByAuthorUserId(u.userId);
        double avg = averageScore(u.userId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("studyDays", days.size());
        m.put("totalMinutes", minutes);
        m.put("sessionCount", sessions);
        m.put("postCount", posts);
        m.put("avgScore", Math.round(avg * 10) / 10.0);
        return m;
    }

    /** 最近 50 条评测记录的综合平均分，权重与 DialogueService 保持一致 */
    private double averageScore(Integer userId) {
        List<AssessmentRecord> records = assessmentRepository.latest(userId, PageRequest.of(0, 50));
        double sum = 0;
        int count = 0;
        for (AssessmentRecord a : records) {
            double pron = a.pronScore == null ? 0 : a.pronScore;
            double fluency = a.fluencyScore == null ? 0 : a.fluencyScore;
            double reaction = a.reactionScore == null ? 0 : a.reactionScore;
            double natural = a.naturalScore == null ? 0 : a.naturalScore;
            if (pron == 0 && fluency == 0 && reaction == 0 && natural == 0) {
                continue;
            }
            sum += pron * 0.35 + fluency * 0.25 + reaction * 0.2 + natural * 0.2;
            count++;
        }
        return count == 0 ? 0 : sum / count;
    }

    /** 成就徽章：纯计算，不建表 */
    private List<Map<String, Object>> achievements(User u) {
        Map<String, Object> s = stats(u);
        long days = ((Number) s.get("studyDays")).longValue();
        long minutes = ((Number) s.get("totalMinutes")).longValue();
        long sessions = ((Number) s.get("sessionCount")).longValue();
        long posts = ((Number) s.get("postCount")).longValue();
        double avg = ((Number) s.get("avgScore")).doubleValue();
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(badge("days", "坚持之星", "累计学习 7 天", days, 7));
        list.add(badge("minutes", "时长达人", "累计学习 300 分钟", minutes, 300));
        list.add(badge("sessions", "对话能手", "完成 20 次对话练习", sessions, 20));
        list.add(badge("posts", "社区活跃", "发布 5 篇帖子", posts, 5));
        list.add(badge("score", "发音达人", "平均综合分达到 85", (long) avg, 85));
        return list;
    }

    private Map<String, Object> badge(String key, String name, String desc, long value, long target) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("name", name);
        m.put("desc", desc);
        m.put("value", value);
        m.put("target", target);
        m.put("achieved", value >= target);
        m.put("progress", target <= 0 ? 100 : (int) Math.min(100, Math.round(value * 100.0 / target)));
        return m;
    }

    private List<Map<String, Object>> posts(User u) {
        List<CommunityPost> rows = postRepository
                .findTop10ByAuthorUserIdAndStatusOrderByCreateTimeDesc(u.userId, 1);
        return rows.stream().map(p -> Map.<String, Object>of(
                "id", p.postId,
                "title", p.title == null ? "" : p.title,
                "topic", p.topic == null ? "" : p.topic,
                "likes", p.likes == null ? 0 : p.likes,
                "comments", p.commentCount == null ? 0 : p.commentCount,
                "createTime", TimeUtil.iso(p.createTime))).collect(Collectors.toList());
    }
}
