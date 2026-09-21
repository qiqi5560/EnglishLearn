package com.englishlearn.controller;

import com.englishlearn.common.ApiResponse;
import com.englishlearn.dto.SocialDtos;
import com.englishlearn.entity.User;
import com.englishlearn.security.AuthFacade;
import com.englishlearn.service.ShareService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内容分享接口（/share）：复制链接留痕，按渠道统计，不接入第三方 SDK。
 */
@RestController
@RequestMapping("/share")
public class ShareController {

    private final AuthFacade authFacade;
    private final ShareService shareService;

    public ShareController(AuthFacade authFacade, ShareService shareService) {
        this.authFacade = authFacade;
        this.shareService = shareService;
    }

    @PostMapping
    public ApiResponse share(@RequestBody SocialDtos.ShareIn body) {
        User user = authFacade.requireUser();
        return ApiResponse.ok(shareService.record(user, body.contentType(), body.contentId(),
                body.channel(), body.url()), "链接已复制");
    }

    @GetMapping("/mine")
    public ApiResponse mine() {
        User user = authFacade.requireUser();
        return ApiResponse.ok(shareService.myShares(user));
    }
}
