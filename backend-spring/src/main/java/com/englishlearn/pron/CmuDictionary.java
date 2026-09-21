package com.englishlearn.pron;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * cmudict 发音词典：英文单词 → ARPAbet 音素序列（G2P 的文本侧数据来源）。
 *
 * <p>词典文件为 {@code .dict} 原始格式，每行形如 {@code WORD  DH AH0}，
 * 变体词写作 {@code WORD(2)}。加载后按小写词索引，取第一个发音。
 */
public final class CmuDictionary {

    private static final Pattern NON_LETTER = Pattern.compile("[^a-zA-Z']");
    private static final Pattern VARIANT = Pattern.compile("\\(\\d+\\)$");

    private final Map<String, List<String>> entries;

    private CmuDictionary(Map<String, List<String>> entries) {
        this.entries = entries;
    }

    /** 解析词典文件（cmudict.dict，约 3.4MB / 13 万词） */
    public static CmuDictionary load(Path dictPath) throws Exception {
        Map<String, List<String>> map = new HashMap<>(140_000);
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(dictPath), decoder))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == ';' || line.charAt(0) == '#') {
                    continue;
                }
                // 分隔符是「词 + 空白 + 音素串」，官方文件用单空格，这里兼容空格与制表符
                String[] parts = line.trim().split("\\s+", 2);
                if (parts.length < 2) {
                    continue;
                }
                String rawKey = parts[0].trim();
                String phonemes = parts[1].trim();
                if (rawKey.isEmpty() || phonemes.isEmpty()) {
                    continue;
                }
                String key = VARIANT.matcher(rawKey).replaceFirst("").toLowerCase();
                // 变体只保留首次出现的发音，保证结果稳定
                if (map.containsKey(key)) {
                    continue;
                }
                map.put(key, splitPhonemes(phonemes));
            }
        }
        return new CmuDictionary(map);
    }

    private static List<String> splitPhonemes(String phonemes) {
        List<String> out = new ArrayList<>();
        for (String p : phonemes.split("\\s+")) {
            if (!p.isEmpty()) {
                out.add(p);
            }
        }
        return out;
    }

    /** 去掉标点、转小写；保留单词内部的撇号（don't 这类词在词典里有独立条目） */
    public String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String kept = NON_LETTER.matcher(raw).replaceAll("");
        return kept.replaceAll("^'+|'+$", "").toLowerCase();
    }

    /**
     * 查单词发音。查不到时按常见词形变化（复数 / 过去式 / 进行时）回退一次，
     * 避免 cats / wanted 这类词被判成「词典未收录」而跳过评测。
     */
    public List<String> lookup(String word) {
        String key = clean(word);
        if (key.isEmpty()) {
            return null;
        }
        List<String> hit = entries.get(key);
        if (hit != null) {
            return hit;
        }
        for (String stem : stems(key)) {
            hit = entries.get(stem);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    private List<String> stems(String key) {
        List<String> out = new ArrayList<>(4);
        if (key.endsWith("ies") && key.length() > 3) {
            out.add(key.substring(0, key.length() - 3) + "y");
        }
        if (key.endsWith("es") && key.length() > 2) {
            out.add(key.substring(0, key.length() - 2));
        }
        if (key.endsWith("s") && key.length() > 1) {
            out.add(key.substring(0, key.length() - 1));
        }
        if (key.endsWith("ed") && key.length() > 2) {
            out.add(key.substring(0, key.length() - 2));
            out.add(key.substring(0, key.length() - 1));
        }
        if (key.endsWith("ing") && key.length() > 3) {
            out.add(key.substring(0, key.length() - 3));
            out.add(key.substring(0, key.length() - 3) + "e");
        }
        return out;
    }
}
