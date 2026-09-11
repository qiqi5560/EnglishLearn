package com.englishlearn.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 会话结束后的综合小结。
 */
public class SummaryResult {
    public double total;
    public double pron;
    public double fluency;
    public double reaction;
    public double natural;
    public List<String> highlights = new ArrayList<>();
    public List<String> improvements = new ArrayList<>();
    public List<String> suggestions = new ArrayList<>();
    public List<Map<String, Object>> corrections = new ArrayList<>();
    public String feedbackText = "";
}