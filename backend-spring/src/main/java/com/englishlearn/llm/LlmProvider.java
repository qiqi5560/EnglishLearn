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
}