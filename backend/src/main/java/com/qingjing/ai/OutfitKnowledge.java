package com.qingjing.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 轻量 RAG：把穿搭知识切成片段，用「中文二元组 + 标签命中」做检索，
 * 取 Top-K 注入 system prompt。
 *
 * <p>相比向量检索，它零依赖、可解释、冷启动快；后续接入 embedding 时
 * 只需替换 {@link #search(String, int)} 的实现，调用方不变。</p>
 */
@Component
public class OutfitKnowledge {

    private static final Logger log = LoggerFactory.getLogger(OutfitKnowledge.class);

    public record Chunk(String id, String title, List<String> tags, String content) {
    }

    private final List<Chunk> chunks = new ArrayList<>();

    public OutfitKnowledge(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource("ai/outfit-knowledge.json").getInputStream()) {
            chunks.addAll(objectMapper.readValue(in, new TypeReference<List<Chunk>>() {
            }));
            log.info("穿搭知识库加载完成，共 {} 个片段", chunks.size());
        } catch (Exception ex) {
            log.warn("穿搭知识库加载失败，RAG 将退化为空上下文: {}", ex.getMessage());
        }
    }

    public List<Chunk> search(String query, int topK) {
        if (query == null || query.isBlank() || chunks.isEmpty()) {
            return List.of();
        }
        Set<String> grams = bigrams(query);
        return chunks.stream()
                .map(chunk -> new Scored(chunk, score(chunk, grams, query)))
                .filter(scored -> scored.score > 0)
                .sorted((a, b) -> Integer.compare(b.score, a.score))
                .limit(Math.max(1, topK))
                .map(scored -> scored.chunk)
                .toList();
    }

    public int size() {
        return chunks.size();
    }

    private int score(Chunk chunk, Set<String> grams, String query) {
        String haystack = chunk.title + " " + String.join(" ", chunk.tags) + " " + chunk.content;
        int score = 0;
        for (String gram : grams) {
            if (haystack.contains(gram)) {
                score += chunk.title.contains(gram) ? 3 : 1;
            }
        }
        for (String tag : chunk.tags) {
            if (query.contains(tag)) {
                score += 4;
            }
        }
        return score;
    }

    private Set<String> bigrams(String text) {
        String cleaned = text.replaceAll("[\\p{Punct}\\s]+", "");
        Set<String> grams = new LinkedHashSet<>();
        for (int i = 0; i + 1 < cleaned.length(); i++) {
            grams.add(cleaned.substring(i, i + 2));
        }
        // 英文/数字关键词直接保留
        Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
                .filter(word -> word.length() >= 2)
                .forEach(grams::add);
        return grams;
    }

    private record Scored(Chunk chunk, int score) {
    }
}