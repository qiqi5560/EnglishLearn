package com.englishlearn.dto;

import com.englishlearn.common.TimeUtil;
import com.englishlearn.entity.QuoteMaterial;
import com.englishlearn.entity.ReadingDoc;

import java.util.List;

/**
 * 名句跟读模块 DTO：名句素材、导入文档与段落。
 */
public final class QuoteDtos {

    private QuoteDtos() {
    }

    /** 名句素材 */
    public record QuoteDto(Integer quoteId,
                           String title,
                           String source,
                           String category,
                           String level,
                           String textEn,
                           String textZh,
                           boolean builtin,
                           int wordCount,
                           String createTime) {
    }

    /** 段落：en 原文，zh 译文（未翻译为 null） */
    public record ParagraphDto(String en, String zh) {
    }

    /** 导入文档（含段落） */
    public record DocDto(Integer docId,
                         String title,
                         String sourceType,
                         Integer paragraphCount,
                         List<ParagraphDto> paragraphs,
                         String createTime) {
    }

    /** 导入文档（不含段落，用于列表） */
    public record DocBriefDto(Integer docId,
                              String title,
                              String sourceType,
                              Integer paragraphCount,
                              String createTime) {
    }

    /** 跟读评测结果 */
    public record ReadEvalDto(double total,
                              double pron,
                              double fluency,
                              double natural,
                              double completion,
                              int accuracy,
                              List<String> missingWords,
                              String feedback,
                              List<String> tips,
                              PhonemeEvalDto phoneme) {
    }

    /** 音素级评测详情：仅在收到录音、且音素引擎可用时返回 */
    public record PhonemeEvalDto(double accuracy,
                                 double completeness,
                                 double fluency,
                                 double speed,
                                 double duration,
                                 List<WordPronDto> words,
                                 List<PronIssueDto> issues,
                                 List<String> skipped,
                                 String message) {
    }

    /** 单词级发音：单词 + 分数 + 逐音素判定 */
    public record WordPronDto(String word, int score, List<PronPhonemeDto> phonemes) {
    }

    public record PronPhonemeDto(String ph, String status, String hint) {
    }

    /** 一条音标级纠错 */
    public record PronIssueDto(String type, String expected, String got, String word, String hint) {
    }

    public static QuoteDto quoteToDict(QuoteMaterial q) {
        return new QuoteDto(
                q.quoteId,
                q.title,
                q.source,
                q.category,
                q.level,
                q.textEn,
                q.textZh,
                q.builtin != null && q.builtin == 1,
                wordCount(q.textEn),
                TimeUtil.iso(q.createTime));
    }

    public static DocBriefDto docToBrief(ReadingDoc d) {
        return new DocBriefDto(d.docId, d.title, d.sourceType, d.paragraphCount, TimeUtil.iso(d.createTime));
    }

    public static DocDto docToDict(ReadingDoc d, List<ParagraphDto> paragraphs) {
        return new DocDto(d.docId, d.title, d.sourceType, d.paragraphCount, paragraphs, TimeUtil.iso(d.createTime));
    }

    /** 英文词数统计（按空白切分，忽略纯符号） */
    public static int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String w : text.split("\\s+")) {
            if (w.matches(".*[A-Za-z].*")) {
                count++;
            }
        }
        return count;
    }
}
