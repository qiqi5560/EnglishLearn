package com.englishlearn.service;

import com.englishlearn.common.ApiException;
import com.englishlearn.common.JsonUtil;
import com.englishlearn.entity.AssessmentRecord;
import com.englishlearn.entity.ConversationMessage;
import com.englishlearn.entity.ConversationSession;
import com.englishlearn.entity.Scene;
import com.englishlearn.entity.StudyRecord;
import com.englishlearn.entity.User;
import com.englishlearn.llm.EvalResult;
import com.englishlearn.llm.LlmProvider;
import com.englishlearn.llm.Reply;
import com.englishlearn.llm.SummaryResult;
import com.englishlearn.repository.AssessmentRecordRepository;
import com.englishlearn.repository.ConversationMessageRepository;
import com.englishlearn.repository.ConversationSessionRepository;
import com.englishlearn.repository.StudyRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话会话服务（F002/F003）：会话生命周期与消息收发。
 */
@Service
public class DialogueService {

    private static final int MAX_CONTEXT = 6;

    private final ConversationSessionRepository sessionRepository;
    private final ConversationMessageRepository messageRepository;
    private final AssessmentRecordRepository assessmentRepository;
    private final StudyRecordRepository studyRecordRepository;
    private final LlmProvider llmProvider;

    public DialogueService(ConversationSessionRepository sessionRepository,
                           ConversationMessageRepository messageRepository,
                           AssessmentRecordRepository assessmentRepository,
                           StudyRecordRepository studyRecordRepository,
                           LlmProvider llmProvider) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.assessmentRepository = assessmentRepository;
        this.studyRecordRepository = studyRecordRepository;
        this.llmProvider = llmProvider;
    }

    @Transactional
    public ConversationSession startSession(User user, Scene scene, String mode) {
        ConversationSession session = new ConversationSession();
        session.userId = user.userId;
        session.scene = scene;
        session.mode = (mode == null || mode.isBlank()) ? "scenario" : mode;
        session.sessionStatus = "ongoing";
        session.startTime = LocalDateTime.now();
        session = sessionRepository.save(session);

        String role = roleOf(scene);
        Reply opening;
        if (scene != null) {
            opening = llmProvider.opening(scene.sceneName, scene.sceneDesc, role);
        } else {
            opening = llmProvider.opening("自由对话", "", "AI 教练");
        }
        ConversationMessage msg = new ConversationMessage();
        msg.sessionId = session.sessionId;
        msg.speaker = "ai";
        msg.contentEn = opening.en();
        msg.contentZh = opening.zh();
        msg.msgTime = LocalDateTime.now();
        messageRepository.save(msg);
        return session;
    }

    @Transactional
    public SendResult sendMessage(ConversationSession session, String content) {
        if (!"ongoing".equals(session.sessionStatus)) {
            throw new ApiException(HttpStatus.CONFLICT, "会话已结束，无法继续发送消息");
        }
        Scene scene = session.scene;

        ConversationMessage userMsg = new ConversationMessage();
        userMsg.sessionId = session.sessionId;
        userMsg.speaker = "user";
        userMsg.contentEn = content.strip();
        userMsg.msgTime = LocalDateTime.now();
        userMsg = messageRepository.save(userMsg);

        String role = roleOf(scene);
        List<ConversationMessage> history = recentMessages(session.sessionId);
        Reply reply = llmProvider.reply(
                scene != null ? scene.sceneName : "自由对话",
                scene != null ? scene.sceneDesc : "",
                role,
                history,
                content);

        ConversationMessage aiMsg = new ConversationMessage();
        aiMsg.sessionId = session.sessionId;
        aiMsg.speaker = "ai";
        aiMsg.contentEn = reply.en();
        aiMsg.contentZh = reply.zh();
        aiMsg.msgTime = LocalDateTime.now();
        aiMsg = messageRepository.save(aiMsg);

        EvalResult eval = llmProvider.evaluate(content);
        AssessmentRecord record = new AssessmentRecord();
        record.sessionId = session.sessionId;
        record.messageId = userMsg.messageId;
        record.pronScore = eval.pron;
        record.fluencyScore = eval.fluency;
        record.reactionScore = eval.reaction;
        record.naturalScore = eval.natural;
        record.grammarFeedback = eval.grammarFeedback;
        record.phonemeIssues = eval.phonemeIssues.isEmpty() ? null : JsonUtil.toJson(eval.phonemeIssues);
        record.betterExpression = eval.betterExpression;
        record.assessTime = LocalDateTime.now();
        assessmentRepository.save(record);

        Map<String, Object> liveScores = new LinkedHashMap<>();
        liveScores.put("pron", eval.pron);
        liveScores.put("fluency", eval.fluency);
        liveScores.put("reaction", eval.reaction);
        liveScores.put("natural", eval.natural);
        return new SendResult(userMsg, aiMsg, liveScores);
    }

    @Transactional
    public Map<String, Object> finishSession(ConversationSession session) {
        if ("finished".equals(session.sessionStatus)) {
            Map<String, Object> parsed = JsonUtil.parseMap(session.aiSummary);
            return parsed != null ? parsed : Map.of("summary", Map.of());
        }

        LocalDateTime now = LocalDateTime.now();
        int durationSec = session.startTime != null
                ? Math.max(0, (int) java.time.Duration.between(session.startTime, now).getSeconds())
                : 0;
        session.endTime = now;
        session.durationSec = durationSec;
        session.sessionStatus = "finished";

        List<ConversationMessage> messages = messageRepository.findBySessionIdOrdered(session.sessionId);
        List<AssessmentRecord> assessments = assessmentRepository.findBySessionIdOrderByAssessId(session.sessionId);

        SummaryResult result = llmProvider.summarize(messages, Collections.emptyList());
        if (!assessments.isEmpty()) {
            double mp = assessments.stream().mapToDouble(a -> a.pronScore).average().orElse(0);
            double mf = assessments.stream().mapToDouble(a -> a.fluencyScore).average().orElse(0);
            double mr = assessments.stream().mapToDouble(a -> a.reactionScore).average().orElse(0);
            double mn = assessments.stream().mapToDouble(a -> a.naturalScore).average().orElse(0);
            result.total = round1(mp * 0.35 + mf * 0.25 + mr * 0.2 + mn * 0.2);
            result.pron = round1(mp);
            result.fluency = round1(mf);
            result.reaction = round1(mr);
            result.natural = round1(mn);
        }

        Map<String, Object> dimensions = new LinkedHashMap<>();
        dimensions.put("pron", result.pron);
        dimensions.put("fluency", result.fluency);
        dimensions.put("reaction", result.reaction);
        dimensions.put("natural", result.natural);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", session.sessionId);
        payload.put("total", result.total);
        payload.put("dimensions", dimensions);
        payload.put("highlights", result.highlights);
        payload.put("improvements", result.improvements);
        payload.put("suggestions", result.suggestions);
        payload.put("corrections", result.corrections);
        payload.put("feedbackText", result.feedbackText);
        payload.put("durationSec", durationSec);
        session.aiSummary = JsonUtil.toJson(payload);
        sessionRepository.save(session);

        int durationMin = Math.max(1, (durationSec + 59) / 60);
        StudyRecord record = new StudyRecord();
        record.userId = session.userId;
        record.sessionId = session.sessionId;
        record.actionType = "scenario";
        record.durationMin = durationMin;
        record.score = result.total;
        record.learnDate = now.toLocalDate();
        studyRecordRepository.save(record);

        return payload;
    }

    public Map<String, Object> getSessionSummary(ConversationSession session) {
        Map<String, Object> parsed = JsonUtil.parseMap(session.aiSummary);
        if (parsed == null) {
            throw new ApiException(HttpStatus.CONFLICT, "会话尚未结束，暂无小结");
        }
        return parsed;
    }

    private List<ConversationMessage> recentMessages(Integer sessionId) {
        List<ConversationMessage> list = new ArrayList<>(messageRepository.findTop6BySessionIdOrderByMessageIdDesc(sessionId));
        Collections.reverse(list);
        return list;
    }

    private String roleOf(Scene scene) {
        if (scene == null) {
            return "";
        }
        Map<String, Object> roleSetting = JsonUtil.parseMap(scene.roleSetting);
        if (roleSetting == null || roleSetting.get("role") == null) {
            return "";
        }
        return String.valueOf(roleSetting.get("role"));
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    public record SendResult(ConversationMessage userMessage, ConversationMessage aiMessage, Map<String, Object> liveScores) {}
}