package com.englishlearn.controller;

import com.englishlearn.common.ApiResponse;
import com.englishlearn.dto.CommunityDtos;
import com.englishlearn.entity.User;
import com.englishlearn.security.AuthFacade;
import com.englishlearn.service.CommunityService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习社区接口（/community）：帖子、点赞、评论、话题。
 */
@RestController
@RequestMapping("/community")
public class CommunityController {

    private final CommunityService communityService;
    private final AuthFacade authFacade;

    public CommunityController(CommunityService communityService, AuthFacade authFacade) {
        this.communityService = communityService;
        this.authFacade = authFacade;
    }

    @GetMapping("/topics")
    public ApiResponse topics() {
        return ApiResponse.ok(communityService.topics());
    }

    @GetMapping("/posts")
    public ApiResponse listPosts(@RequestParam(required = false) String topic,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int pageSize) {
        User user = authFacade.requireUser();
        return ApiResponse.ok(communityService.listPosts(user, topic, page, pageSize));
    }

    @PostMapping("/posts")
    public ApiResponse createPost(@RequestBody CommunityDtos.PostCreateIn body) {
        User user = authFacade.requireUser();
        return ApiResponse.ok(communityService.createPost(user, body.title(), body.content(), body.topic()), "发布成功");
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse postDetail(@PathVariable Integer postId) {
        User user = authFacade.requireUser();
        return ApiResponse.ok(communityService.postDetail(user, postId));
    }

    @PostMapping("/posts/{postId}/like")
    public ApiResponse toggleLike(@PathVariable Integer postId) {
        User user = authFacade.requireUser();
        return ApiResponse.ok(communityService.toggleLike(user, postId));
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse addComment(@PathVariable Integer postId, @RequestBody CommunityDtos.CommentIn body) {
        User user = authFacade.requireUser();
        return ApiResponse.ok(communityService.addComment(user, postId, body.content()), "评论成功");
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse deletePost(@PathVariable Integer postId) {
        User user = authFacade.requireUser();
        communityService.deletePost(user, postId);
        return ApiResponse.ok(null, "删除成功");
    }
}