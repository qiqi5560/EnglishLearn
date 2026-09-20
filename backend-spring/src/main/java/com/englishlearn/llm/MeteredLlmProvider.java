package com.englishlearn.llm;

import com.englishlearn.entity.ConversationMessage;

import java.util.List;

/**
 * 指标装饰器：把真实的 LLM Provider 包一层，所有调用都会写入 {@link LlmMetrics}，
 * 业务层（DialogueService / QuoteService / PlanService）无需改动即可获得算力监控数据。
 */
public class MeteredLlmProvider implements LlmProvider {

    private final LlmProvider delegate;
    private final LlmMetrics metrics;

    public MeteredLlmProvider(LlmProvider delegate, LlmMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics;
    }

    private String providerName() {
        return delegate.name();
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public Reply opening(String sceneName, String sceneDesc, String role) {
        return metrics.measure("opening", providerName(), () -> delegate.opening(sceneName, sceneDesc, role));
    }

    @Override
    public Reply reply(String sceneName, String sceneDesc, String role,
                       List<ConversationMessage> history, String userInput) {
        return metrics.measure("reply", providerName(),
                () -> delegate.reply(sceneName, sceneDesc, role, history, userInput));
    }

    @Override
    public EvalResult evaluate(String userInput) {
        return metrics.measure("evaluate", providerName(), () -> delegate.evaluate(userInput));
    }

    @Override
    public LevelJudgement judgeLevel(List<String> answers) {
        return metrics.measure("judgeLevel", providerName(), () -> delegate.judgeLevel(answers));
    }

    @Override
    public SummaryResult summarize(List<ConversationMessage> messages, List<EvalResult> evaluations) {
        return metrics.measure("summarize", providerName(), () -> delegate.summarize(messages, evaluations));
    }

    @Override
    public List<String> translate(List<String> texts) {
        return metrics.measure("translate", providerName(), () -> delegate.translate(texts));
    }

    @Override
    public EvalResult evaluateReading(String target, String spoken) {
        return metrics.measure("evaluateReading", providerName(), () -> delegate.evaluateReading(target, spoken));
    }
}
