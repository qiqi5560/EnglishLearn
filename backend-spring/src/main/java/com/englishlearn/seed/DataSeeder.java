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
import com.englishlearn.entity.QuoteMaterial;
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
import com.englishlearn.repository.QuoteMaterialRepository;
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
    private final QuoteMaterialRepository quoteRepository;
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
                      QuoteMaterialRepository quoteRepository,
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
        this.quoteRepository = quoteRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        tx.executeWithoutResult(status -> seed());
        // 名句素材库独立初始化：老库（已有场景数据）也能补上名句
        tx.executeWithoutResult(status -> seedQuotes());
        // 管理员账号独立初始化：老库也能补建，保证后台始终可登录
        tx.executeWithoutResult(status -> seedAdmins());
    }

    /** 内置管理员账号（按手机号判重，可重复执行） */
    private static final String[][] ADMIN_ACCOUNTS = {
            {"13900000000", "管理员"},
            {"13900000001", "运营管理员"},
    };

    private void seedAdmins() {
        String pwd = encoder.encode("admin123");
        for (String[] spec : ADMIN_ACCOUNTS) {
            if (userRepository.findByPhone(spec[0]).isPresent()) {
                continue;
            }
            mkUser(spec[0], spec[1], "admin", "adult", pwd);
            log.info("seed: 已创建管理员账号 {}（{}）", spec[0], spec[1]);
        }
    }

    /** 名句跟读素材库：15 条内置名句，每条约 50 词，带中英对照 */
    private void seedQuotes() {
        if (quoteRepository.countByBuiltin(1) > 0) {
            return;
        }
        String[][] spec = {
                {"阿甘正传 · 生活就像一盒巧克力", "《阿甘正传》", "A2",
                        "My mama always said life is like a box of chocolates, and you never know what you are going to get. "
                                + "That simple sentence has carried me through every strange road I have walked, every friend I have lost, "
                                + "and every morning I did not understand. So I keep walking, and I keep tasting.",
                        "妈妈常说，生活就像一盒巧克力，你永远不知道会拿到哪一颗。就是这句简单的话，陪我走过了每一段陌生的路、"
                                + "告别了每一位朋友，也熬过了每一个我不明白的清晨。所以我继续往前走，也继续品尝。"},
                {"蝙蝠侠 · 我们为什么会跌倒", "《蝙蝠侠：侠影之谜》", "B1",
                        "Why do we fall? So we can learn to pick ourselves up. That question stayed with me in every dark hour of my life. "
                                + "Falling is not the end of the story; it is only the moment that decides whether you stay down or stand again. "
                                + "Stand up. That is the whole lesson, and it is enough.",
                        "我们为什么会跌倒？是为了学会自己站起来。在我人生每一个黑暗的时刻，这个问题都陪着我。"
                                + "跌倒并不是故事的终点，它只是决定你趴着还是重新站起来的那个瞬间。站起来。这就是全部的功课，也足够了。"},
                {"星球大战 · 没有「试试看」", "《星球大战》", "B1",
                        "Do or do not. There is no try. When I was young I thought those words were cruel. Later I understood that trying "
                                + "is a door left half open, and half-open doors let all the courage out. Choose the thing, then move. "
                                + "Courage is not a feeling; it is a decision you make before your hands stop shaking.",
                        "要么做，要么不做，没有「试试看」。年轻时我觉得这句话太狠，后来才明白，「试试看」是一扇半开的门，"
                                + "而半开的门会把勇气全部漏掉。选定那件事，然后行动。勇气不是一种感觉，而是在手还在抖之前就做下的决定。"},
                {"狮子王 · 过去会让人痛", "《狮子王》", "B1",
                        "The past can hurt. But you can either run from it, or learn from it. I spent years running, and the past always ran faster. "
                                + "Then I turned around and looked at it properly, and it became smaller. It stopped being a monster. "
                                + "What you carry, you can also put down.",
                        "过去会让人痛。你可以选择逃避，也可以选择从中学习。我逃了很多年，可过去总跑得比我快。"
                                + "后来我转过身，认真地看着它，它就变小了，不再是一个怪物。你背着的东西，也可以放下。"},
                {"肖申克的救赎 · 希望是美好的", "《肖申克的救赎》", "B2",
                        "Hope is a good thing, maybe the best of things, and no good thing ever dies. In the darkest place I have known, "
                                + "hope was the only door that stayed unlocked. It did not make the walls thinner. It made me stronger than the walls. "
                                + "Get busy living, or get busy dying.",
                        "希望是美好的，也许是最美好的事物，而美好的事物永不消逝。在我所知道的最黑暗的地方，希望是唯一一扇没有上锁的门。"
                                + "它没有让墙变薄，却让我比墙更坚固。忙着活，或者忙着死。"},
                {"当幸福来敲门 · 守护你的梦想", "《当幸福来敲门》", "A2",
                        "Do not ever let somebody tell you that you cannot do something. Not even me. You have a dream; you have to protect it. "
                                + "People who cannot do something will tell you that you cannot do it too. If you want something, go get it. "
                                + "Period. Do not explain, do not argue, just go.",
                        "永远别让别人告诉你，你做不到。连我也不行。你有梦想，就要去守护它。那些自己做不到的人，总会告诉你你也做不到。"
                                + "想要什么，就去争取。就这样，不必解释，不必争辩，只管去做。"},
                {"指环王 · 如何用好被给予的时间", "《指环王》", "B2",
                        "All we have to decide is what to do with the time that is given to us. There are other forces at work in this world "
                                + "besides the will of evil, and there is good worth fighting for. Even the smallest person can change the course "
                                + "of the future.",
                        "我们要决定的，只是如何用好被给予的时间。在这个世界上，除了邪恶的意志，还有别的力量在起作用，还有值得为之奋斗的善意。"
                                + "即使是最渺小的人，也能改变未来的走向。"},
                {"蜘蛛侠 · 能力与责任", "《蜘蛛侠》", "B1",
                        "With great power comes great responsibility. I learned that the hard way, and the lesson cost me someone I loved. "
                                + "Power is not a reward; it is a bill that arrives later. Every choice you make with it belongs to you. "
                                + "Use it well, because you cannot give it back.",
                        "能力越大，责任越大。这个道理我是付出代价才学会的，而代价是我失去了一个爱的人。能力不是奖赏，而是一张迟到的账单。"
                                + "你用它做出的每一个选择，都属于你自己。好好用它，因为你无法退还。"},
                {"哈利·波特 · 决定我们的是选择", "《哈利·波特》", "B2",
                        "It is not our abilities that show what we truly are, it is our choices. I have known brilliant people who chose the easy road "
                                + "and ordinary people who chose the right one. When the moment comes, you will not be asked what you can do. "
                                + "You will be asked what you choose.",
                        "决定我们成为什么样的人的，不是能力，而是选择。我见过聪明的人选了容易的路，也见过平凡的人选了正确的路。"
                                + "当那一刻到来时，没人会问你有什么能力，只会问你选择什么。"},
                {"功夫熊猫 · 今天是礼物", "《功夫熊猫》", "A2",
                        "Yesterday is history, tomorrow is a mystery, but today is a gift. That is why it is called the present. "
                                + "I wasted a lot of days worrying about days that had not arrived yet. Now I open my eyes and ask one question: "
                                + "what can I do today?",
                        "昨天已成历史，明天仍是谜团，而今天是礼物，所以它被称作「present」。我浪费过很多天，去担心那些还没到来的日子。"
                                + "现在我只睁开眼问一个问题：今天我能做点什么？"},
                {"海底总动员 · 只管一直往前游", "《海底总动员》", "A2",
                        "Just keep swimming. When the water gets dark and you cannot see the shore, you do not need a map. You need one more stroke. "
                                + "I have learned that fear is loud but it is not strong; it only wins if you stop moving. "
                                + "So keep swimming, one stroke at a time.",
                        "只管一直往前游就好。当水变暗、看不到岸边的时候，你不需要地图，你只需要再划一下。"
                                + "我慢慢明白，恐惧很吵，但并不强大；只有当你停下来时，它才会赢。所以继续游，一次一下。"},
                {"飞屋环游记 · 冒险就在前方", "《飞屋环游记》", "A2",
                        "Adventure is out there! I waited most of my life for the right moment, and the right moment never came with a bow on it. "
                                + "It came as an ordinary Tuesday, with a shaky step and a full heart. Take the step. "
                                + "The adventure does not start when you are ready; it starts when you move.",
                        "冒险就在前方！我等了大半辈子，等一个「合适的时机」，而它从没有系着蝴蝶结出现。"
                                + "它只是以一个普通的周二到来，带着一步颤抖的脚步和一颗满满的心。迈出那一步吧。"
                                + "冒险不是在你准备好时开始，而是在你动身时开始。"},
                {"勇敢传说 · 命运就在心里", "《勇敢传说》", "B1",
                        "Our fate lives within us. You only have to be brave enough to see it. For a long time I looked for my destiny in "
                                + "other people's approval, and it was never there. It was in my own hands, in my own voice, waiting for me to speak. "
                                + "So I stopped asking, and started choosing.",
                        "命运就在我们心里，你只需要足够勇敢去看见它。有很长一段时间，我在别人的认可里寻找自己的命运，可它从来不在那里。"
                                + "它就在我自己的手里，在我自己的声音里，等着我开口。于是我不再追问，开始选择。"},
                {"花木兰 · 逆境中绽放的花", "《花木兰》", "B1",
                        "The flower that blooms in adversity is the rarest of all. Hard seasons do not destroy a good root; they only decide "
                                + "which flowers are worth waiting for. If this year has been heavy for you, remember that you are still rooted, "
                                + "still growing. The bloom is coming.",
                        "逆境中绽放的花朵，最为珍贵。艰难的时节不会毁掉一条好根，它只是决定了哪一朵花值得等待。"
                                + "如果这一年对你来说很沉重，请记住：你依然扎根，依然在生长。花就要开了。"},
                {"教父 · 伟大是长出来的", "《教父》", "B2",
                        "Great men are not born great, they grow great. I used to think greatness arrived like weather, something you either got "
                                + "or did not. Then I watched ordinary people keep ordinary promises for years, and I understood. It is built. "
                                + "Brick by brick, on days nobody claps for.",
                        "伟人并非生来伟大，而是逐渐成长为伟大。我曾以为伟大像天气一样降临，要么得到，要么没有。"
                                + "后来我看到平凡的人把平凡的承诺守了很多年，我才明白：伟大是一砖一瓦砌出来的，砌在那些无人喝彩的日子里。"}};
        for (String[] q : spec) {
            QuoteMaterial m = new QuoteMaterial();
            m.title = q[0];
            m.source = q[1];
            m.category = "电影台词";
            m.level = q[2];
            m.textEn = q[3];
            m.textZh = q[4];
            m.builtin = 1;
            quoteRepository.save(m);
        }
        log.info("seed: 名句素材库初始化完成，共 {} 条", spec.length);
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