package com.englishlearn.service;

import com.englishlearn.common.ApiException;
import com.englishlearn.common.JsonUtil;
import com.englishlearn.dto.QuoteDtos;
import com.englishlearn.dto.QuoteDtos.DocBriefDto;
import com.englishlearn.dto.QuoteDtos.DocDto;
import com.englishlearn.dto.QuoteDtos.ParagraphDto;
import com.englishlearn.dto.QuoteDtos.QuoteDto;
import com.englishlearn.dto.QuoteDtos.ReadEvalDto;
import com.englishlearn.entity.AssessmentRecord;
import com.englishlearn.entity.QuoteMaterial;
import com.englishlearn.entity.ReadingDoc;
import com.englishlearn.entity.StudyRecord;
import com.englishlearn.entity.User;
import com.englishlearn.llm.EvalResult;
import com.englishlearn.llm.LlmProvider;
import com.englishlearn.pron.PronResult;
import com.englishlearn.pron.PronunciationService;
import com.englishlearn.repository.AssessmentRecordRepository;
import com.englishlearn.repository.QuoteMaterialRepository;
import com.englishlearn.repository.ReadingDocRepository;
import com.englishlearn.repository.StudyRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 名句跟读服务：名句素材库（内置 15 条 + 用户自建）、阅读素材导入与分段、
 * 一键翻译（单段 / 整体）、AI 跟读评测。
 */
@Service
public class QuoteService {

    private final QuoteMaterialRepository quoteRepository;
    private final ReadingDocRepository docRepository;
    private final LlmProvider llmProvider;
    private final StudyRecordRepository studyRecordRepository;
    private final AssessmentRecordRepository assessmentRepository;
    private final PronunciationService pronunciationService;

    public QuoteService(QuoteMaterialRepository quoteRepository,
                        ReadingDocRepository docRepository,
                        LlmProvider llmProvider,
                        StudyRecordRepository studyRecordRepository,
                        AssessmentRecordRepository assessmentRepository,
                        PronunciationService pronunciationService) {
        this.quoteRepository = quoteRepository;
        this.docRepository = docRepository;
        this.llmProvider = llmProvider;
        this.studyRecordRepository = studyRecordRepository;
        this.assessmentRepository = assessmentRepository;
        this.pronunciationService = pronunciationService;
    }

    // ------------------------------------------------------------
    // 名句素材库
    // ------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<QuoteDto> listQuotes(User user, String category, String level, String keyword) {
        List<QuoteMaterial> all = quoteRepository.search(
                user.userId, blankOrNull(category), blankOrNull(level), blankOrNull(keyword));
        List<QuoteDto> out = new ArrayList<>(all.size());
        for (QuoteMaterial q : all) {
            out.add(QuoteDtos.quoteToDict(q));
        }
        return out;
    }

    @Transactional
    public QuoteDto createQuote(User user, CreateQuoteReq req) {
        if (req.title() == null || req.title().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "标题不能为空");
        }
        if (req.textEn() == null || req.textEn().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "内容不能为空");
        }
        QuoteMaterial q = new QuoteMaterial();
        q.title = req.title().trim();
        q.source = req.source();
        q.category = req.category() == null ? "英语美句" : req.category();
        q.level = req.level() == null ? "B1" : req.level();
        q.textEn = req.textEn().trim();
        q.textZh = req.textZh();
        q.builtin = 0;
        q.ownerId = user.userId;
        q = quoteRepository.save(q);
        return QuoteDtos.quoteToDict(q);
    }

    @Transactional
    public void deleteQuote(User user, Integer quoteId) {
        QuoteMaterial q = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "名句不存在"));
        if (q.builtin != null && q.builtin == 1) {
            throw new ApiException(HttpStatus.FORBIDDEN, "内置名句不可删除");
        }
        if (!user.userId.equals(q.ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只能删除自己的名句");
        }
        quoteRepository.delete(q);
    }

    // ------------------------------------------------------------
    // 阅读素材导入
    // ------------------------------------------------------------
    @Transactional
    public DocDto createDoc(User user, CreateDocReq req) {
        if (req.title() == null || req.title().isBlank() || req.text() == null || req.text().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "标题与正文不能为空");
        }
        List<ParagraphDto> paragraphs = splitParagraphs(req.text());
        if (paragraphs.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "未能从内容中解析出段落");
        }
        ReadingDoc doc = new ReadingDoc();
        doc.ownerId = user.userId;
        doc.title = req.title().trim();
        doc.sourceType = req.sourceType();
        doc.paragraphCount = paragraphs.size();
        doc.paragraphsJson = JsonUtil.toJson(paragraphs);
        doc = docRepository.save(doc);
        return QuoteDtos.docToDict(doc, paragraphs);
    }

    @Transactional(readOnly = true)
    public List<DocBriefDto> listDocs(User user) {
        List<ReadingDoc> docs = docRepository.findByOwnerIdOrderByDocIdDesc(user.userId);
        List<DocBriefDto> out = new ArrayList<>(docs.size());
        for (ReadingDoc d : docs) {
            out.add(QuoteDtos.docToBrief(d));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public DocDto getDoc(User user, Integer docId) {
        ReadingDoc doc = docRepository.findByDocIdAndOwnerId(docId, user.userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "素材不存在"));
        return QuoteDtos.docToDict(doc, parseParagraphs(doc.paragraphsJson));
    }

    @Transactional
    public void deleteDoc(User user, Integer docId) {
        ReadingDoc doc = docRepository.findByDocIdAndOwnerId(docId, user.userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "素材不存在"));
        docRepository.delete(doc);
    }

    // ------------------------------------------------------------
    // 一键翻译（单段 / 整篇）
    // ------------------------------------------------------------
    @Transactional
    public DocDto translateDoc(User user, Integer docId, Boolean save) {
        ReadingDoc doc = docRepository.findByDocIdAndOwnerId(docId, user.userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "素材不存在"));
        List<ParagraphDto> paragraphs = parseParagraphs(doc.paragraphsJson);
        translateParagraphs(paragraphs);
        if (save != null && save) {
            doc.paragraphsJson = JsonUtil.toJson(paragraphs);
            docRepository.save(doc);
        }
        return QuoteDtos.docToDict(doc, paragraphs);
    }

    /** 翻译单段文本，返回译文（不落库，供逐段翻译与首页台词角调用） */
    public String translateParagraph(User user, String textEn) {
        if (textEn == null || textEn.isBlank()) {
            return null;
        }
        List<String> result = llmProvider.translate(List.of(textEn));
        return result.isEmpty() ? null : result.get(0);
    }

    // ------------------------------------------------------------
    // AI 跟读评测
    // ------------------------------------------------------------
    @Transactional
    public ReadEvalDto evaluateReading(User user, String target, String spoken) {
        if (target == null || target.isBlank() || spoken == null || spoken.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "缺少参照文本或朗读内容");
        }
        EvalResult eval = llmProvider.evaluateReading(target, spoken);
        double total = Math.round((eval.pron * 0.35 + eval.fluency * 0.25 + eval.natural * 0.25 + eval.reaction * 0.15) * 10.0) / 10.0;
        int accuracy = (int) Math.round(eval.reaction);
        List<String> missing = missingWords(target, spoken);
        // 更宽松一点的语气提示，避免消极反馈
        String feedback = eval.grammarFeedback;
        if (feedback == null || feedback.isBlank()) {
            feedback = accuracy >= 85 ? "跟读得很完整，节奏平稳，继续保持！" : "试着放慢语速，一句一句读清楚，再连起来。";
        }
        List<String> tips = new ArrayList<>();
        tips.add(accuracy >= 85 ? "已经很棒了，试着带上原句的情感重音再读一遍。" : "重点练习没读到的这几个词，然后再整句跟读。");
        if (eval.pron < 80) {
            tips.add("注意每个单词的重音，慢下来把元音读饱满。");
        }
        if (updated(eval.pron, eval.fluency, eval.natural, eval.reaction)) {
            tips.add("保持这个状态每天来一段，语感会越来越顺。");
        }

        // 落库进学习报表：study_record 驱动统计卡与成长曲线，assessment_record 驱动能力雷达图
        LocalDateTime now = LocalDateTime.now();
        AssessmentRecord assessment = new AssessmentRecord();
        assessment.userId = user.userId;
        assessment.pronScore = eval.pron;
        assessment.fluencyScore = eval.fluency;
        assessment.reactionScore = eval.reaction;
        assessment.naturalScore = eval.natural;
        assessment.grammarFeedback = feedback;
        assessment.assessTime = now;
        assessmentRepository.save(assessment);

        StudyRecord studyRecord = new StudyRecord();
        studyRecord.userId = user.userId;
        studyRecord.actionType = "reading";
        studyRecord.durationMin = 1;
        studyRecord.score = total;
        studyRecord.learnDate = now.toLocalDate();
        studyRecordRepository.save(studyRecord);

        return new ReadEvalDto(total, eval.pron, eval.fluency, eval.natural, eval.reaction,
                accuracy, missing, feedback, tips, null);
    }

    /**
     * 音素级跟读评测：用户上传录音时走本地 ONNX 音素模型，
     * 逐音素判定读对 / 读错 / 漏读，给出音标级纠错。
     */
    @Transactional
    public ReadEvalDto evaluateReadingAudio(User user, String target, byte[] audio) {
        if (target == null || target.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "缺少参照文本");
        }
        if (audio == null || audio.length == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "没有收到录音");
        }
        PronResult result = pronunciationService.assess(audio, target);
        if (!result.message().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, result.message());
        }

        double pron = result.accuracy();
        double fluency = result.fluency();
        double completion = result.completeness();
        double natural = result.overall();
        double total = Math.round((pron * 0.35 + fluency * 0.25 + natural * 0.25 + completion * 0.15) * 10.0) / 10.0;

        List<String> missing = weakWords(result);
        String feedback = phonemeFeedback(result);
        List<String> tips = phonemeTips(result);

        saveReadingRecord(user, pron, fluency, completion, natural, total, feedback);

        return new ReadEvalDto(total, pron, fluency, natural, completion,
                (int) Math.round(completion), missing, feedback, tips, toPhonemeDto(result));
    }

    /** 音素评分落库，与文本评分共用同一套学习报表数据 */
    private void saveReadingRecord(User user, double pron, double fluency, double reaction,
                                   double natural, double total, String feedback) {
        LocalDateTime now = LocalDateTime.now();
        AssessmentRecord assessment = new AssessmentRecord();
        assessment.userId = user.userId;
        assessment.pronScore = pron;
        assessment.fluencyScore = fluency;
        assessment.reactionScore = reaction;
        assessment.naturalScore = natural;
        assessment.grammarFeedback = feedback;
        assessment.assessTime = now;
        assessmentRepository.save(assessment);

        StudyRecord studyRecord = new StudyRecord();
        studyRecord.userId = user.userId;
        studyRecord.actionType = "reading";
        studyRecord.durationMin = 1;
        studyRecord.score = total;
        studyRecord.learnDate = now.toLocalDate();
        studyRecordRepository.save(studyRecord);
    }

    private static List<String> weakWords(PronResult result) {
        List<String> weak = new ArrayList<>();
        for (PronResult.WordScore w : result.words()) {
            if (w.score() < 60) {
                weak.add(w.word());
            }
        }
        return weak.size() > 5 ? weak.subList(0, 5) : weak;
    }

    /** 用音素错误拼一句中文反馈，直接告诉用户哪个音没读对 */
    private static String phonemeFeedback(PronResult result) {
        if (result.issues().isEmpty()) {
            return "音素级检测通过，每个音都读得很到位！";
        }
        StringBuilder sb = new StringBuilder("有 " + result.issues().size() + " 处发音可以更准：");
        int shown = 0;
        for (PronResult.PronIssue issue : result.issues()) {
            if (shown >= 3) {
                break;
            }
            String tail = issue.hint().isEmpty() ? "" : "（" + issue.hint() + "）";
            if ("del".equals(issue.type())) {
                sb.append(' ').append(issue.word()).append(" 的 /").append(issue.expected()).append("/ 没读出来").append(tail);
            } else if ("sub".equals(issue.type())) {
                sb.append(' ').append(issue.word()).append(" 的 /").append(issue.expected())
                        .append("/ 读成了 /").append(issue.got()).append('/').append(tail);
            } else {
                sb.append(" 多读了一个 /").append(issue.got()).append('/');
            }
            sb.append('；');
            shown++;
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private static List<String> phonemeTips(PronResult result) {
        List<String> tips = new ArrayList<>();
        if (result.accuracy() >= 85) {
            tips.add("音素准确度很高，试着带上原句的情感重音再读一遍。");
        } else {
            tips.add("重点练习标红的词，跟着音标把每个音读饱满，再整句跟读。");
        }
        if (result.speed() > 16) {
            tips.add("语速偏快（" + result.speed() + " 音素/秒），慢一点让每个音发完整。");
        } else if (result.speed() < 8) {
            tips.add("语速偏慢，可以把词连起来读，会更接近母语节奏。");
        }
        if (result.completeness() < 80) {
            tips.add("有音素没读出来，先保证每个词读完整，再追求流畅。");
        }
        if (!result.skipped().isEmpty()) {
            tips.add("未参与音素评测的词：" + String.join("、", result.skipped().subList(0, Math.min(3, result.skipped().size()))));
        }
        return tips;
    }

    private static QuoteDtos.PhonemeEvalDto toPhonemeDto(PronResult result) {
        List<QuoteDtos.WordPronDto> words = new ArrayList<>();
        for (PronResult.WordScore w : result.words()) {
            List<QuoteDtos.PronPhonemeDto> items = new ArrayList<>();
            for (PronResult.PhonemeItem item : w.phonemes()) {
                items.add(new QuoteDtos.PronPhonemeDto(item.ph(), item.status(), item.hint()));
            }
            words.add(new QuoteDtos.WordPronDto(w.word(), w.score(), items));
        }
        List<QuoteDtos.PronIssueDto> issues = new ArrayList<>();
        for (PronResult.PronIssue issue : result.issues()) {
            issues.add(new QuoteDtos.PronIssueDto(issue.type(), issue.expected(), issue.got(), issue.word(), issue.hint()));
        }
        return new QuoteDtos.PhonemeEvalDto(result.accuracy(), result.completeness(), result.fluency(),
                result.speed(), result.duration(), words, issues, result.skipped(), result.message());
    }

    private boolean updated(double a, double b, double c, double d) {
        return Math.min(a, Math.min(b, Math.min(c, d))) >= 82;
    }

    // ------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------
    private void translateParagraphs(List<ParagraphDto> paragraphs) {
        List<String> enList = new ArrayList<>(paragraphs.size());
        for (ParagraphDto p : paragraphs) {
            enList.add(p.en());
        }
        List<String> zhList = llmProvider.translate(enList);
        for (int i = 0; i < paragraphs.size(); i++) {
            ParagraphDto p = paragraphs.get(i);
            String zh = zhList != null && i < zhList.size() ? zhList.get(i) : null;
            paragraphs.set(i, new ParagraphDto(p.en(), zh));
        }
    }

    /** 按空行切段；无空行时按句子切分，避免整篇挤成一段 */
    static List<ParagraphDto> splitParagraphs(String text) {
        String cleaned = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        List<String> chunks = new ArrayList<>();
        for (String block : cleaned.split("\\n\\s*\\n")) {
            String b = block.trim();
            if (!b.isEmpty()) {
                chunks.add(b);
            }
        }
        if (chunks.size() <= 1) {
            chunks.clear();
            for (String sentence : cleaned.split("(?<=[.?!])\\s+")) {
                String s = sentence.trim();
                if (!s.isEmpty()) {
                    chunks.add(s);
                }
            }
        }
        List<ParagraphDto> out = new ArrayList<>(chunks.size());
        for (String c : chunks) {
            out.add(new ParagraphDto(c, null));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static List<ParagraphDto> parseParagraphs(String json) {
        List<ParagraphDto> out = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return out;
        }
        try {
            List<Object> list = JsonUtil.parseList(json);
            if (list == null) {
                return out;
            }
            for (Object item : list) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<String, Object> m = (Map<String, Object>) item;
                String en = m.get("en") == null ? "" : String.valueOf(m.get("en"));
                Object zhObj = m.get("zh");
                String zh = zhObj == null ? null : String.valueOf(zhObj);
                out.add(new ParagraphDto(en, zh == null || zh.isBlank() ? null : zh));
            }
        } catch (Exception ignored) {
            // 解析失败时返回已解析部分，避免整篇素材不可用
        }
        return out;
    }

    private static List<String> missingWords(String target, String spoken) {
        List<String> targetWords = words(target);
        List<String> spokenWords = words(spoken);
        List<String> missing = new ArrayList<>();
        for (String w : targetWords) {
            if (w.length() <= 3) {
                continue; // 忽略 the/and 等极小词
            }
            boolean found = false;
            for (String s : spokenWords) {
                if (s.equalsIgnoreCase(w)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missing.add(w);
            }
        }
        return missing.size() > 5 ? missing.subList(0, 5) : missing;
    }

    private static List<String> words(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        for (String w : text.split("[^A-Za-z']+")) {
            if (!w.isEmpty()) {
                out.add(w);
            }
        }
        return out;
    }

    private static String blankOrNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // ------------------------------------------------------------
    // 文件文本抽取：txt / md 直接按 UTF-8 解码，docx 解压读取 word/document.xml
    // （不引入额外依赖，用 JDK 自带 java.util.zip 完成）
    // ------------------------------------------------------------
    public static String extractText(String filename, byte[] bytes) throws java.io.IOException {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".docx")) {
            return extractDocx(bytes);
        }
        if (lower.endsWith(".doc")) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "暂不支持旧版 .doc 格式，请在 Word 中另存为 .docx 或 .txt 后再导入");
        }
        String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        // 去掉 UTF-8 BOM
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        return text;
    }

    /** docx 本质是 zip：取 word/document.xml，按 <w:p> 段落还原文本 */
    private static String extractDocx(byte[] bytes) throws java.io.IOException {
        StringBuilder sb = new StringBuilder();
        try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!"word/document.xml".equals(entry.getName())) {
                    continue;
                }
                String xml = new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                for (String para : xml.split("</w:p>")) {
                    String body = para.replaceAll("(?s)<w:tab[^>]*/>", "\t")
                            .replaceAll("<[^>]+>", "")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&quot;", "\"")
                            .replace("&apos;", "'")
                            .trim();
                    if (!body.isEmpty()) {
                        sb.append(body).append("\n\n");
                    }
                }
                break;
            }
        }
        return sb.toString().trim();
    }

    public record CreateQuoteReq(String title, String source, String category, String level, String textEn, String textZh) {
    }

    public record CreateDocReq(String title, String text, String sourceType) {
    }
}