package com.englishlearn.llm;

import java.util.ArrayList;
import java.util.List;

/**
 * 跟读评分的本地确定性实现：按词对齐（最长公共子序列）算覆盖度，
 * 再推导发音 / 流利度 / 自然度 / 完整度。
 *
 * 之所以不交给小模型打分：1.5B 级别的模型数值评分极不稳定，
 * 同一段完美朗读会给出 97 分和 10 分两个结果，甚至整段返回 0 分。
 * 本地打分零网络开销（毫秒级）、结果可复现，评分曲线才可信。
 */
public final class ReadingScorer {

    private ReadingScorer() {
    }

    public static EvalResult score(String target, String spoken) {
        List<String> targetWords = words(target);
        List<String> spokenWords = words(spoken);

        int matched = lcs(targetWords, spokenWords);
        double coverage = targetWords.isEmpty() ? 0.0 : (double) matched / targetWords.size();
        // 多读 / 乱读的词占比，作为扣分项
        double extraRatio = targetWords.isEmpty() ? 0.0
                : (double) Math.max(0, spokenWords.size() - matched) / targetWords.size();

        double base = 55 + 40 * coverage;
        double penalty = Math.min(15, extraRatio * 25);

        EvalResult r = new EvalResult();
        r.pron = round1(clamp(base - penalty * 0.6, 35, 98));
        r.fluency = round1(clamp(base - penalty * 0.9, 35, 98));
        r.natural = round1(clamp(base - penalty * 0.5, 35, 98));
        r.reaction = round1(clamp(coverage * 100, 0, 100));
        r.grammarFeedback = feedback(coverage, matched, targetWords.size());
        r.phonemeIssues = new ArrayList<>();
        return r;
    }

    /** 按实际读对的词数给出中文点评，比小模型生成的套话更具体 */
    public static String feedback(double coverage, int matched, int total) {
        String hit = "（读对 " + matched + "/" + total + " 词）";
        if (coverage < 0.5) {
            return "只读到了原句的一部分" + hit + "，建议先逐句慢读，把每个单词念清楚。";
        }
        if (coverage < 0.8) {
            return "大部分读到位了" + hit + "，试着把长句分段，稳住节奏再连起来。";
        }
        if (coverage < 0.95) {
            return "读得比较完整" + hit + "，个别词还没咬准，再慢速精读一遍会更稳。";
        }
        return "读得很完整" + hit + "，语调自然。继续保持，注意重音与情感。";
    }

    /** 取小写单词序列，去掉标点 */
    private static List<String> words(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        for (String w : text.toLowerCase().split("[^a-z0-9']+")) {
            if (!w.isEmpty()) {
                out.add(w);
            }
        }
        return out;
    }

    /** 最长公共子序列长度：即按顺序读对的词数 */
    private static int lcs(List<String> a, List<String> b) {
        int n = a.size();
        int m = b.size();
        if (n == 0 || m == 0) {
            return 0;
        }
        int[] prev = new int[m + 1];
        int[] cur = new int[m + 1];
        for (int i = 1; i <= n; i++) {
            String aw = a.get(i - 1);
            for (int j = 1; j <= m; j++) {
                if (aw.equals(b.get(j - 1))) {
                    cur[j] = prev[j - 1] + 1;
                } else {
                    cur[j] = Math.max(prev[j], cur[j - 1]);
                }
            }
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return prev[m];
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.min(hi, Math.max(lo, v));
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
