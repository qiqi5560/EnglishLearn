package com.englishlearn.llm;

import com.englishlearn.entity.ConversationMessage;

import java.util.List;

/**
 * LLM Provider 顶层接口：所有实现类（Mock / Ollama）实现该协议。
 * 场景、历史上下文均由调用方传入，路由层不感知具体实现。
 */
public interface LlmProvider {

    String name();

    Reply opening(String sceneName, String sceneDesc, String role);

    Reply reply(String sceneName, String sceneDesc, String role, List<ConversationMessage> history, String userInput);

    EvalResult evaluate(String userInput);

    SummaryResult summarize(List<ConversationMessage> messages, List<EvalResult> evaluations);

    /**
     * 批量英译中（名句跟读的「一键翻译」）。返回顺序与入参一一对应，
     * 单条失败时该位置返回 null，由调用方决定是否提示。
     */
    List<String> translate(List<String> texts);

    /**
     * 跟读评测：以 target 为参照，评估用户朗读 spoken 的表现。
     * 复用 EvalResult 字段：pron 发音 / fluency 流利度 / natural 语调 / reaction 完整度。
     */
    EvalResult evaluateReading(String target, String spoken);
}