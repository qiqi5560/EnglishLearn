package com.englishlearn.controller;

import com.englishlearn.common.ApiResponse;
import com.englishlearn.dto.AdminDtos;
import com.englishlearn.entity.User;
import com.englishlearn.security.AuthFacade;
import com.englishlearn.service.AdminInsightService;
import com.englishlearn.service.AdminService;
import com.englishlearn.service.AuditLogService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理后台接口（/admin，全部需要管理员权限）。
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final AdminInsightService insightService;
    private final AuditLogService auditLog;
    private final AuthFacade authFacade;

    public AdminController(AdminService adminService,
                           AdminInsightService insightService,
                           AuditLogService auditLog,
                           AuthFacade authFacade) {
        this.adminService = adminService;
        this.insightService = insightService;
        this.auditLog = auditLog;
        this.authFacade = authFacade;
    }

    // ============================================================
    // 数据看板
    // ============================================================

    @GetMapping("/dashboard")
    public ApiResponse dashboard() {
        authFacade.requireAdmin();
        return ApiResponse.ok(adminService.dashboard());
    }

    // ============================================================
    // 运营洞察：用户使用报表 / 日活动量 / 算力监控 / 系统概览
    // ============================================================

    @GetMapping("/usage-report")
    public ApiResponse usageReport(@RequestParam(defaultValue = "30") int days,
                                   @RequestParam(required = false) String keyword) {
        authFacade.requireAdmin();
        return ApiResponse.ok(insightService.usageReport(days, keyword));
    }

    @GetMapping("/activity")
    public ApiResponse activity(@RequestParam(defaultValue = "14") int days) {
        authFacade.requireAdmin();
        return ApiResponse.ok(insightService.activity(days));
    }

    @GetMapping("/compute")
    public ApiResponse compute() {
        authFacade.requireAdmin();
        return ApiResponse.ok(insightService.compute());
    }

    @GetMapping("/system")
    public ApiResponse system() {
        authFacade.requireAdmin();
        return ApiResponse.ok(insightService.systemOverview());
    }

    /**
     * 操作日志：管理员写操作的追溯视图（进程内保留最近 200 条）。
     * module 为空表示查看全部模块。
     */
    @GetMapping("/audit-logs")
    public ApiResponse auditLogs(@RequestParam(required = false) String module,
                                 @RequestParam(defaultValue = "100") int limit) {
        authFacade.requireAdmin();
        int capped = Math.max(1, Math.min(limit, 200));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", auditLog.size());
        data.put("modules", auditLog.moduleStats());
        data.put("list", auditLog.list(module, capped));
        return ApiResponse.ok(data);
    }

    // ============================================================
    // 场景管理
    // ============================================================

    @GetMapping("/scenes")
    public ApiResponse listScenes(@RequestParam(required = false) String keyword) {
        authFacade.requireAdmin();
        return ApiResponse.ok(adminService.listScenes(keyword));
    }

    @PutMapping("/scenes/{sceneId}")
    public ApiResponse updateScene(@PathVariable Integer sceneId, @RequestBody AdminDtos.SceneUpdateIn body) {
        User admin = authFacade.requireAdmin();
        Object data = adminService.updateScene(sceneId, body);
        auditLog.record(admin, "场景管理", "更新场景", "场景 #" + sceneId);
        return ApiResponse.ok(data, "场景已更新");
    }

    @PostMapping("/scenes")
    public ApiResponse createScene(@RequestBody AdminDtos.SceneIn body) {
        User admin = authFacade.requireAdmin();
        Object data = adminService.createScene(body);
        auditLog.record(admin, "场景管理", "新增场景", String.valueOf(body.sceneName()));
        return ApiResponse.ok(data, "场景已添加，所有用户立即可见");
    }

    @DeleteMapping("/scenes/{sceneId}")
    public ApiResponse deleteScene(@PathVariable Integer sceneId) {
        User admin = authFacade.requireAdmin();
        adminService.deleteScene(sceneId);
        auditLog.record(admin, "场景管理", "删除场景", "场景 #" + sceneId);
        return ApiResponse.ok(null, "场景已删除");
    }

    // ============================================================
    // 资源管理
    // ============================================================

    @GetMapping("/resources")
    public ApiResponse listResources(@RequestParam(required = false) String keyword,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int pageSize) {
        authFacade.requireAdmin();
        return ApiResponse.ok(adminService.listResources(keyword, page, pageSize));
    }

    @PostMapping("/resources")
    public ApiResponse createResource(@RequestBody AdminDtos.ResourceIn body) {
        User admin = authFacade.requireAdmin();
        Object data = adminService.createResource(body, admin);
        auditLog.record(admin, "资源管理", "新增资源", String.valueOf(body.title()));
        return ApiResponse.ok(data, "资源已添加");
    }

    @PutMapping("/resources/{resourceId}")
    public ApiResponse updateResource(@PathVariable Integer resourceId, @RequestBody AdminDtos.ResourceUpdateIn body) {
        User admin = authFacade.requireAdmin();
        Object data = adminService.updateResource(resourceId, body);
        auditLog.record(admin, "资源管理", "更新资源", "资源 #" + resourceId);
        return ApiResponse.ok(data, "资源已更新");
    }

    @DeleteMapping("/resources/{resourceId}")
    public ApiResponse deleteResource(@PathVariable Integer resourceId) {
        User admin = authFacade.requireAdmin();
        adminService.deleteResource(resourceId);
        auditLog.record(admin, "资源管理", "删除资源", "资源 #" + resourceId);
        return ApiResponse.ok(null, "资源已删除");
    }

    // ============================================================
    // 社区管理
    // ============================================================

    @GetMapping("/posts")
    public ApiResponse listPosts(@RequestParam(value = "status", required = false) Integer status,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int pageSize) {
        authFacade.requireAdmin();
        return ApiResponse.ok(adminService.listPosts(status, page, pageSize));
    }

    @PutMapping("/posts/{postId}")
    public ApiResponse reviewPost(@PathVariable Integer postId, @RequestBody AdminDtos.PostReviewIn body) {
        User admin = authFacade.requireAdmin();
        Object data = adminService.reviewPost(postId, body);
        auditLog.record(admin, "社区管理", "审核帖子", "帖子 #" + postId + " → 状态 " + body.status());
        return ApiResponse.ok(data, "操作成功");
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse deletePost(@PathVariable Integer postId) {
        User admin = authFacade.requireAdmin();
        adminService.deletePost(postId);
        auditLog.record(admin, "社区管理", "删除帖子", "帖子 #" + postId);
        return ApiResponse.ok(null, "删除成功");
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse deleteComment(@PathVariable Integer commentId) {
        User admin = authFacade.requireAdmin();
        adminService.deleteComment(commentId);
        auditLog.record(admin, "社区管理", "删除评论", "评论 #" + commentId);
        return ApiResponse.ok(null, "评论已删除");
    }

    // ============================================================
    // 用户管理
    // ============================================================

    @GetMapping("/users")
    public ApiResponse listUsers(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String role,
                                 @RequestParam(required = false) Boolean punished,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int pageSize) {
        authFacade.requireAdmin();
        return ApiResponse.ok(adminService.listUsers(keyword, role, punished, page, pageSize));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse updateUser(@PathVariable Integer userId, @RequestBody AdminDtos.UserUpdateIn body) {
        User admin = authFacade.requireAdmin();
        Object data = adminService.updateUser(userId, body, admin);
        auditLog.record(admin, "用户管理", "更新用户", "用户 #" + userId);
        return ApiResponse.ok(data, "用户已更新");
    }

    @PostMapping("/users/{userId}/reset-password")
    public ApiResponse resetPassword(@PathVariable Integer userId, @RequestBody(required = false) AdminDtos.ResetPasswordIn body) {
        User admin = authFacade.requireAdmin();
        adminService.resetPassword(userId, body == null ? null : body.password());
        auditLog.record(admin, "用户管理", "重置密码", "用户 #" + userId);
        return ApiResponse.ok(null, "密码已重置");
    }

    @PostMapping("/users/{userId}/ban")
    public ApiResponse banUser(@PathVariable Integer userId, @RequestBody AdminDtos.BanIn body) {
        User admin = authFacade.requireAdmin();
        Object data = adminService.banUser(userId, body, admin);
        auditLog.record(admin, "用户管理", "发帖处罚",
                "用户 #" + userId + " 禁言 " + (body == null ? 1 : body.days()) + " 天");
        return ApiResponse.ok(data, "已对该用户发起处罚");
    }

    @PostMapping("/users/{userId}/unban")
    public ApiResponse unbanUser(@PathVariable Integer userId) {
        User admin = authFacade.requireAdmin();
        Object data = adminService.unbanUser(userId);
        auditLog.record(admin, "用户管理", "解除处罚", "用户 #" + userId);
        return ApiResponse.ok(data, "已解除处罚");
    }

    // ============================================================
    // 名句素材管理
    // ============================================================

    @GetMapping("/quotes")
    public ApiResponse listQuotes(@RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Integer builtin) {
        authFacade.requireAdmin();
        return ApiResponse.ok(adminService.listQuotes(keyword, builtin));
    }

    @PostMapping("/quotes")
    public ApiResponse createQuote(@RequestBody AdminDtos.QuoteIn body) {
        User admin = authFacade.requireAdmin();
        Object data = adminService.createQuote(body);
        auditLog.record(admin, "名句素材", "新增名句", String.valueOf(body.title()));
        return ApiResponse.ok(data, "名句已添加");
    }

    @PutMapping("/quotes/{quoteId}")
    public ApiResponse updateQuote(@PathVariable Integer quoteId, @RequestBody AdminDtos.QuoteIn body) {
        User admin = authFacade.requireAdmin();
        Object data = adminService.updateQuote(quoteId, body);
        auditLog.record(admin, "名句素材", "更新名句", "名句 #" + quoteId);
        return ApiResponse.ok(data, "名句已更新");
    }

    @DeleteMapping("/quotes/{quoteId}")
    public ApiResponse deleteQuote(@PathVariable Integer quoteId) {
        User admin = authFacade.requireAdmin();
        adminService.deleteQuote(quoteId);
        auditLog.record(admin, "名句素材", "删除名句", "名句 #" + quoteId);
        return ApiResponse.ok(null, "名句已删除");
    }

    // ============================================================
    // 系统配置
    // ============================================================

    @GetMapping("/configs")
    public ApiResponse getConfigs() {
        authFacade.requireAdmin();
        return ApiResponse.ok(adminService.getConfigs());
    }

    @PutMapping("/configs")
    public ApiResponse updateConfigs(@RequestBody Map<String, Object> body) {
        User admin = authFacade.requireAdmin();
        Object data = adminService.updateConfigs(body);
        auditLog.record(admin, "系统配置", "更新配置", "系统全局配置");
        return ApiResponse.ok(data, "配置已保存");
    }
}
