package com.englishlearn.seed;

import com.englishlearn.common.JsonUtil;
import com.englishlearn.entity.AssessmentRecord;
import com.englishlearn.entity.CommunityComment;
import com.englishlearn.entity.CommunityPost;
import com.englishlearn.entity.ConversationMessage;
import com.englishlearn.entity.ConversationSession;
import com.englishlearn.entity.DailyTask;
import com.englishlearn.entity.LearningPlan;
import com.englishlearn.entity.LearningResource;
import com.englishlearn.entity.Scene;
import com.englishlearn.entity.StudyRecord;
import com.englishlearn.entity.SysConfig;
import com.englishlearn.entity.User;
import com.englishlearn.repository.AssessmentRecordRepository;
import com.englishlearn.repository.CommunityCommentRepository;
import com.englishlearn.repository.CommunityPostRepository;
import com.englishlearn.repository.ConversationMessageRepository;
import com.englishlearn.repository.ConversationSessionRepository;
import com.englishlearn.repository.DailyTaskRepository;
import com.englishlearn.repository.LearningPlanRepository;
import com.englishlearn.repository.LearningResourceRepository;
import com.englishlearn.repository.SceneRepository;
import com.englishlearn.repository.StudyRecordRepository;
import com.englishlearn.repository.SysConfigRepository;
import com.englishlearn.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 种子数据：首次启动时灌入与原 FastAPI 后端一致的演示数据。
 * 内容：6 个场景、6 个学习资源、3 篇社区帖子、演示账号（含报表历史）、系统配置。
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final TransactionTemplate tx;
    private final UserRepository userRepository;
    private final SceneRepository sceneRepository;
    private final LearningResourceRepository resourceRepository;
    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;
    private final LearningPlanRepository planRepository;
    private final DailyTaskRepository taskRepository;
    private final ConversationSessionRepository sessionRepository;
    private final ConversationMessageRepository messageRepository;
    private final AssessmentRecordRepository assessmentRepository;
    private final StudyRecordRepository studyRecordRepository;
    private final SysConfigRepository configRepository;
    private final BCryptPasswordEncoder encoder;

    public DataSeeder(PlatformTransactionManager txManager,
                      UserRepository userRepository,
                      SceneRepository sceneRepository,
                      LearningResourceRepository resourceRepository,
                      CommunityPostRepository postRepository,
                      CommunityCommentRepository commentRepository,
                      LearningPlanRepository planRepository,
                      DailyTaskRepository taskRepository,
                      ConversationSessionRepository sessionRepository,
                      ConversationMessageRepository messageRepository,
                      AssessmentRecordRepository assessmentRepository,
                      StudyRecordRepository studyRecordRepository,
                      SysConfigRepository configRepository,
                      BCryptPasswordEncoder encoder) {
        this.tx = new TransactionTemplate(txManager);
        this.userRepository = userRepository;
        this.sceneRepository = sceneRepository;
        this.resourceRepository = resourceRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.planRepository = planRepository;
        this.taskRepository = taskRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.assessmentRepository = assessmentRepository;
        this.studyRecordRepository = studyRecordRepository;
        this.configRepository = configRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        tx.executeWithoutResult(status -> seed());
    }

    private void seed() {
        if (sceneRepository.count() > 0) {
            log.info("检测到已有数据，跳过种子初始化");
            return;
        }
        Map<String, User> users = seedUsers();
        Map<String, Scene> scenes = seedScenes();
        List<LearningResource> resources = seedResources();
        seedPosts(users);
        seedPlansAndLearning(users, scenes, resources);
        seedConfigs();
        log.info("seed: 演示数据初始化完成");
    }

    private User mkUser(String phone, String nick, String role, String age, String password) {
        User u = new User();
        u.phone = phone;
        u.passwordHash = password;
        u.nickname = nick;
        u.userRole = role;
        u.ageGroup = age;
        u.status = 1;
        u.registerTime = LocalDateTime.now();
        return userRepository.save(u);
    }

    private Map<String, User> seedUsers() {
        String pwd = encoder.encode("123456");
        String adminPwd = encoder.encode("admin123");
        Map<String, User> users = new LinkedHashMap<>();
        users.put("13900000000", mkUser("13900000000", "管理员", "admin", "adult", adminPwd));
        users.put("13800138000", mkUser("13800138000", "Momo", "learner", "adult", pwd));
        users.put("13700000000", mkUser("13700000000", "Leo", "learner", "adult", pwd));
        users.put("13600000000", mkUser("13600000000", "Cici", "learner", "adult", pwd));
        users.put("13300000000", mkUser("13300000000", "小明", "learner", "child", pwd));
        users.put("13500000000", mkUser("13500000000", "王妈妈", "guardian", "adult", pwd));
        log.info("seed: 创建演示账号 {} 个", users.size());
        return users;
    }

    private Map<String, Scene> seedScenes() {
        String[][] spec = {
                {"餐厅点餐", "在餐厅点餐、询问推荐与结账", "生活", "A2", "服务员"},
                {"商务会议", "参与英文商务会议并发表观点", "工作", "B2", "同事"},
                {"机场值机", "办理登机手续、托运行李", "出行", "A2", "地勤"},
                {"课堂讨论", "围绕主题进行课堂讨论与提问", "学习", "B1", "同学"},
                {"酒店入住", "办理入住、咨询设施与服务", "出行", "A2", "前台"},
                {"面试问答", "模拟英文面试自我介绍与问答", "工作", "B2", "面试官"}};
        Map<String, Scene> scenes = new LinkedHashMap<>();
        for (String[] s : spec) {
            Scene scene = new Scene();
            scene.sceneName = s[0];
            scene.sceneDesc = s[1];
            scene.sceneCategory = s[2];
            scene.levelScope = s[3];
            scene.roleSetting = JsonUtil.toJson(Map.of("role", s[4], "script", s[0] + "情境对话练习"));
            scene.status = 1;
            scene = sceneRepository.save(scene);
            scenes.put(s[0], scene);
        }
        return scenes;
    }

    private List<LearningResource> seedResources() {
        String[][] spec = {
                {"BBC 六分钟英语 · 职场礼仪", "新闻", "职场", "B1", "360"},
                {"Friends 老友记 · 第一季片段", "剧集", "生活", "A2", "240"},
                {"TED · 高效学习的秘密", "播客", "学习", "B2", "600"},
                {"小王子 · 有声书第一章", "有声书", "生活", "A2", "480"},
                {"雅思口语 · Part 2 话题训练", "新闻", "雅思", "B2", "300"},
                {"机场广播 · 值机通知", "新闻", "出行", "A2", "180"}};
        java.util.ArrayList<LearningResource> resources = new java.util.ArrayList<>();
        for (String[] r : spec) {
            LearningResource res = new LearningResource();
            res.title = r[0];
            res.resType = r[1];
            res.category = r[2];
            res.level = r[3];
            res.durationSec = Integer.parseInt(r[4]);
            res.status = 1;
            resources.add(resourceRepository.save(res));
        }
        return resources;
    }

    private void seedPosts(Map<String, User> users) {
        String[][] posts = {
                {"13800138000", "学习心得", "坚持跟读 30 天，我的发音变化", "每天跟读老友记片段 15 分钟，第 30 天发音评分从 62 涨到了 81，分享一下方法……", "128"},
                {"13700000000", "结伴练习", "寻找口语搭子，每晚 8 点", "B1 水平，想练商务英语，有没有一起结伴练习的小伙伴？", "45"},
                {"13600000000", "学习心得", "AI 场景对话真的太适合社恐了", "在餐厅点餐场景里练了 20 分钟，不用怕尴尬，出错也会温柔纠正……", "96"}};
        Map<String, String[][]> comments = Map.of(
                "坚持跟读 30 天，我的发音变化", new String[][]{{"13600000000", "太棒了！请问你用的是什么评分工具呀？"}, {"13700000000", "同感，坚持最重要，一起加油！"}},
                "寻找口语搭子，每晚 8 点", new String[][]{{"13800138000", "举手！我 A2，也想练商务方向"}, {"13600000000", "每晚 8 点有点早，能改 9 点吗"}},
                "AI 场景对话真的太适合社恐了", new String[][]{{"13800138000", "哈哈哈同社恐，我练的餐厅场景"}, {"13700000000", "错了也不尴尬，这点最棒"}});
        for (String[] p : posts) {
            CommunityPost post = new CommunityPost();
            post.author = users.get(p[0]);
            post.title = p[2];
            post.content = p[3];
            post.topic = p[1];
            post.likes = Integer.parseInt(p[4]);
            post.status = 1;
            post.isTop = false;
            post.commentCount = 0;
            post = postRepository.save(post);
            String[][] cs = comments.get(p[2]);
            for (String[] c : cs) {
                CommunityComment comment = new CommunityComment();
                comment.postId = post.postId;
                comment.author = users.get(c[0]);
                comment.content = c[1];
                commentRepository.save(comment);
            }
            post.commentCount = cs.length;
            postRepository.save(post);
        }
    }

    private void seedPlansAndLearning(Map<String, User> users, Map<String, Scene> scenes, List<LearningResource> resources) {
        LocalDate today = LocalDate.now();
        String[][] planSpecs = {
                {"13800138000", "兴趣", "A2"},
                {"13700000000", "商务", "B1"},
                {"13600000000", "考试", "B2"}};
        Map<String, LearningPlan> plans = new LinkedHashMap<>();
        for (String[] s : planSpecs) {
            LearningPlan plan = new LearningPlan();
            plan.userId = users.get(s[0]).userId;
            plan.targetGoal = s[1];
            plan.levelStart = s[2];
            plan.levelCurrent = s[2];
            plan.planContent = planContentJson(s[1]);
            plan.planStart = today.minusDays(30);
            plan.planStatus = "active";
            plan = planRepository.save(plan);
            plans.put(s[0], plan);
        }

        User momo = users.get("13800138000");
        buildTodayTasks(momo, plans.get("13800138000"), scenes, resources);

        int[] scoreSeq = {58, 63, 61, 70, 74, 78, 79, 81};
        // 最近的周六
        int daysBack = (today.getDayOfWeek().getValue() + 1) % 7;
        LocalDate saturday = today.minusDays(daysBack);
        for (int i = 0; i < scoreSeq.length; i++) {
            StudyRecord record = new StudyRecord();
            record.userId = momo.userId;
            record.actionType = "scenario";
            record.durationMin = 12 + (i % 3) * 2;
            record.score = (double) scoreSeq[i];
            record.learnDate = saturday.minusWeeks(7L - i);
            studyRecordRepository.save(record);
        }

        LocalDateTime now = LocalDateTime.now();
        Scene restaurant = scenes.get("餐厅点餐");
        seedFinishedSession(momo, restaurant, now.minusDays(2), 76.5,
                new double[]{76, 68, 72, 64});
        seedFinishedSession(momo, restaurant, now.minusDays(1), 81.0,
                new double[]{84, 79, 82, 78});
    }

    private String planContentJson(String goal) {
        return JsonUtil.toJson(Map.of(
                "goal", goal,
                "stages", List.of(
                        Map.of("stage", 1, "focus", "基础表达与场景开口", "tasks", 4),
                        Map.of("stage", 2, "focus", "流利度与句型丰富度提升", "tasks", 4),
                        Map.of("stage", 3, "focus", "实战综合演练与弱项专项", "tasks", 4))));
    }

    private void buildTodayTasks(User user, LearningPlan plan, Map<String, Scene> scenes, List<LearningResource> resources) {
        String[][] templates = {
                {"场景对话", "餐厅点餐 · 10 分钟", "10", "scene:餐厅点餐"},
                {"跟读", "老友记片段跟读", "15", "resource:老友记"},
                {"精听", "BBC 六分钟英语精听", "12", "resource:BBC"},
                {"单词", "日常词汇复习 20 个", "8", ""}};
        LocalDate today = LocalDate.now();
        for (String[] t : templates) {
            DailyTask task = new DailyTask();
            task.userId = user.userId;
            task.planId = plan.planId;
            task.taskType = t[0];
            task.title = t[1];
            task.durationMin = Integer.parseInt(t[2]);
            if (t[3].startsWith("scene:")) {
                Scene s = scenes.get(t[3].substring(6));
                task.sceneId = s != null ? s.sceneId : null;
            } else if (t[3].startsWith("resource:")) {
                String kw = t[3].substring(9);
                task.resourceId = resources.stream()
                        .filter(r -> r.title != null && r.title.contains(kw))
                        .map(r -> r.resourceId)
                        .findFirst().orElse(null);
            }
            task.done = 0;
            task.taskDate = today;
            taskRepository.save(task);
        }
    }

    private void seedFinishedSession(User user, Scene scene, LocalDateTime endDt, double total, double[] dims) {
        ConversationSession session = new ConversationSession();
        session.userId = user.userId;
        session.scene = scene;
        session.mode = "scenario";
        session.startTime = endDt.minusMinutes(12);
        session.endTime = endDt;
        session.durationSec = 720;
        session.sessionStatus = "finished";
        session = sessionRepository.save(session);

        ConversationMessage ai = new ConversationMessage();
        ai.sessionId = session.sessionId;
        ai.speaker = "ai";
        ai.contentEn = "Good evening! Welcome to our restaurant. How many people are in your party?";
        ai.contentZh = "晚上好！欢迎光临本餐厅，请问一共几位？";
        ai.msgTime = endDt.minusMinutes(12);
        messageRepository.save(ai);

        ConversationMessage userMsg = new ConversationMessage();
        userMsg.sessionId = session.sessionId;
        userMsg.speaker = "user";
        userMsg.contentEn = "Hello, a table for two please. Could you recommend today's special?";
        userMsg.contentZh = "你好，两位。能推荐一下今天的特色菜吗？";
        userMsg.msgTime = endDt.minusMinutes(11);
        messageRepository.save(userMsg);

        AssessmentRecord assessment = new AssessmentRecord();
        assessment.sessionId = session.sessionId;
        assessment.pronScore = dims[0];
        assessment.fluencyScore = dims[1];
        assessment.reactionScore = dims[2];
        assessment.naturalScore = dims[3];
        assessment.grammarFeedback = "句子结构清晰，注意形容词顺序。";
        assessment.phonemeIssues = JsonUtil.toJson(List.of(Map.of("word", "special", "phoneme", "/ˈspeʃl/", "note", "元音弱读")));
        assessment.assessTime = endDt.minusMinutes(11);
        assessmentRepository.save(assessment);

        Map<String, Object> dimensions = new LinkedHashMap<>();
        dimensions.put("pron", dims[0]);
        dimensions.put("fluency", dims[1]);
        dimensions.put("reaction", dims[2]);
        dimensions.put("natural", dims[3]);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", session.sessionId);
        payload.put("total", total);
        payload.put("dimensions", dimensions);
        payload.put("highlights", List.of("表达自然，能围绕场景主动开口并回应对方。"));
        payload.put("improvements", List.of("连读与弱读可继续打磨。"));
        payload.put("suggestions", List.of("回听本场录音并复练 2 次。"));
        payload.put("corrections", List.of());
        payload.put("feedbackText", "综合表现不错，最终得分 " + total + "。");
        payload.put("durationSec", 720);
        session.aiSummary = JsonUtil.toJson(payload);
        sessionRepository.save(session);
    }

    private void seedConfigs() {
        SysConfig cfg = new SysConfig();
        cfg.key = "system_config";
        cfg.value = JsonUtil.toJson(Map.of(
                "speech", Map.of("slowSpeed", 0.8, "normalSpeed", 1.2),
                "recommend", Map.of("content", true, "collab", true, "model", true),
                "audit", Map.of("content", true, "manual", true)));
        cfg.remark = "系统全局配置";
        configRepository.save(cfg);
    }
}