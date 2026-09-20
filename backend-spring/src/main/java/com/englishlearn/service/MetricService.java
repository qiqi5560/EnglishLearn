package com.englishlearn.service;

import com.englishlearn.entity.CommunityComment;
import com.englishlearn.entity.CommunityPost;
import com.englishlearn.entity.ConversationSession;
import com.englishlearn.entity.DailyTask;
import com.englishlearn.entity.PostLike;
import com.englishlearn.entity.StudyRecord;
import com.englishlearn.repository.CommunityCommentRepository;
import com.englishlearn.repository.CommunityPostRepository;
import com.englishlearn.repository.ConversationMessageRepository;
import com.englishlearn.repository.ConversationSessionRepository;
import com.englishlearn.repository.DailyTaskRepository;
import com.englishlearn.repository.PostLikeRepository;
import com.englishlearn.repository.StudyRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 推荐效果指标量化服务（一期：基于现有业务数据近似统计）。
 *
 * <p>四项指标：点击率、完成率、互动率、跳出率。每项都带有口径定义与样本量检测 ——
 * 分母小于 {@link #MIN_DENOMINATOR} 时指标返回 null（前端显示"—"）并标记数据不足，
 * 避免用小样本算出误导性的百分比。
 *
 * <p>二期接入埋点事件表后，只需替换本服务中的数据读取来源，接口结构保持不变。
 */
@Service
public class MetricService {

    /** 会话时长低于该秒数且无有效发言视为跳出 */
    private static final int BOUNCE_MAX_DURATION_SEC = 30;
    /** 分母低于该值时认为样本不足，不输出具体百分比 */
    private static final int MIN_DENOMINATOR = 5;
    /** 以「用户数」为分母的指标单独放宽阈值（演示库活跃用户本就不多） */
    private static final int MIN_USER_DENOMINATOR = 3;

    private final DailyTaskRepository taskRepository;
    private final ConversationSessionRepository sessionRepository;
    private final ConversationMessageRepository messageRepository;
    private final StudyRecordRepository studyRecordRepository;
    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;
    private final PostLikeRepository likeRepository;

    public MetricService(DailyTaskRepository taskRepository,
                         ConversationSessionRepository sessionRepository,
                         ConversationMessageRepository messageRepository,
                         StudyRecordRepository studyRecordRepository,
                         CommunityPostRepository postRepository,
                         CommunityCommentRepository commentRepository,
                         PostLikeRepository likeRepository) {
        this.taskRepository = taskRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.studyRecordRepository = studyRecordRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
    }

    /** 单项统计的中间结果 */
    private record Stats(int tasks, int clicked, int done, int sessions, int bounced,
                         int activeUsers, int interactingUsers) {}

    /**
     * 计算指定天数的指标。
     *
     * @param days 统计窗口天数，1~90
     */
    public Map<String, Object> metrics(int days) {
        int windowDays = Math.max(1, Math.min(days, 90));
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(windowDays - 1L);
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<DailyTask> tasks = taskRepository.findByTaskDateBetween(start, today);
        List<ConversationSession> sessions = sessionRepository.findByStartTimeBetween(startAt, now);

        // 每个会话的用户发言数（批量查询，避免 N+1）
        Map<Integer, Long> userMessages = new HashMap<>();
        if (!sessions.isEmpty()) {
            List<Integer> sessionIds = sessions.stream().map(s -> s.sessionId).toList();
            for (Object[] row : messageRepository.countUserMessages(sessionIds)) {
                userMessages.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
            }
        }

        // 用户-日期 → 当天练习过的场景，用于判定任务是否被点击
        Map<String, Set<Integer>> sceneByUserDate = new HashMap<>();
        for (ConversationSession session : sessions) {
            if (session.userId == null || session.startTime == null || session.scene == null) {
                continue;
            }
            sceneByUserDate.computeIfAbsent(key(session.userId, session.startTime.toLocalDate()),
                    k -> new HashSet<>()).add(session.scene.sceneId);
        }

        // 学习记录：用户-日期 与活跃用户
        Set<String> studyUserDate = new HashSet<>();
        Set<Integer> studyUsers = new HashSet<>();
        for (StudyRecord record : studyRecordRepository.findByLearnDateBetween(start, today)) {
            if (record.userId == null) {
                continue;
            }
            studyUsers.add(record.userId);
            if (record.learnDate != null) {
                studyUserDate.add(key(record.userId, record.learnDate));
            }
        }

        // 社区互动：按天统计互动用户，用于互动率
        Map<LocalDate, Set<Integer>> interactionsByDate = new HashMap<>();
        for (CommunityPost post : postRepository.findAll()) {
            if (post.author != null) {
                addInteraction(interactionsByDate, post.createTime, startAt, post.author.userId);
            }
        }
        for (CommunityComment comment : commentRepository.findAll()) {
            if (comment.author != null) {
                addInteraction(interactionsByDate, comment.createTime, startAt, comment.author.userId);
            }
        }
        for (PostLike like : likeRepository.findAll()) {
            addInteraction(interactionsByDate, like.createTime, startAt, like.userId);
        }

        Stats overall = compute(tasks, sessions, sceneByUserDate, studyUserDate, studyUsers,
                userMessages, interactionsByDate.values().stream()
                        .flatMap(Set::stream).collect(java.util.stream.Collectors.toSet()));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("ctr", rate(overall.clicked(), overall.tasks()));
        summary.put("completionRate", rate(overall.done(), overall.tasks()));
        summary.put("interactionRate", rate(overall.interactingUsers(), overall.activeUsers(), MIN_USER_DENOMINATOR));
        summary.put("bounceRate", rate(overall.bounced(), overall.sessions()));

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("tasks", overall.tasks());
        detail.put("clickedTasks", overall.clicked());
        detail.put("doneTasks", overall.done());
        detail.put("sessions", overall.sessions());
        detail.put("bouncedSessions", overall.bounced());
        detail.put("activeUsers", overall.activeUsers());
        detail.put("interactingUsers", overall.interactingUsers());

        boolean insufficient = overall.tasks() < MIN_DENOMINATOR
                || overall.sessions() < MIN_DENOMINATOR
                || overall.activeUsers() < MIN_USER_DENOMINATOR;
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("tasks", overall.tasks());
        sample.put("sessions", overall.sessions());
        sample.put("activeUsers", overall.activeUsers());
        sample.put("interactions", overall.interactingUsers());
        sample.put("insufficient", insufficient);
        sample.put("minDenominator", MIN_DENOMINATOR);

        // 按天趋势
        List<String> dates = new ArrayList<>();
        List<Double> ctrTrend = new ArrayList<>();
        List<Double> completionTrend = new ArrayList<>();
        List<Double> interactionTrend = new ArrayList<>();
        List<Double> bounceTrend = new ArrayList<>();
        for (int i = 0; i < windowDays; i++) {
            LocalDate day = start.plusDays(i);
            List<DailyTask> dayTasks = tasks.stream().filter(t -> day.equals(t.taskDate)).toList();
            List<ConversationSession> daySessions = sessions.stream()
                    .filter(s -> s.startTime != null && day.equals(s.startTime.toLocalDate()))
                    .toList();
            Stats dayStats = compute(dayTasks, daySessions, sceneByUserDate, studyUserDate, studyUsers,
                    userMessages, interactionsByDate.getOrDefault(day, Set.of()));
            dates.add(day.toString());
            ctrTrend.add(rate(dayStats.clicked(), dayStats.tasks()));
            completionTrend.add(rate(dayStats.done(), dayStats.tasks()));
            interactionTrend.add(rate(dayStats.interactingUsers(), dayStats.activeUsers(), MIN_USER_DENOMINATOR));
            bounceTrend.add(rate(dayStats.bounced(), dayStats.sessions()));
        }

        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("dates", dates);
        trend.put("ctr", ctrTrend);
        trend.put("completionRate", completionTrend);
        trend.put("interactionRate", interactionTrend);
        trend.put("bounceRate", bounceTrend);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("days", windowDays);
        out.put("summary", summary);
        out.put("detail", detail);
        out.put("sample", sample);
        out.put("trend", trend);
        out.put("definitions", definitions());
        out.put("stage", "phase1");
        out.put("stageNote", "一期口径基于现有业务数据近似统计；二期接入埋点后将改用真实曝光与停留数据");
        return out;
    }

    private Stats compute(List<DailyTask> tasks,
                          List<ConversationSession> sessions,
                          Map<String, Set<Integer>> sceneByUserDate,
                          Set<String> studyUserDate,
                          Set<Integer> studyUsers,
                          Map<Integer, Long> userMessages,
                          Set<Integer> interactingUsers) {
        int clicked = 0;
        for (DailyTask task : tasks) {
            if (isClicked(task, sceneByUserDate, studyUserDate)) {
                clicked++;
            }
        }
        int done = (int) tasks.stream().filter(t -> t.done != null && t.done == 1).count();

        int bounced = 0;
        for (ConversationSession session : sessions) {
            long messages = userMessages.getOrDefault(session.sessionId, 0L);
            int duration = session.durationSec == null ? 0 : session.durationSec;
            if (messages == 0 || duration < BOUNCE_MAX_DURATION_SEC) {
                bounced++;
            }
        }

        Set<Integer> active = new HashSet<>();
        for (ConversationSession session : sessions) {
            if (session.userId != null) {
                active.add(session.userId);
            }
        }
        for (DailyTask task : tasks) {
            if (task.userId != null) {
                active.add(task.userId);
            }
        }
        active.addAll(studyUsers);

        int interacting = 0;
        for (Integer userId : active) {
            if (interactingUsers.contains(userId)) {
                interacting++;
            }
        }
        return new Stats(tasks.size(), clicked, done, sessions.size(), bounced, active.size(), interacting);
    }

    /** 任务是否被点击：场景类任务看当天是否练过该场景，其余看学习记录或完成状态 */
    private boolean isClicked(DailyTask task, Map<String, Set<Integer>> sceneByUserDate, Set<String> studyUserDate) {
        if (task.userId == null || task.taskDate == null) {
            return false;
        }
        if (task.sceneId != null) {
            Set<Integer> scenes = sceneByUserDate.get(key(task.userId, task.taskDate));
            return scenes != null && scenes.contains(task.sceneId);
        }
        if (studyUserDate.contains(key(task.userId, task.taskDate))) {
            return true;
        }
        return task.done != null && task.done == 1;
    }

    private static void addInteraction(Map<LocalDate, Set<Integer>> target, LocalDateTime time,
                                       LocalDateTime startAt, Integer userId) {
        if (time == null || userId == null || time.isBefore(startAt)) {
            return;
        }
        target.computeIfAbsent(time.toLocalDate(), k -> new HashSet<>()).add(userId);
    }

    /** 计算百分比；分母不足时返回 null，由前端显示"—" */
    private static Double rate(int numerator, int denominator) {
        return rate(numerator, denominator, MIN_DENOMINATOR);
    }

    private static Double rate(int numerator, int denominator, int minDenominator) {
        if (denominator < minDenominator) {
            return null;
        }
        return Math.round(numerator * 1000.0 / denominator) / 10.0;
    }

    private static String key(Integer userId, LocalDate date) {
        return userId + "|" + date;
    }

    /** 四项指标口径定义，固化检测标准，避免口径歧义 */
    private List<Map<String, Object>> definitions() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(definition("ctr", "点击率",
                "已产生学习行为的每日任务数 / 每日任务总数（场景类任务看当天是否练过该场景）",
                "daily_task + conversation_session + study_record"));
        list.add(definition("completionRate", "完成率",
                "done = 1 的每日任务数 / 每日任务总数",
                "daily_task.done"));
        list.add(definition("interactionRate", "互动率",
                "窗口内有发帖 / 评论 / 点赞行为的用户数 / 窗口内活跃用户数",
                "community_post + community_comment + post_like"));
        list.add(definition("bounceRate", "跳出率",
                "无用户发言或时长 < 30 秒的会话数 / 会话总数",
                "conversation_session + conversation_message"));
        return list;
    }

    private static Map<String, Object> definition(String key, String name, String formula, String source) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("name", name);
        item.put("formula", formula);
        item.put("source", source);
        return item;
    }
}
