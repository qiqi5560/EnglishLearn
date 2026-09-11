package com.englishlearn.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 一次对话消息的实时评测结果（F003 四维）。
 */
public class EvalResult {
    public double pron;
    public double fluency;
    public double reaction;
    public double natural;
    public String grammarFeedback;
    public List<Map<String, Object>> phonemeIssues = new ArrayList<>();
    public String betterExpression;
}