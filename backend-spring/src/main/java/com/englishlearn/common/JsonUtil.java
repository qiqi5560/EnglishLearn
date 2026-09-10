package com.englishlearn.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON 序列化/反序列化工具（用于 TEXT 列中存储的 JSON 串）。
 */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {
    }

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }

    public static Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            return null;
        }
    }

    public static List<Object> parseList(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<Object>>() {
            });
        } catch (Exception e) {
            return null;
        }
    }
}