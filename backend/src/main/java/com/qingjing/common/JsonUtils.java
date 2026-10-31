package com.qingjing.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON 列与 Java 集合之间的轻量转换。解析失败时降级为空集合，
 * 避免一条脏数据让整个衣橱列表 500。
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private JsonUtils() {
    }

    public static List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(json, STRING_LIST);
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public static List<Long> parseLongList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<Long>>() {
            });
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public static String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            return null;
        }
    }

    /** 逗号分隔字符串 → 列表，用于 season 这类简单枚举集合。 */
    public static List<String> splitCsv(String csv) {
        List<String> result = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return result;
        }
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    public static String joinCsv(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(",", values);
    }
}