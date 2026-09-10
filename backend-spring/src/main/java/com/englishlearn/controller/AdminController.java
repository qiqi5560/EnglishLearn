package com.englishlearn.controller;

import com.englishlearn.common.ApiResponse;
import com.englishlearn.dto.AdminDtos;
import com.englishlearn.entity.User;
import com.englishlearn.security.AuthFacade;
import com.englishlearn.service.AdminService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理后台接口（/admin，全部需要管理员权限）。
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuthFacade authFacade;

    public AdminController(AdminService adminService, AuthFacade authFacade) {
        this.adminService = adminService;
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
    // 场景管理
    // ============================================================

    @GetMapping("/scenes")
    public ApiResponse listScenes(@RequestParam(required = false) String keyword) {
        authFacade.requireAdmin();
        return ApiResponse.ok(adminService.listScenes(keyword));
    }

    @PutMapping("/scenes/{sceneId}")
    public ApiResponse updateScene(@PathVariable Integer sceneId, @RequestBody AdminDtos.SceneUpdateIn body) {
        authFacade.requireAdmin();
        return ApiResponse.ok(adminService.updateScene(sceneId, body), "场景已更新");
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
        return ApiResponse.ok(adminService.createResource(body, admin), "资源已添加");
    }

    @PutMapping("/resources/{resourceId}")
    public ApiResponse updateResource(@PathVariable Integer resourceId, @RequestBody AdminDtos.ResourceUpdateIn body) {
        authFacade.requireAdmin();
        return ApiResponse.ok(adminService.updateResource(resourceId, body), "资源已更新");
    }

    @DeleteMapping("/resources/{resourceId}")
    public ApiResponse deleteResource(@PathVariable Integer resourceId) {
        authFacade.requireAdmin();
        adminService.deleteResource(resourceId);
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
        authFacade.requireAdmin();
        return ApiResponse.ok(adminService.reviewPost(postId, body), "操作成功");
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse deletePost(@PathVariable Integer postId) {
        authFacade.requireAdmin();
        adminService.deletePost(postId);
        return ApiResponse.ok(null, "删除成功");
    }

    // ============================================================
    // 用户管理
    // ============================================================

    @GetMapping("/users")
    public ApiResponse listUsers(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String role,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int pageSize) {
        authFacade.requireAdmin();
        return ApiResponse.ok(adminService.listUsers(keyword, role, page, pageSize));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse updateUser(@PathVariable Integer userId, @RequestBody AdminDtos.UserUpdateIn body) {
        User admin = authFacade.requireAdmin();
        return ApiResponse.ok(adminService.updateUser(userId, body, admin), "用户已更新");
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
        authFacade.requireAdmin();
        return ApiResponse.ok(adminService.updateConfigs(body), "配置已保存");
    }
}