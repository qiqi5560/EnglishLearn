package com.englishlearn.controller;

import com.englishlearn.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查接口（/health）。
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse health() {
        return ApiResponse.ok(Map.of("service", "backend", "status", "running"), "ok");
    }
}