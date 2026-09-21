package com.englishlearn.pron;

import java.util.List;

/** 音素级评测结果。 */
public record PronResult(double overall,
                         double accuracy,
                         double completeness,
                         double fluency,
                         double duration,
                         double speed,
                         List<WordScore> words,
                         List<PronIssue> issues,
                         List<String> skipped,
                         String message) {

    /** 单词级评分：单词 + 分数 + 逐音素状态 */
    public record WordScore(String word, int score, List<PhonemeItem> phonemes) {
    }

    /** 单个音素的判定：读对 match / 读错 sub / 漏读 del */
    public record PhonemeItem(String ph, String status, String hint) {
    }

    /** 一条发音问题（音标级纠错） */
    public record PronIssue(String type, String expected, String got, String word, String hint) {
    }

    public static PronResult empty(String message, List<String> skipped) {
        return new PronResult(0, 0, 0, 0, 0, 0, List.of(), List.of(), skipped, message);
    }
}
