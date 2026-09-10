package com.englishlearn.controller;

import com.englishlearn.common.ApiException;
import com.englishlearn.common.ApiResponse;
import com.englishlearn.common.JsonUtil;
import com.englishlearn.common.TimeUtil;
import com.englishlearn.dto.DialogueDtos;
import com.englishlearn.dto.Dtos;
import com.englishlearn.entity.ConversationMessage;
import com.englishlearn.entity.ConversationSession;
import com.englishlearn.entity.Scene;
import com.englishlearn.entity.User;
import com.englishlearn.repository.ConversationMessageRepository;
import com.englishlearn.repository.ConversationSessionRepository;
import com.englishlearn.security.AuthFacade;
import com.englishlearn.service.DialogueService;
import com.englishlearn.service.SceneService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话会话接口（/dialogues）：创建会话、收发消息、实时评测、结束与小结。
 */
@RestController
@RequestMapping("/dialogues")
public class DialogueController {

    private final DialogueService dialogueService;
    private final SceneService sceneService;
    private final ConversationSessionRepository sessionRepository;
    private final ConversationMessageRepository messageRepository;
    private final AuthFacade authFacade;

    public DialogueController(DialogueService dialogueService,
                              SceneService sceneService,
                              ConversationSessionRepository sessionRepository,
                              ConversationMessageRepository messageRepository,
                              AuthFacade authFacade) {
        this.dialogueService = dialogueService;
        this.sceneService = sceneService;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.authFacade = authFacade;
    }

    @PostMapping("/sessions")
    public ApiResponse createSession(@RequestBody DialogueDtos.SessionCreateIn body) {
        User user = authFacade.requireUser();
        Scene scene = null;
        if (body.sceneId() != null) {
            scene = sceneService.getAvailableScene(body.sceneId());
            if (scene == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "场景不存在或已下架");
            }
        }
        ConversationSession session = dialogueService.startSession(user, scene, body.mode());
        List<ConversationMessage> messages = messageRepository.findBySessionIdOrdered(session.sessionId);
        return ApiResponse.ok(Dtos.sessionToDict(session, messages), "会话已创建");
    }

    @GetMapping("/sessions")
    public ApiResponse mySessions(@RequestParam(required = false) String status,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int pageSize) {
        User user = authFacade.requireUser();
        Page<ConversationSession> result = sessionRepository.mySessions(user.userId, status, PageRequest.of(page - 1, pageSize));
        List<Map<String, Object>> items = new ArrayList<>();
        for (ConversationSession s : result.getContent()) {
            Map<String, Object> summary = JsonUtil.parseMap(s.aiSummary);
            Double total = summary != null && summary.get("total") != null
                    ? Double.parseDouble(String.valueOf(summary.get("total"))) : null;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sessionId", s.sessionId);
            item.put("sceneName", s.scene != null ? s.scene.sceneName : "自由对话");
            item.put("mode", s.mode);
            item.put("status", s.sessionStatus);
            item.put("total", total);
            item.put("durationSec", s.durationSec);
            item.put("startTime", TimeUtil.iso(s.startTime));
            item.put("endTime", TimeUtil.iso(s.endTime));
            items.add(item);
        }
        return ApiResponse.ok(Map.of("list", items, "total", items.size()));
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse sessionMessages(@PathVariable Integer sessionId) {
        User user = authFacade.requireUser();
        ConversationSession session = ownedSession(sessionId, user);
        List<ConversationMessage> messages = messageRepository.findBySessionIdOrdered(session.sessionId);
        return ApiResponse.ok(Map.of("session", Dtos.sessionToDict(session, messages)));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse postMessage(@PathVariable Integer sessionId, @RequestBody DialogueDtos.MessageIn body) {
        User user = authFacade.requireUser();
        ConversationSession session = ownedSession(sessionId, user);
        if (body.content() == null || body.content().isBlank()) {
            throw new ApiException(422, "消息内容不能为空");
        }
        DialogueService.SendResult result = dialogueService.sendMessage(session, body.content());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userMessage", Dtos.messageToDict(result.userMessage()));
        data.put("aiMessage", Dtos.messageToDict(result.aiMessage()));
        data.put("liveScores", result.liveScores());
        return ApiResponse.ok(data, "ok");
    }

    @PostMapping("/sessions/{sessionId}/finish")
    public ApiResponse finishSession(@PathVariable Integer sessionId) {
        User user = authFacade.requireUser();
        ConversationSession session = ownedSession(sessionId, user);
        return ApiResponse.ok(dialogueService.finishSession(session), "会话已结束，小结已生成");
    }

    @GetMapping("/sessions/{sessionId}/summary")
    public ApiResponse sessionSummary(@PathVariable Integer sessionId) {
        User user = authFacade.requireUser();
        ConversationSession session = ownedSession(sessionId, user);
        return ApiResponse.ok(dialogueService.getSessionSummary(session));
    }

    private ConversationSession ownedSession(Integer sessionId, User user) {
        ConversationSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null || !user.userId.equals(session.userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "会话不存在");
        }
        return session;
    }
}