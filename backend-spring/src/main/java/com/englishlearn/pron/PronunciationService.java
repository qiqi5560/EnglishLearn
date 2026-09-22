package com.englishlearn.pron;

import com.englishlearn.pron.PhonemeAligner.Op;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 音素级发音评测：输入「16kHz WAV 录音 + 参考句子」，输出逐音素的判定与纠错。
 *
 * <p>流程与 Python 版 {@code AI_/pronunciation.py} 一致：
 * G2P（cmudict）产出目标音素 → ONNX 声学模型识别实际音素 →
 * 编辑距离对齐 → 聚合出单词级与整句分数。
 *
 * <p>模型约 300MB，采用惰性加载：首次跟读评测时才载入，失败只记日志，
 * 跟读自动回退为文本比对评分，不影响其它功能。
 */
@Service
public class PronunciationService {

    private static final Logger log = LoggerFactory.getLogger(PronunciationService.class);

    /** ARPAbet（cmudict 音素集）→ 声学模型音素集；模型音素表 38 个，ɔ/ʒ 做近似归并 */
    private static final Map<String, String> ARPABET_TO_MODEL = Map.ofEntries(
            Map.entry("AA", "ɑ"), Map.entry("AE", "æ"), Map.entry("AH", "ə"),
            Map.entry("AO", "ɑ"), Map.entry("AW", "aʊ"), Map.entry("AY", "aɪ"),
            Map.entry("B", "b"), Map.entry("CH", "ʧ"), Map.entry("D", "d"),
            Map.entry("DH", "ð"), Map.entry("EH", "ɛ"), Map.entry("ER", "ɝ"),
            Map.entry("EY", "eɪ"), Map.entry("F", "f"), Map.entry("G", "g"),
            Map.entry("HH", "h"), Map.entry("IH", "ɪ"), Map.entry("IY", "i"),
            Map.entry("JH", "ʤ"), Map.entry("K", "k"), Map.entry("L", "l"),
            Map.entry("M", "m"), Map.entry("N", "n"), Map.entry("NG", "ŋ"),
            Map.entry("OW", "oʊ"), Map.entry("OY", "ɔɪ"), Map.entry("P", "p"),
            Map.entry("R", "ɹ"), Map.entry("S", "s"), Map.entry("SH", "ʃ"),
            Map.entry("T", "t"), Map.entry("TH", "θ"), Map.entry("UH", "ʊ"),
            Map.entry("UW", "u"), Map.entry("V", "v"), Map.entry("W", "w"),
            Map.entry("Y", "j"), Map.entry("Z", "z"), Map.entry("ZH", "ʃ"));

    /** 对齐时两次发音的最大时间间隔（秒）：超过视为断层，后面的「匹配」按漏读处理 */
    private static final double MAX_SPEECH_GAP = 1.0;

    /** 常见音素的口语化提示：告诉用户「这个音该怎么发」 */
    private static final Map<String, String> PHONEME_HINT = Map.ofEntries(
            Map.entry("θ", "咬舌音 th（think）"), Map.entry("ð", "咬舌浊音 th（this）"),
            Map.entry("ɹ", "r 音（舌尖别碰到上颚）"), Map.entry("l", "l 音（舌尖抵上齿龈）"),
            Map.entry("v", "v 音（上齿轻咬下唇）"), Map.entry("w", "w 音（要圆唇）"),
            Map.entry("ʃ", "sh 音"), Map.entry("ʧ", "ch 音"), Map.entry("ʤ", "j 音"),
            Map.entry("ŋ", "ng 鼻音"), Map.entry("æ", "梅花音（cat）"),
            Map.entry("ə", "弱读的 schwa 音"), Map.entry("ɪ", "短 i 音（sit）"),
            Map.entry("i", "长 i 音（see）"), Map.entry("ʊ", "短 u 音（book）"),
            Map.entry("u", "长 u 音（food）"), Map.entry("ɑ", "a 音（father）"),
            Map.entry("ɛ", "短 e 音（bed）"), Map.entry("ɝ", "卷舌 er 音（bird）"),
            Map.entry("aɪ", "ai 音（my）"), Map.entry("aʊ", "au 音（now）"),
            Map.entry("eɪ", "ei 音（day）"), Map.entry("oʊ", "o 音（go）"),
            Map.entry("ɔɪ", "oi 音（boy）"), Map.entry("ɾ", "闪音（美式 water 的 t）"));

    @Value("${pron.enabled:false}")
    private boolean enabled;
    @Value("${pron.model-path:}")
    private String modelPath;
    @Value("${pron.vocab-path:}")
    private String vocabPath;
    @Value("${pron.dict-path:}")
    private String dictPath;

    private final Object lock = new Object();
    private volatile boolean initialized;
    private volatile Engine engine;

    /** 音素引擎是否可用（配置开启且模型加载成功） */
    public boolean isAvailable() {
        return ensureEngine() != null;
    }

    /**
     * 对一次跟读做音素级评测。
     *
     * @param wav       浏览器上传的 WAV 音频字节（16bit PCM）
     * @param reference 跟读的参考句子
     */
    public PronResult assess(byte[] wav, String reference) {
        Engine engine = ensureEngine();
        if (engine == null) {
            return PronResult.empty("音素评测引擎不可用", List.of());
        }
        TextPhonemes text = textToPhonemes(engine.dict, reference);
        if (text.phonemes.isEmpty()) {
            return PronResult.empty("参考句子中没有可评测的单词（不在发音词典中）", text.skipped);
        }
        float[] waveform;
        float[] raw;
        double duration;
        try {
            WavDecoder.Pcm pcm = WavDecoder.decode(wav);
            raw = pcm.samples();
            waveform = WavDecoder.normalize(raw);
            duration = pcm.duration();
        } catch (Exception e) {
            log.warn("[发音评测] 音频解析失败: {}", e.getMessage());
            return PronResult.empty("音频解析失败，请重新录一次", text.skipped);
        }

        List<PhonemeRecognizer.PhonemeHit> hits = engine.recognizer.recognize(waveform);
        // CTC 在静音/噪声段也会吐出随机音素，若不剪掉，对齐时会把它们「蹭」到没读的词上造成假满分
        hits = trimSilence(raw, hits);
        if (hits.isEmpty()) {
            return PronResult.empty("没有识别到有效语音，请靠近麦克风再读一次", text.skipped);
        }
        // 用实际说话时长算语速，录音首尾的静音不再拖低语速分
        duration = Math.max(hits.get(hits.size() - 1).end() - hits.get(0).start(), 0.5);
        List<String> hyp = new ArrayList<>(hits.size());
        for (PhonemeRecognizer.PhonemeHit hit : hits) {
            hyp.add(hit.phoneme());
        }

        List<Op> ops = PhonemeAligner.align(text.phonemes, hyp);
        int n = text.phonemes.size();
        String[] status = new String[n];
        List<PronResult.PronIssue> issues = new ArrayList<>();
        int match = 0;
        int sub = 0;
        int del = 0;
        int ins = 0;

        int lastRef = -1; // 最近一次对齐到的参考音素下标，用于给「多读」的音定位归属单词
        double lastTime = -1; // 上一个真实发音音素的结束时间，用于检测时间断层
        for (Op op : ops) {
            if ("ins".equals(op.op())) {
                PhonemeRecognizer.PhonemeHit hit = hits.get(op.hypIndex());
                if (lastTime >= 0 && hit.start() - lastTime > MAX_SPEECH_GAP) {
                    continue; // 断层后静音里多读的音不计入
                }
                ins++;
                // 多读的音落在 lastRef 与 lastRef+1 之间，归属到最近已读的单词（句首多读归第一个词）
                String word = wordAt(text, lastRef >= 0 ? lastRef : 0);
                issues.add(new PronResult.PronIssue("ins", "-", hit.phoneme(), word, ""));
                continue;
            }
            int i = op.targetIndex();
            lastRef = i;
            String expected = text.phonemes.get(i);
            String word = wordAt(text, i);
            if ("del".equals(op.op())) {
                status[i] = "del";
                del++;
                issues.add(new PronResult.PronIssue("del", expected, "-", word,
                        PHONEME_HINT.getOrDefault(expected, "")));
                continue;
            }
            PhonemeRecognizer.PhonemeHit hit = hits.get(op.hypIndex());
            if (lastTime >= 0 && hit.start() - lastTime > MAX_SPEECH_GAP) {
                // 时间断层：从这里开始其实没读，「匹配」是静音垃圾音素蹭上的，一律按漏读处理
                status[i] = "del";
                del++;
                issues.add(new PronResult.PronIssue("del", expected, "-", word,
                        PHONEME_HINT.getOrDefault(expected, "")));
                continue;
            }
            lastTime = hit.end();
            status[i] = op.op();
            if ("sub".equals(op.op())) {
                sub++;
                issues.add(new PronResult.PronIssue("sub", expected, hit.phoneme(), word,
                        PHONEME_HINT.getOrDefault(expected, "")));
            } else {
                match++;
            }
        }

        List<PronResult.WordScore> words = new ArrayList<>();
        for (TextPhonemes.Span span : text.spans) {
            List<PronResult.PhonemeItem> items = new ArrayList<>();
            int correct = 0;
            for (int k = 0; k < span.length(); k++) {
                int idx = span.start() + k;
                String st = status[idx];
                if ("match".equals(st)) {
                    correct++;
                }
                String ph = text.phonemes.get(idx);
                items.add(new PronResult.PhonemeItem(ph, st, PHONEME_HINT.getOrDefault(ph, "")));
            }
            int score = span.length() == 0 ? 0 : (int) Math.round(correct * 100.0 / span.length());
            words.add(new PronResult.WordScore(span.word(), score, items));
        }

        double accuracy = clamp(match * 100.0 / n);
        double completeness = clamp((match + sub) * 100.0 / n);
        double rate = n / Math.max(duration, 0.1);
        double fluency;
        if (rate < 8) {
            fluency = clamp(rate / 8 * 100);
        } else if (rate <= 16) {
            fluency = 100.0;
        } else {
            fluency = clamp(100 - (rate - 16) * 5, 60, 100);
        }
        double overall = clamp(0.60 * accuracy + 0.25 * completeness + 0.15 * fluency);

        return new PronResult(Math.round(overall), Math.round(accuracy), Math.round(completeness),
                Math.round(fluency), Math.round(duration * 100) / 100.0, Math.round(rate * 10) / 10.0,
                words, issues, text.skipped, "");
    }

    /** 参考文本 → 目标音素序列（模型音素集）与单词区间 */
    private TextPhonemes textToPhonemes(CmuDictionary dict, String text) {
        List<String> flat = new ArrayList<>();
        List<TextPhonemes.Span> spans = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return new TextPhonemes(flat, spans, skipped);
        }
        for (String raw : text.split("\\s+")) {
            String word = dict.clean(raw);
            if (word.isEmpty()) {
                continue;
            }
            List<String> entries = dict.lookup(word);
            if (entries == null) {
                skipped.add(word);
                continue;
            }
            List<String> phonemes = new ArrayList<>();
            for (String p : entries) {
                String key = p.replaceAll("\\d", "").toUpperCase();
                String mapped = ARPABET_TO_MODEL.get(key);
                if (mapped != null) {
                    phonemes.add(mapped);
                }
            }
            if (phonemes.isEmpty()) {
                skipped.add(word);
                continue;
            }
            spans.add(new TextPhonemes.Span(word, flat.size(), phonemes.size()));
            flat.addAll(phonemes);
        }
        return new TextPhonemes(flat, spans, skipped);
    }

    private static String wordAt(TextPhonemes text, int index) {
        for (TextPhonemes.Span span : text.spans) {
            if (index >= span.start() && index < span.start() + span.length()) {
                return span.word();
            }
        }
        return "";
    }

    /**
     * 基于能量的简易端点检测（VAD）：把静音/噪声段里识别出的垃圾音素剪掉。
     *
     * <p>CTC 模型在无声段也会输出随机音素，若不处理，全局对齐会把它们匹配到
     * 没读的词上，造成「没读也是满分」的假象。做法：25ms 帧算 RMS，
     * 以峰值能量的 12% 为语音门限，命中帧前后各保留 120ms 缓冲，
     * 中点不在语音帧内的音素直接丢弃。
     */
    private static List<PhonemeRecognizer.PhonemeHit> trimSilence(float[] raw,
            List<PhonemeRecognizer.PhonemeHit> hits) {
        if (hits.isEmpty() || raw == null || raw.length == 0) {
            return hits;
        }
        int rate = WavDecoder.TARGET_RATE;
        int frameLen = (int) (0.025 * rate);
        int hop = (int) (0.010 * rate);
        int frames = Math.max(1, (raw.length - frameLen) / hop + 1);
        double[] rms = new double[frames];
        for (int f = 0; f < frames; f++) {
            int off = f * hop;
            double sum = 0;
            for (int k = 0; k < frameLen && off + k < raw.length; k++) {
                sum += raw[off + k] * raw[off + k];
            }
            rms[f] = Math.sqrt(sum / frameLen);
        }
        double peak = 0;
        for (double v : rms) {
            peak = Math.max(peak, v);
        }
        if (peak < 1e-4) {
            return List.of();
        }
        double threshold = Math.max(peak * 0.12, 1e-3);
        boolean[] speech = new boolean[frames];
        int pad = 12; // 120ms 缓冲，避免剪掉词首尾的弱音
        for (int f = 0; f < frames; f++) {
            if (rms[f] >= threshold) {
                for (int k = Math.max(0, f - pad); k <= Math.min(frames - 1, f + pad); k++) {
                    speech[k] = true;
                }
            }
        }
        List<PhonemeRecognizer.PhonemeHit> kept = new ArrayList<>(hits.size());
        for (PhonemeRecognizer.PhonemeHit hit : hits) {
            int f = (int) ((hit.start() + hit.end()) / 2 / 0.010);
            if (f >= 0 && f < frames && speech[f]) {
                kept.add(hit);
            }
        }
        return kept;
    }

    private static double clamp(double v) {
        return clamp(v, 0, 100);
    }

    private static double clamp(double v, double low, double high) {
        return Math.max(low, Math.min(high, v));
    }

    private Engine ensureEngine() {
        if (!enabled) {
            return null;
        }
        if (initialized) {
            return engine;
        }
        synchronized (lock) {
            if (initialized) {
                return engine;
            }
            try {
                CmuDictionary dict = CmuDictionary.load(Path.of(dictPath));
                PhonemeRecognizer recognizer = new PhonemeRecognizer(Path.of(modelPath), Path.of(vocabPath));
                engine = new Engine(dict, recognizer);
                log.info("[发音评测] 音素模型加载完成：{}", modelPath);
            } catch (Exception e) {
                log.warn("[发音评测] 音素模型加载失败，跟读将回退为文本比对评分: {}", e.getMessage());
                engine = null;
            }
            initialized = true;
            return engine;
        }
    }

    /** 参考句子的音素展开结果 */
    private record TextPhonemes(List<String> phonemes, List<Span> spans, List<String> skipped) {
        record Span(String word, int start, int length) {
        }
    }

    private record Engine(CmuDictionary dict, PhonemeRecognizer recognizer) {
    }
}
