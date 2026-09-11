package com.englishlearn.llm;

import com.englishlearn.entity.ConversationMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ollama Provider：调用 qwen2.5:7b-instruct 完成场景对话（reply）与口语评估（evaluate）。
 * 评估强制要求模型输出 JSON。当 Ollama 不可用或返回异常时回退到 Mock 规则实现，
 * 保证演示流程不中断。
 */
public class OllamaLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaLlmProvider.class);

    private final String baseUrl;
    private final String model;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MockLlmProvider fallback = new MockLlmProvider();

    public OllamaLlmProvider(String baseUrl, String model, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.objectMapper = objectMapper;

        Timeout connectTimeout = Timeout.ofSeconds(5);
        Timeout socketTimeout = Timeout.ofSeconds(60);

        // 连接池 + keep-alive：复用 TCP 连接，弱网/多次请求下减少握手开销
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .setTimeToLive(TimeValue.ofSeconds(600))
                .build();
        PoolingHttpClientConnectionManagerBuilder managerBuilder = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(50)
                .setMaxConnPerRoute(20)
                .setDefaultConnectionConfig(connectionConfig);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                .setResponseTimeout(socketTimeout)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(managerBuilder.build())
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();

        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    @Override
    public String name() {
        return "ollama";
    }

    @Override
    public Reply opening(String sceneName, String sceneDesc, String role) {
        // 开场白沿用规则式实现，保证角色开场稳定。
        return fallback.opening(sceneName, sceneDesc, role);
    }

    @Override
    public Reply reply(String sceneName, String sceneDesc, String role, List<ConversationMessage> history, String userInput) {
        String historyText = history == null ? "" : history.stream()
                .map(m -> m.speaker + ": " + m.contentEn)
                .collect(Collectors.joining("\n"));
        String prompt = "You are role-playing in an English speaking practice scenario.\n"
                + "Scene: " + sceneName + "\n"
                + "Description: " + sceneDesc + "\n"
                + "Your role: " + role + "\n"
                + "Recent conversation:\n" + historyText + "\n"
                + "User just said: " + userInput + "\n"
                + "Reply naturally in English as " + role + ". Provide a Chinese translation.\n"
                + "Respond ONLY with JSON: {\"en\": \"...\", \"zh\": \"...\"}";
        String content = chat(prompt, true);
        if (content != null) {
            JsonNode node = parseJson(content);
            if (node != null && node.hasNonNull("en")) {
                return new Reply(node.path("en").asText(), node.path("zh").asText(""));
            }
        }
        return fallback.reply(sceneName, sceneDesc, role, history, userInput);
    }

    @Override
    public EvalResult evaluate(String userInput) {
        String prompt = "You are an English oral tutor. Evaluate the learner's sentence below.\n"
                + "Score pronunciation (pron), fluency, reaction and naturalness on a 0-100 scale.\n"
                + "Also provide grammarFeedback, a list of phonemeIssues (word/phoneme/note) and an optional betterExpression.\n"
                + "Sentence: " + userInput + "\n"
                + "Respond ONLY with JSON: {\"pron\":0,\"fluency\":0,\"reaction\":0,\"natural\":0,"
                + "\"grammarFeedback\":\"\",\"phonemeIssues\":[{\"word\":\"\",\"phoneme\":\"\",\"note\":\"\"}],\"betterExpression\":\"\"}";
        String content = chat(prompt, true);
        if (content != null) {
            JsonNode node = parseJson(content);
            if (node != null) {
                EvalResult r = new EvalResult();
                r.pron = node.path("pron").asDouble();
                r.fluency = node.path("fluency").asDouble();
                r.reaction = node.path("reaction").asDouble();
                r.natural = node.path("natural").asDouble();
                r.grammarFeedback = node.path("grammarFeedback").asText(null);
                r.betterExpression = node.path("betterExpression").asText(null);
                r.phonemeIssues = new ArrayList<>();
                JsonNode issues = node.path("phonemeIssues");
                if (issues.isArray()) {
                    for (JsonNode it : issues) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("word", it.path("word").asText(""));
                        item.put("phoneme", it.path("phoneme").asText(""));
                        item.put("note", it.path("note").asText(""));
                        r.phonemeIssues.add(item);
                    }
                }
                return r;
            }
        }
        return fallback.evaluate(userInput);
    }

    @Override
    public SummaryResult summarize(List<ConversationMessage> messages, List<EvalResult> evaluations) {
        // 小结聚合沿用规则式实现，四维均分由调用方依据评估记录覆写。
        return fallback.summarize(messages, evaluations);
    }

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_BASE_DELAY_MS = 500;

    private String chat(String prompt, boolean json) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return doChat(prompt, json);
            } catch (ResourceAccessException e) {
                // 弱网下的连接失败/读超时/连接被重置多为瞬时抖动，指数退避重试可明显提升成功率
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("Ollama 网络异常（已重试 {} 次），回退到 mock：{}", MAX_ATTEMPTS, e.getMessage());
                    return null;
                }
                sleepBeforeRetry(attempt);
            } catch (Exception e) {
                // 非网络类错误（如响应不是 JSON）重试无意义，直接回退
                log.warn("Ollama 调用失败，回退到 mock：{}", e.getMessage());
                return null;
            }
        }
        return null;
    }

    private String doChat(String prompt, boolean json) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("stream", false);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        if (json) {
            body.put("format", "json");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String resp = restTemplate.postForObject(baseUrl + "/api/chat", entity, String.class);
        JsonNode root = objectMapper.readTree(resp);
        return root.path("message").path("content").asText(null);
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(RETRY_BASE_DELAY_MS * (1L << (attempt - 1)));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private JsonNode parseJson(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (Exception e) {
            return null;
        }
    }
}