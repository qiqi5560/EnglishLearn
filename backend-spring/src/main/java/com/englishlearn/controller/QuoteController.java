package com.englishlearn.controller;

import com.englishlearn.common.ApiException;
import com.englishlearn.common.ApiResponse;
import com.englishlearn.dto.QuoteDtos;
import com.englishlearn.dto.QuoteDtos.DocBriefDto;
import com.englishlearn.dto.QuoteDtos.DocDto;
import com.englishlearn.entity.User;
import com.englishlearn.security.AuthFacade;
import com.englishlearn.service.QuoteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 名句跟读接口（/quotes）：名句素材库、阅读素材导入与翻译、AI 跟读评测。
 */
@RestController
@RequestMapping("/quotes")
public class QuoteController {

    private final QuoteService quoteService;
    private final AuthFacade authFacade;

    public QuoteController(QuoteService quoteService, AuthFacade authFacade) {
        this.quoteService = quoteService;
        this.authFacade = authFacade;
    }

    // ---------------- 名句素材库 ----------------
    @GetMapping
    public ApiResponse listQuotes(@RequestParam(required = false) String category,
                                  @RequestParam(required = false) String level,
                                  @RequestParam(required = false) String keyword) {
        User user = authFacade.requireUser();
        return ApiResponse.ok(quoteService.listQuotes(user, category, level, keyword));
    }

    @PostMapping
    public ApiResponse createQuote(@RequestBody QuoteService.CreateQuoteReq req) {
        User user = authFacade.requireUser();
        return ApiResponse.ok(quoteService.createQuote(user, req), "已加入你的名句库");
    }

    @DeleteMapping("/{quoteId}")
    public ApiResponse deleteQuote(@PathVariable Integer quoteId) {
        User user = authFacade.requireUser();
        quoteService.deleteQuote(user, quoteId);
        return ApiResponse.ok(null, "已删除");
    }

    /** 单段翻译（不落库）：body { text }，名句跟读与首页台词角通用 */
    @PostMapping("/translate")
    public ApiResponse translate(@RequestBody Map<String, String> body) {
        User user = authFacade.requireUser();
        String text = body.getOrDefault("text", "");
        String zh = quoteService.translateParagraph(user, text);
        return ApiResponse.ok(Map.of("textEn", text, "textZh", zh == null ? "" : zh));
    }

    // ---------------- 阅读素材导入 ----------------
    @PostMapping("/docs")
    public ApiResponse createDoc(@RequestBody QuoteService.CreateDocReq req) {
        User user = authFacade.requireUser();
        return ApiResponse.ok(quoteService.createDoc(user, req), "已导入并自动分段");
    }

    @GetMapping("/docs")
    public ApiResponse listDocs() {
        User user = authFacade.requireUser();
        List<DocBriefDto> docs = quoteService.listDocs(user);
        return ApiResponse.ok(docs);
    }

    @GetMapping("/docs/{docId}")
    public ApiResponse getDoc(@PathVariable Integer docId) {
        User user = authFacade.requireUser();
        return ApiResponse.ok(quoteService.getDoc(user, docId));
    }

    @DeleteMapping("/docs/{docId}")
    public ApiResponse deleteDoc(@PathVariable Integer docId) {
        User user = authFacade.requireUser();
        quoteService.deleteDoc(user, docId);
        return ApiResponse.ok(null, "已删除");
    }

    @PostMapping("/docs/{docId}/translate")
    public ApiResponse translateDoc(@PathVariable Integer docId, @RequestParam(defaultValue = "false") Boolean save) {
        User user = authFacade.requireUser();
        DocDto doc = quoteService.translateDoc(user, docId, save);
        return ApiResponse.ok(doc);
    }

    /** 上传文件导入（支持 .txt / .md / .docx），服务端解析后自动分段 */
    @PostMapping("/docs/upload")
    public ApiResponse uploadDoc(@RequestParam("file") MultipartFile file) {
        User user = authFacade.requireUser();
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请选择要导入的文件");
        }
        String filename = file.getOriginalFilename() == null ? "未命名" : file.getOriginalFilename();
        String text;
        try {
            text = QuoteService.extractText(filename, file.getBytes());
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "文件读取失败，请重试");
        }
        if (text == null || text.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "未能从文件中读取到英文内容，请确认文件格式（支持 txt / md / docx）");
        }
        String title = filename.replaceAll("\\.[A-Za-z0-9]+$", "");
        String sourceType = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "txt";
        DocDto doc = quoteService.createDoc(user, new QuoteService.CreateDocReq(title, text, sourceType));
        return ApiResponse.ok(doc, "已导入并自动分段");
    }

    // ---------------- AI 跟读评测 ----------------
    @PostMapping("/evaluate")
    public ApiResponse evaluateReading(@RequestBody Map<String, String> body) {
        User user = authFacade.requireUser();
        QuoteDtos.ReadEvalDto eval = quoteService.evaluateReading(
                user, body.getOrDefault("target", ""), body.getOrDefault("spoken", ""));
        return ApiResponse.ok(eval);
    }
}