package com.englishlearn.service;

import com.englishlearn.common.ApiException;
import com.englishlearn.common.JsonUtil;
import com.englishlearn.dto.Dtos;
import com.englishlearn.dto.PlanDtos;
import com.englishlearn.entity.DailyTask;
import com.englishlearn.entity.LearningPlan;
import com.englishlearn.entity.Scene;
import com.englishlearn.entity.User;
import com.englishlearn.llm.EvalResult;
import com.englishlearn.llm.LevelJudgement;
import com.englishlearn.llm.LlmProvider;
import com.englishlearn.repository.DailyTaskRepository;
import com.englishlearn.repository.LearningPlanRepository;
import com.englishlearn.repository.LearningResourceRepository;
import com.englishlearn.repository.SceneRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 个性化学习方案服务（F001）：入学测评判定、方案生成、每日任务闭环。
 */
@Service
public class PlanService {

    public static final List<String> GOAL_LIST = List.of("考试", "商务", "出国", "兴趣");

    private record TaskTemplate(String type, String title, int minutes, Map<String, String> bind) {}

    private static final Map<String, List<TaskTemplate>> TASK_TEMPLATES = Map.of(
            "商务", List.of(
                    new TaskTemplate("场景对话", "商务会议 · 10 分钟", 10, Map.of("scene", "商务会议")),
                    new TaskTemplate("跟读", "职场英语片段跟读", 15, Map.of("resource", "职场")),
                    new TaskTemplate("精听", "BBC 六分钟英语精听", 12, Map.of("resource", "BBC")),
                    new TaskTemplate("单词", "商务词汇复习 20 个", 8, null)),
            "考试", List.of(
                    new TaskTemplate("场景对话", "口语话题练习 · 10 分钟", 10, Map.of("scene", "面试问答")),
                    new TaskTemplate("跟读", "听力原文逐句跟读", 15, Map.of("resource", "雅思")),
                    new TaskTemplate("精听", "真题听力精听训练", 12, Map.of("resource", "雅思")),
                    new TaskTemplate("单词", "高频考试词汇复习", 8, null)),
            "出国", List.of(
                    new TaskTemplate("场景对话", "机场值机 · 10 分钟", 10, Map.of("scene", "机场值机")),
                    new TaskTemplate("跟读", "旅行英语片段跟读", 15, Map.of("resource", "机场广播")),
                    new TaskTemplate("精听", "机场广播精听训练", 12, Map.of("resource", "机场广播")),
                    new TaskTemplate("单词", "旅行词汇复习 20 个", 8, null)),
            "兴趣", List.of(
                    new TaskTemplate("场景对话", "餐厅点餐 · 10 分钟", 10, Map.of("scene", "餐厅点餐")),
                    new TaskTemplate("跟读", "老友记片段跟读", 15, Map.of("resource", "老友记")),
                    new TaskTemplate("精听", "BBC 六分钟英语精听", 12, Map.of("resource", "BBC")),
                    new TaskTemplate("单词", "日常词汇复习 20 个", 8, null))
    );

    private final LearningPlanRepository planRepository;
    private final DailyTaskRepository taskRepository;
    private final SceneRepository sceneRepository;
    private final LearningResourceRepository resourceRepository;
    private final LlmProvider llmProvider;

    public PlanService(LearningPlanRepository planRepository,
                       DailyTaskRepository taskRepository,
                       SceneRepository sceneRepository,
                       LearningResourceRepository resourceRepository,
                       LlmProvider llmProvider) {
        this.planRepository = planRepository;
        this.taskRepository = taskRepository;
        this.sceneRepository = sceneRepository;
        this.resourceRepository = resourceRepository;
        this.llmProvider = llmProvider;
    }

    public LearningPlan getActivePlan(User user) {
        return planRepository.findByUserIdAndPlanStatus(user.userId, "active").orElse(null);
    }

    public String currentLevelOf(LearningPlan plan) {
        return plan != null ? plan.levelCurrent : null;
    }

    public record LevelResult(String level, String summary) {}

    // 注意：不包事务。判级与口语评价都会调用 LLM（Ollama 本地推理一次约十几秒），
    // 若包在事务里会长时间占用 SQLite 写锁，易触发 SQLITE_BUSY。
    public Map<String, Object> submitEntranceTest(User user, List<PlanDtos.EntranceAnswer> answers, String targetGoal) {
        String goal = (targetGoal == null || targetGoal.isBlank()) ? "兴趣" : targetGoal;
        if (!GOAL_LIST.contains(goal)) {
            throw new ApiException(422, "目标取值应为：" + String.join("/", GOAL_LIST));
        }
        // 等级判定与「看图描述」口语评价并行调用模型，总耗时约等于单次
        List<String> texts = answers == null ? List.of()
                : answers.stream().map(a -> a.text() == null ? "" : a.text()).toList();
        var judgeFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> llmProvider.judgeLevel(texts));
        var evalFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> evalSpeaking(answers));
        LevelJudgement judgement = judgeFuture.join();
        Map<String, Object> speakingEval = evalFuture.join();

        LevelResult result = toLevelResult(judgement, answers);
        LearningPlan plan = upsertPlan(user, result.level(), goal);
        List<DailyTask> tasks = ensureTodayTasks(user, plan);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("level", result.level());
        data.put("summary", result.summary());
        data.put("targetGoal", goal);
        data.put("plan", Dtos.planToDict(plan));
        data.put("tasks", tasks.stream().map(Dtos::taskToDict).toList());
        data.put("speakingEval", speakingEval);
        return data;
    }

    private LevelResult toLevelResult(LevelJudgement judgement, List<PlanDtos.EntranceAnswer> answers) {
        int questionCount = answers == null ? 0 : answers.size();
        String comment = judgement.comment() == null || judgement.comment().isBlank()
                ? "" : judgement.comment();
        String summary = "基于本次 " + questionCount + " 题作答内容评分 " + judgement.score()
                + " 分，判定为 " + judgement.level() + " 水平。"
                + (comment.isEmpty() ? "" : " " + comment);
        return new LevelResult(judgement.level(), summary);
    }

    /**
     * 对「看图描述」作答（id=2）调用大模型做口语评价。
     * 大模型只负责口语评价与等级判定，不生成题目与图片。
     */
    private Map<String, Object> evalSpeaking(List<PlanDtos.EntranceAnswer> answers) {
        String text = null;
        if (answers != null) {
            text = answers.stream()
                    .filter(a -> a.id() == 2)
                    .map(PlanDtos.EntranceAnswer::text)
                    .filter(t -> t != null && !t.isBlank())
                    .findFirst().orElse(null);
        }
        if (text == null) {
            return null;
        }
        EvalResult r = llmProvider.evaluate(text);
        Map<String, Object> eval = new LinkedHashMap<>();
        eval.put("pron", r.pron);
        eval.put("fluency", r.fluency);
        eval.put("reaction", r.reaction);
        eval.put("natural", r.natural);
        eval.put("grammarFeedback", r.grammarFeedback);
        eval.put("phonemeIssues", r.phonemeIssues);
        eval.put("betterExpression", r.betterExpression);
        return eval;
    }

    @Transactional
    public Map<String, Object> generatePlan(User user, String targetGoal) {
        if (targetGoal == null || !GOAL_LIST.contains(targetGoal)) {
            throw new ApiException(422, "目标取值应为：" + String.join("/", GOAL_LIST));
        }
        LearningPlan old = getActivePlan(user);
        String currentLevel = old != null ? old.levelCurrent : "A1";
        LearningPlan plan = upsertPlan(user, currentLevel, targetGoal);
        List<DailyTask> tasks = ensureTodayTasks(user, plan);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("plan", Dtos.planToDict(plan));
        data.put("tasks", tasks.stream().map(Dtos::taskToDict).toList());
        return data;
    }

    @Transactional
    public Map<String, Object> todayTasks(User user) {
        LearningPlan plan = getActivePlan(user);
        Map<String, Object> data = new LinkedHashMap<>();
        if (plan == null) {
            data.put("plan", null);
            data.put("tasks", List.of());
            return data;
        }
        List<DailyTask> tasks = ensureTodayTasks(user, plan);
        data.put("plan", Dtos.planToDict(plan));
        data.put("tasks", tasks.stream().map(Dtos::taskToDict).toList());
        return data;
    }

    @Transactional
    public DailyTask toggleTask(Integer taskId, Integer userId, boolean done) {
        DailyTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null || !userId.equals(task.userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "任务不存在");
        }
        task.done = done ? 1 : 0;
        return taskRepository.save(task);
    }

    @Transactional
    public DailyTask addSceneToPlan(User user, Integer sceneId) {
        LearningPlan plan = getActivePlan(user);
        if (plan == null) throw new ApiException(404, "请先完成入学测评或生成学习方案");
        Scene scene = sceneRepository.findById(sceneId).filter(s -> Integer.valueOf(1).equals(s.status)).orElse(null);
        if (scene == null) throw new ApiException(404, "场景不存在或已下架");
        LocalDate today = LocalDate.now();
        DailyTask existing = taskRepository.findByUserIdAndTaskDateAndSceneId(user.userId, today, sceneId).orElse(null);
        if (existing != null) throw new ApiException(409, "该场景已加入今日计划");
        if (taskRepository.countByUserIdAndTaskDateAndTaskType(user.userId, today, "场景对话") >= 10) {
            throw new ApiException(422, "今日场景任务已达上限");
        }
        DailyTask task = new DailyTask();
        task.userId = user.userId;
        task.planId = plan.planId;
        task.taskType = "场景对话";
        task.title = scene.sceneName;
        task.durationMin = 10;
        task.sceneId = sceneId;
        task.done = 0;
        task.taskDate = today;
        return taskRepository.save(task);
    }

    private LearningPlan upsertPlan(User user, String level, String targetGoal) {
        LearningPlan plan = planRepository.findByUserId(user.userId).orElse(null);
        String content = JsonUtil.toJson(Map.of(
                "goal", targetGoal,
                "stages", List.of(
                        Map.of("stage", 1, "focus", "基础表达与场景开口", "tasks", 4),
                        Map.of("stage", 2, "focus", "流利度与句型丰富度提升", "tasks", 4),
                        Map.of("stage", 3, "focus", "实战综合演练与弱项专项", "tasks", 4))));
        LocalDate today = LocalDate.now();
        if (plan == null) {
            plan = new LearningPlan();
            plan.userId = user.userId;
            plan.targetGoal = targetGoal;
            plan.levelStart = level;
            plan.levelCurrent = level;
            plan.planContent = content;
            plan.planStart = today;
            plan.planStatus = "active";
            plan = planRepository.save(plan);
        } else {
            plan.targetGoal = targetGoal;
            plan.levelStart = plan.levelStart != null ? plan.levelStart : level;
            plan.levelCurrent = level;
            plan.planContent = content;
            plan.planStatus = "active";
            plan.planStart = plan.planStart != null ? plan.planStart : today;
            plan.updateTime = LocalDateTime.now();
            plan = planRepository.save(plan);
        }
        return plan;
    }

    private List<DailyTask> buildTasksForDate(User user, LearningPlan plan, LocalDate targetDate) {
        String goal = TASK_TEMPLATES.containsKey(plan.targetGoal) ? plan.targetGoal : "兴趣";
        List<TaskTemplate> templates = TASK_TEMPLATES.get(goal);
        List<DailyTask> created = new ArrayList<>();
        for (TaskTemplate t : templates) {
            if (taskRepository.existsByUserIdAndTaskDateAndTitle(user.userId, targetDate, t.title())) {
                continue;
            }
            Integer sceneId = null;
            Integer resourceId = null;
            if (t.bind() != null && t.bind().containsKey("scene")) {
                sceneId = sceneRepository.findFirstBySceneNameAndStatus(t.bind().get("scene"), 1)
                        .map(s -> s.sceneId).orElse(null);
            }
            if (t.bind() != null && t.bind().containsKey("resource")) {
                resourceId = resourceRepository.findFirstByTitleContainingAndStatus(t.bind().get("resource"), 1)
                        .map(r -> r.resourceId).orElse(null);
            }
            DailyTask task = new DailyTask();
            task.userId = user.userId;
            task.planId = plan.planId;
            task.taskType = t.type();
            task.title = t.title();
            task.durationMin = t.minutes();
            task.sceneId = sceneId;
            task.resourceId = resourceId;
            task.done = 0;
            task.taskDate = targetDate;
            created.add(taskRepository.save(task));
        }
        return created;
    }

    private List<DailyTask> ensureTodayTasks(User user, LearningPlan plan) {
        LocalDate today = LocalDate.now();
        List<DailyTask> tasks = taskRepository.findByUserIdAndTaskDateOrderByTaskId(user.userId, today);
        if (tasks.isEmpty() && plan != null) {
            tasks = buildTasksForDate(user, plan, today);
        }
        return tasks;
    }
}
