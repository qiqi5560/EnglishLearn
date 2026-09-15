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

    /** 入学测评：综合所有作答内容判定 CEFR 等级（按内容质量打分，而非词数） */
    LevelJudgement judgeLevel(List<String> answers);

    SummaryResult summarize(List<ConversationMessage> messages, List<EvalResult> evaluations);
}