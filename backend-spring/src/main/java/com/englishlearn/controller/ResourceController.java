package com.englishlearn.controller;

import com.englishlearn.common.ApiException;
import com.englishlearn.common.ApiResponse;
import com.englishlearn.dto.Dtos;
import com.englishlearn.entity.LearningResource;
import com.englishlearn.service.ResourceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习资源接口（/resources）。
 */
@RestController
@RequestMapping("/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public ApiResponse listResources(@RequestParam(required = false) String type,
                                     @RequestParam(required = false) String category,
                                     @RequestParam(required = false) String level,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(resourceService.listResources(type, category, level, keyword, page, pageSize));
    }

    @GetMapping("/{resourceId}")
    public ApiResponse detail(@PathVariable Integer resourceId) {
        LearningResource item = resourceService.getAvailableResource(resourceId);
        if (item == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "资源不存在或已下架");
        }
        return ApiResponse.ok(Dtos.resourceToDict(item));
    }
}