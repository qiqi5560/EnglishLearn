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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    /** 熔断：连续失败达阈值后，冷却期内直接回退 mock，避免每条消息都干等连接超时 */
    private static final int FAILURE_THRESHOLD = 2;
    private static final long COOLDOWN_MS = 600_000L;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile long unavailableUntil = 0L;

    public OllamaLlmProvider(String baseUrl, String model, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.objectMapper = objectMapper;

        Timeout connectTimeout = Timeout.ofSeconds(3);
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
                + "Reply naturally in English as " + role + " in 1-2 short sentences. Provide a Chinese translation.\n"
                + "Respond ONLY with JSON: {\"en\": \"...\", \"zh\": \"...\"}";
        String content = chat(prompt, true, 150);
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
                + "Be brief: every text field under 15 words, at most 2 phonemeIssues.\n"
                + "Sentence: " + userInput + "\n"
                + "Respond ONLY with JSON: {\"pron\":0,\"fluency\":0,\"reaction\":0,\"natural\":0,"
                + "\"grammarFeedback\":\"\",\"phonemeIssues\":[{\"word\":\"\",\"phoneme\":\"\",\"note\":\"\"}],\"betterExpression\":\"\"}";
        String content = chat(prompt, true, 300);
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
    public LevelJudgement judgeLevel(List<String> answers) {
        String joined;
        if (answers == null || answers.isEmpty()) {
            joined = "";
        } else {
            joined = IntStream.range(0, answers.size())
                    .mapToObj(i -> "Q" + (i + 1) + ": " + (answers.get(i) == null ? "" : answers.get(i).trim()))
                    .collect(Collectors.joining("\n"));
        }
        if (joined.isBlank()) {
            return new LevelJudgement("A1", 10, "No valid answers were provided.");
        }
        String prompt = "You are an English placement examiner. The learner answered three oral questions:\n"
                + joined + "\n"
                + "Judge content quality ONLY: relevance to each question, grammar, vocabulary range, completeness. "
                + "Random, memorized, meaningless or off-topic answers must score below 40.\n"
                + "Give an overall score 0-100 and the CEFR level consistent with it: "
                + "0-39=A1, 40-54=A2, 55-69=B1, 70-79=B2, 80-89=C1, 90+=C2. Comment in under 20 words.\n"
                + "Respond ONLY with JSON: {\"score\":0,\"level\":\"A1\",\"comment\":\"\"}";
        String content = chat(prompt, true, 200);
        if (content != null) {
            JsonNode node = parseJson(content);
            if (node != null) {
                int score = clampScore(node.path("score").asInt(-1));
                String level = normalizeLevel(node.path("level").asText(null), score);
                if (level != null) {
                    return new LevelJudgement(level, score, node.path("comment").asText(""));
                }
            }
        }
        return fallback.judgeLevel(answers);
    }

    private static int clampScore(int score) {
        if (score < 0) return 0;
        return Math.min(score, 100);
    }

    /** 模型给的等级与分数冲突时，以分数为准 */
    private static String normalizeLevel(String level, int score) {
        if (level == null || !level.matches("(?i)A1|A2|B1|B2|C1|C2")) {
            return scoreToLevel(score);
        }
        return level.toUpperCase();
    }

    private static String scoreToLevel(int score) {
        if (score < 40) return "A1";
        if (score < 55) return "A2";
        if (score < 70) return "B1";
        if (score < 80) return "B2";
        if (score < 90) return "C1";
        return "C2";
    }

    @Override
    public SummaryResult summarize(List<ConversationMessage> messages, List<EvalResult> evaluations) {
        // 小结聚合沿用规则式实现，四维均分由调用方依据评估记录覆写。
        return fallback.summarize(messages, evaluations);
    }

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_BASE_DELAY_MS = 500;

    private String chat(String prompt, boolean json, int numPredict) {
        // 熔断期内不再发起请求，直接回退，省去每次的连接超时等待
        if (System.currentTimeMillis() < unavailableUntil) {
            return null;
        }
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String content = doChat(prompt, json, numPredict);
                if (content != null) {
                    consecutiveFailures.set(0);
                    unavailableUntil = 0L;
                }
                return content;
            } catch (ResourceAccessException e) {
                // 只有读超时才值得重试；服务器不可达时重试只会成倍拉长等待
                if (isReadTimeout(e) && attempt < MAX_ATTEMPTS) {
                    sleepBeforeRetry(attempt);
                    continue;
                }
                onUnavailable("网络异常", e.getMessage());
                return null;
            } catch (Exception e) {
                // 非网络类错误（如响应不是 JSON）重试无意义，直接回退
                log.warn("Ollama 调用失败，回退到 mock：{}", e.getMessage());
                return null;
            }
        }
        return null;
    }

    /** 区分读超时与连接失败：只有前者值得重试 */
    private static boolean isReadTimeout(ResourceAccessException e) {
        Throwable t = e.getCause();
        while (t != null) {
            if (t instanceof java.net.SocketTimeoutException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /** 记录失败，达到阈值后打开熔断 */
    private void onUnavailable(String reason, String detail) {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= FAILURE_THRESHOLD) {
            unavailableUntil = System.currentTimeMillis() + COOLDOWN_MS;
            log.warn("Ollama {}（连续 {} 次），{} 分钟内直接回退 mock：{}",
                    reason, failures, COOLDOWN_MS / 60000, detail);
        } else {
            log.warn("Ollama {}，回退到 mock：{}", reason, detail);
        }
    }

    private String doChat(String prompt, boolean json, int numPredict) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("stream", false);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        if (json) {
            body.put("format", "json");
        }
        // 限制最大生成长度：模型啰嗦时是响应慢的主因
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("num_predict", numPredict);
        options.put("temperature", 0.7);
        body.put("options", options);
        // 模型常驻内存，避免每句话都重新加载 4.7GB 权重
        body.put("keep_alive", "30m");
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