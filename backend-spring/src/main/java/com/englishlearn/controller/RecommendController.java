package com.englishlearn.controller;

import com.englishlearn.common.ApiResponse;
import com.englishlearn.entity.User;
import com.englishlearn.security.AuthFacade;
import com.englishlearn.service.LevelPredictService;
import com.englishlearn.service.LevelPredictService.LevelPrediction;
import com.englishlearn.service.RecommendService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 个性化推荐接口（/recommend，需登录）。
 */
@RestController
@RequestMapping("/recommend")
public class RecommendController {

    private final RecommendService recommendService;
    private final LevelPredictService levelPredictService;
    private final AuthFacade authFacade;

    public RecommendController(RecommendService recommendService,
                               LevelPredictService levelPredictService,
                               AuthFacade authFacade) {
        this.recommendService = recommendService;
        this.levelPredictService = levelPredictService;
        this.authFacade = authFacade;
    }

    /** 水平预测详情：等级、档位、各档概率、置信度、特征 */
    @GetMapping("/profile")
    public ApiResponse profile() {
        User user = authFacade.requireUser();
        return ApiResponse.ok(levelPredictService.predict(user).toMap());
    }

    /** 一次返回水平预测 + 场景 / 资源 / 任务三类推荐 */
    @GetMapping("/overview")
    public ApiResponse overview(@RequestParam(defaultValue = "6") int limit) {
        User user = authFacade.requireUser();
        LevelPrediction prediction = levelPredictService.predict(user);
        int capped = Math.max(1, Math.min(limit, 20));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("level", prediction.toMap());
        data.put("scenes", recommendService.recommendScenes(user, prediction, capped));
        data.put("resources", recommendService.recommendResources(user, prediction, capped));
        data.put("tasks", recommendService.recommendTasks(user, prediction, capped));
        data.put("strategies", recommendService.strategyStatus());
        return ApiResponse.ok(data);
    }

    @GetMapping("/scenes")
    public ApiResponse scenes(@RequestParam(defaultValue = "6") int limit) {
        User user = authFacade.requireUser();
        LevelPrediction prediction = levelPredictService.predict(user);
        return ApiResponse.ok(recommendService.recommendScenes(user, prediction, cap(limit)));
    }

    @GetMapping("/resources")
    public ApiResponse resources(@RequestParam(defaultValue = "6") int limit) {
        User user = authFacade.requireUser();
        LevelPrediction prediction = levelPredictService.predict(user);
        return ApiResponse.ok(recommendService.recommendResources(user, prediction, cap(limit)));
    }

    @GetMapping("/tasks")
    public ApiResponse tasks(@RequestParam(defaultValue = "6") int limit) {
        User user = authFacade.requireUser();
        LevelPrediction prediction = levelPredictService.predict(user);
        return ApiResponse.ok(recommendService.recommendTasks(user, prediction, cap(limit)));
    }

    private static int cap(int limit) {
        return Math.max(1, Math.min(limit, 20));
    }
}
