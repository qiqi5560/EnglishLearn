package com.englishlearn.controller;

import com.englishlearn.common.ApiResponse;
import com.englishlearn.entity.User;
import com.englishlearn.security.AuthFacade;
import com.englishlearn.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习报表接口（/reports）。
 */
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final AuthFacade authFacade;

    public ReportController(ReportService reportService, AuthFacade authFacade) {
        this.reportService = reportService;
        this.authFacade = authFacade;
    }

    @GetMapping("/overview")
    public ApiResponse overview() {
        User user = authFacade.requireUser();
        return ApiResponse.ok(reportService.overview(user));
    }
}