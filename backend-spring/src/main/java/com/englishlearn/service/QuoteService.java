package com.englishlearn.service;

import com.englishlearn.common.ApiException;
import com.englishlearn.common.JsonUtil;
import com.englishlearn.dto.QuoteDtos;
import com.englishlearn.dto.QuoteDtos.DocBriefDto;
import com.englishlearn.dto.QuoteDtos.DocDto;
import com.englishlearn.dto.QuoteDtos.ParagraphDto;
import com.englishlearn.dto.QuoteDtos.QuoteDto;
import com.englishlearn.dto.QuoteDtos.ReadEvalDto;
import com.englishlearn.entity.QuoteMaterial;
import com.englishlearn.entity.ReadingDoc;
import com.englishlearn.entity.User;
import com.englishlearn.llm.EvalResult;
import com.englishlearn.llm.LlmProvider;
import com.englishlearn.repository.QuoteMaterialRepository;
import com.englishlearn.repository.ReadingDocRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public QuoteService(QuoteMaterialRepository quoteRepository,
                        ReadingDocRepository docRepository,
                        LlmProvider llmProvider) {
        this.quoteRepository = quoteRepository;
        this.docRepository = docRepository;
        this.llmProvider = llmProvider;
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
        return new ReadEvalDto(total, eval.pron, eval.fluency, eval.natural, eval.reaction,
                accuracy, missing, feedback, tips);
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