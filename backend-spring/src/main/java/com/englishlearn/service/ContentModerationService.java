package com.englishlearn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 内容安全审核（规则层）：在用户消息入库前识别不当用语，命中则直接撤回。
 *
 * 设计要点：
 * 1. 零依赖、零网络调用，纯本地规则，不影响现有对话响应速度；
 * 2. 词库外置在 resources/moderation-words.txt，改词重启即生效；
 * 3. 归一化后比对，可拦住 f*ck / f u c k / sh1t / phuck / fukkkk 这类变形；
 * 4. 单词走「全等匹配」，避免 class / pass / bass / as 被误判。
 */
@Service
public class ContentModerationService {

    private static final Logger log = LoggerFactory.getLogger(ContentModerationService.class);

    /** 词库缺失时的兜底词，保证核心脏话一定拦得住 */
    private static final List<String> FALLBACK_WORDS = List.of("fuck", "shit", "bitch", "asshole", "cunt");

    /** Leetspeak 还原：数字/符号 -> 字母 */
    private static final Map<Character, Character> LEET = Map.ofEntries(
            Map.entry('0', 'o'), Map.entry('1', 'i'), Map.entry('2', 'z'), Map.entry('3', 'e'),
            Map.entry('4', 'a'), Map.entry('5', 's'), Map.entry('6', 'g'), Map.entry('7', 't'),
            Map.entry('8', 'b'), Map.entry('9', 'g'), Map.entry('@', 'a'), Map.entry('$', 's'),
            Map.entry('!', 'i'), Map.entry('|', 'i'), Map.entry('+', 't'));

    /** 拦截后 AI 的礼貌提醒（本地模板，按角色口吻随机一句） */
    private static final List<String[]> NOTICE_TEMPLATES = List.of(
            new String[]{"Sorry, I can't go along with that. Let's keep our conversation polite, shall we?",
                    "抱歉，这句话我不便回应。我们保持礼貌，继续练习好吗？"},
            new String[]{"Let's keep it respectful, please. Now, back to our conversation — go ahead.",
                    "请保持用语得体。我们回到刚才的对话，继续吧。"},
            new String[]{"Hmm, that word isn't appropriate here. Try saying it in a politer way.",
                    "这个词不太合适，换一种更礼貌的说法再试一次吧。"});

    /** 归一化后的单词黑名单（全等匹配） */
    private final Set<String> blockWords = new LinkedHashSet<>();
    /** 归一化后的短语黑名单（包含匹配，供多词短语与中文使用） */
    private final Set<String> blockPhrases = new LinkedHashSet<>();

    private final boolean enabled;
    private final boolean showAiNotice;

    public ContentModerationService(@Value("${app.moderation.enabled:true}") boolean enabled,
                                    @Value("${app.moderation.show-ai-notice:true}") boolean showAiNotice) {
        this.enabled = enabled;
        this.showAiNotice = showAiNotice;
        loadDictionary();
    }

    /** 审核结果：blocked=true 时上层应阻止消息入库 */
    public record Decision(boolean blocked, String reason, String tip, String noticeEn, String noticeZh) {

        public static Decision pass() {
            return new Decision(false, null, null, null, null);
        }
    }

    /**
     * 审核一段用户输入。命中的词只用于日志，不回传前端，避免被当作「绕过词典」。
     */
    public Decision inspect(String text) {
        if (!enabled || text == null || text.isBlank()) {
            return Decision.pass();
        }
        String hit = findHit(text);
        if (hit == null) {
            return Decision.pass();
        }
        log.info("内容审核命中，消息已撤回（命中词条长度 {}）", hit.length());
        String[] notice = showAiNotice ? randomNotice() : null;
        return new Decision(
                true,
                "消息含不当用语，已被撤回",
                "该消息含不当用语，已被撤回",
                notice == null ? null : notice[0],
                notice == null ? null : notice[1]);
    }

    /**
     * 依次用四种形态匹配，返回命中的词条（归一化形式）：
     * 1) 常规分词（标点/空格都是分隔符）；
     * 2) 只按空白分词，词内去掉 * . - 等混淆符号（拦 F*ck / f-u-c-k）；
     * 3) 整句去掉所有分隔符后全等比对（拦 f u c k 这类拆字母写法）；
     * 4) 短语包含匹配（中文与多词短语，如 son of a bitch）。
     * 全部走「全等」而非 substring，避免 class / document / peacock 被误伤。
     */
    private String findHit(String text) {
        for (String token : text.split("[^\\p{L}\\p{N}]+")) {
            String key = normalize(token);
            if (!key.isEmpty() && blockWords.contains(key)) {
                return key;
            }
        }
        for (String token : text.split("\\s+")) {
            String key = normalize(token);
            if (!key.isEmpty() && blockWords.contains(key)) {
                return key;
            }
        }
        String compact = normalize(text);
        if (!compact.isEmpty() && blockWords.contains(compact)) {
            return compact;
        }
        for (String phrase : blockPhrases) {
            if (!phrase.isEmpty() && compact.contains(phrase)) {
                return phrase;
            }
        }
        return null;
    }

    private static String[] randomNotice() {
        int idx = ThreadLocalRandom.current().nextInt(NOTICE_TEMPLATES.size());
        return NOTICE_TEMPLATES.get(idx);
    }

    /**
     * 归一化：小写 -> Leetspeak 还原 -> 去掉非字母字符（保留中文等 Unicode 字母）
     * -> ph 归 f、ck 归 k -> 折叠 3 个以上连续重复字母。
     * 词库与用户输入走同一个函数，保证两边形态一致。
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = Character.toLowerCase(raw.charAt(i));
            if (c >= 'a' && c <= 'z') {
                sb.append(c);
                continue;
            }
            Character mapped = LEET.get(c);
            if (mapped != null) {
                sb.append(mapped.charValue());
                continue;
            }
            // 保留中文等其它语言的字母，丢弃标点、空格、星号等分隔符
            if (Character.isLetter(c)) {
                sb.append(c);
            }
        }
        // ph->f、ck->k 反复替换到稳定，保证 fucck / fukck 这类写法与 fuck 收敛到同一形态
        String s = sb.toString();
        String prev;
        do {
            prev = s;
            s = s.replace("ph", "f").replace("ck", "k");
        } while (!s.equals(prev));
        // 只折叠 3 个以上的重复：保留 ass / class / pass 原形，仍能拦住 fukkkk
        return s.replaceAll("([a-z])\\1{2,}", "$1");
    }

    /** 加载词库：分 [words] 与 [phrases] 两段 */
    private void loadDictionary() {
        try (InputStream in = new ClassPathResource("moderation-words.txt").getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String section = "words";
            String line;
            while ((line = reader.readLine()) != null) {
                String raw = line.trim();
                if (raw.isEmpty() || raw.startsWith("#")) {
                    continue;
                }
                String lower = raw.toLowerCase(Locale.ROOT);
                if ("[words]".equals(lower)) {
                    section = "words";
                    continue;
                }
                if ("[phrases]".equals(lower)) {
                    section = "phrases";
                    continue;
                }
                String key = normalize(raw);
                if (key.isEmpty()) {
                    continue;
                }
                if ("phrases".equals(section)) {
                    blockPhrases.add(key);
                } else {
                    blockWords.add(key);
                }
            }
            log.info("内容审核词库已加载：单词 {} 条，短语 {} 条", blockWords.size(), blockPhrases.size());
        } catch (IOException e) {
            log.warn("违规词库加载失败，改用内置兜底词：{}", e.getMessage());
            for (String w : FALLBACK_WORDS) {
                blockWords.add(normalize(w));
            }
        }
    }
}
