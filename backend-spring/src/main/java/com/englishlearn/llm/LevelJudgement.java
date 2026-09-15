package com.englishlearn.llm;

/**
 * 入学测评等级判定结果：score 为 0-100 综合分，level 为 CEFR 等级。
 */
public record LevelJudgement(String level, int score, String comment) {
}
