package com.englishlearn.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LLM 提供方选择：按配置 llm.provider 切换实现（策略模式入口）。
 */
@Configuration
public class LlmConfig {

    @Bean
    public LlmProvider llmProvider(@Value("${llm.provider:mock}") String provider,
                                   @Value("${llm.ollama.base-url:http://localhost:11434}") String baseUrl,
                                   @Value("${llm.ollama.model:qwen2.5:1.5b-instruct}") String model,
                                   ObjectMapper objectMapper,
                                   LlmMetrics metrics) {
        LlmProvider delegate = "ollama".equalsIgnoreCase(provider)
                ? new OllamaLlmProvider(baseUrl, model, objectMapper)
                : new MockLlmProvider();
        return new MeteredLlmProvider(delegate, metrics);
    }

    /** 后台预热模型：启动即加载进内存，用户第一次跟读评测不用等加载 */
    @Bean
    public ApplicationRunner llmWarmupRunner(LlmProvider llmProvider) {
        return args -> {
            if (!(llmProvider instanceof MeteredLlmProvider metered)) {
                return;
            }
            if (!(metered.delegate() instanceof OllamaLlmProvider ollama)) {
                return;
            }
            Thread t = new Thread(ollama::warmup, "llm-warmup");
            t.setDaemon(true);
            t.start();
        };
    }
}
