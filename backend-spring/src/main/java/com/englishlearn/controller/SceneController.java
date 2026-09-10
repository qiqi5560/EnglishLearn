package com.englishlearn.controller;

import com.englishlearn.common.ApiException;
import com.englishlearn.common.ApiResponse;
import com.englishlearn.dto.Dtos;
import com.englishlearn.entity.Scene;
import com.englishlearn.service.SceneService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对话场景浏览接口（/scenes）。
 */
@RestController
@RequestMapping("/scenes")
public class SceneController {

    private final SceneService sceneService;

    public SceneController(SceneService sceneService) {
        this.sceneService = sceneService;
    }

    @GetMapping
    public ApiResponse listScenes(@RequestParam(required = false) String category,
                                  @RequestParam(required = false) String level,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(sceneService.listScenes(category, level, keyword, page, pageSize));
    }

    @GetMapping("/recommended")
    public ApiResponse recommended(@RequestParam(defaultValue = "6") int limit) {
        return ApiResponse.ok(sceneService.recommended(limit));
    }

    @GetMapping("/{sceneId}")
    public ApiResponse detail(@PathVariable Integer sceneId) {
        Scene scene = sceneService.getAvailableScene(sceneId);
        if (scene == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "场景不存在或已下架");
        }
        return ApiResponse.ok(Dtos.sceneToDict(scene));
    }
}