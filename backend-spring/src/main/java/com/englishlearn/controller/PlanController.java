package com.englishlearn.controller;

import com.englishlearn.common.ApiException;
import com.englishlearn.common.ApiResponse;
import com.englishlearn.dto.Dtos;
import com.englishlearn.dto.PlanDtos;
import com.englishlearn.entity.DailyTask;
import com.englishlearn.entity.LearningPlan;
import com.englishlearn.entity.User;
import com.englishlearn.security.AuthFacade;
import com.englishlearn.service.PlanService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 个性化方案接口（/plans）：入学测评、当前方案、每日任务闭环。
 */
@RestController
@RequestMapping("/plans")
public class PlanController {

    private final PlanService planService;
    private final AuthFacade authFacade;

    public PlanController(PlanService planService, AuthFacade authFacade) {
        this.planService = planService;
        this.authFacade = authFacade;
    }

    @PostMapping("/entrance-test")
    public ApiResponse submitEntranceTest(@RequestBody PlanDtos.EntranceTestIn body) {
        User user = authFacade.requireUser();
        return ApiResponse.ok(planService.submitEntranceTest(user, body.answers(), body.targetGoal()), "测评完成，方案已生成");
    }

    @GetMapping("/current")
    public ApiResponse currentPlan() {
        User user = authFacade.requireUser();
        LearningPlan plan = planService.getActivePlan(user);
        return ApiResponse.ok(plan != null ? Dtos.planToDict(plan) : null);
    }

    @GetMapping("/level")
    public ApiResponse currentLevel() {
        User user = authFacade.requireUser();
        LearningPlan plan = planService.getActivePlan(user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("level", plan != null ? plan.levelCurrent : null);
        return ApiResponse.ok(data);
    }

    @PostMapping("/generate")
    public ApiResponse generatePlan(@RequestBody PlanDtos.GeneratePlanIn body) {
        User user = authFacade.requireUser();
        if (body.targetGoal() == null || !PlanService.GOAL_LIST.contains(body.targetGoal())) {
            throw new ApiException(422, "目标取值应为：" + String.join("/", PlanService.GOAL_LIST));
        }
        return ApiResponse.ok(planService.generatePlan(user, body.targetGoal()), "方案已更新");
    }

    @GetMapping("/today-tasks")
    public ApiResponse todayTasks() {
        User user = authFacade.requireUser();
        return ApiResponse.ok(planService.todayTasks(user));
    }

    @PutMapping("/tasks/{taskId}")
    public ApiResponse toggleTask(@PathVariable Integer taskId, @RequestBody PlanDtos.TaskUpdateIn body) {
        User user = authFacade.requireUser();
        boolean done = body.done() == null || body.done();
        DailyTask task = planService.toggleTask(taskId, user.userId, done);
        return ApiResponse.ok(Dtos.taskToDict(task), "任务状态已更新");
    }

    @PostMapping("/scenes/{sceneId}/add")
    public ApiResponse addSceneToPlan(@PathVariable Integer sceneId) {
        User user = authFacade.requireUser();
        return ApiResponse.ok(Dtos.taskToDict(planService.addSceneToPlan(user, sceneId)), "已加入今日计划");
    }
}
