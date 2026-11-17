package com.czit.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.czit.entity.KnowledgeChunk;
import com.czit.mapper.KnowledgeChunkMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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
 * 轻量 RAG：把穿搭知识切成片段，用「中文二元组 + 标签命中」做检索，取 Top-K 注入 system prompt。
 *
 * <p>知识片段现在存在 `knowledge_chunk` 表里（管理端可增删改），首次启动时如果表为空，
 * 会自动从 `ai/outfit-knowledge.json` 初始化；数据库不可用时降级为直接读 JSON。
 * 相比向量检索，它零依赖、可解释、冷启动快；后续接入 embedding 只需替换 search 的实现。</p>
 */
@Component
public class OutfitKnowledge {

    private static final Logger log = LoggerFactory.getLogger(OutfitKnowledge.class);

    public record Chunk(String id, String title, List<String> tags, String content) {
    }

    private final List<Chunk> chunks = new ArrayList<>();
    private final ObjectMapper objectMapper;
    private final KnowledgeChunkMapper knowledgeMapper;

    public OutfitKnowledge(ObjectMapper objectMapper, KnowledgeChunkMapper knowledgeMapper) {
        this.objectMapper = objectMapper;
        this.knowledgeMapper = knowledgeMapper;
    }

    /** 启动时加载；管理端改完知识库后也会调用它重建索引。 */
    @PostConstruct
    public synchronized void refresh() {
        List<Chunk> loaded = new ArrayList<>();
        try {
            List<KnowledgeChunk> rows = knowledgeMapper.selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                    .eq(KnowledgeChunk::getEnabled, 1));
            if (rows.isEmpty()) {
                seedFromJson();
                rows = knowledgeMapper.selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                        .eq(KnowledgeChunk::getEnabled, 1));
            }
            for (KnowledgeChunk row : rows) {
                loaded.add(new Chunk(String.valueOf(row.getId()), row.getTitle(), parseTags(row.getTags()), row.getContent()));
            }
            log.info("穿搭知识库加载完成，共 {} 个片段（来源：knowledge_chunk 表）", loaded.size());
        } catch (Exception ex) {
            log.warn("知识库读取数据库失败，降级读取 JSON: {}", ex.getMessage());
            loaded.addAll(loadJson());
        }
        chunks.clear();
        chunks.addAll(loaded);
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

    private void seedFromJson() {
        List<Chunk> json = loadJson();
        for (Chunk chunk : json) {
            KnowledgeChunk row = new KnowledgeChunk();
            row.setTitle(chunk.title());
            row.setCategory("通用");
            row.setTags(chunk.tags() == null || chunk.tags().isEmpty() ? null : String.join(",", chunk.tags()));
            row.setContent(chunk.content());
            row.setEnabled(1);
            row.setDeleted(0);
            knowledgeMapper.insert(row);
        }
        log.info("knowledge_chunk 表为空，已从 JSON 初始化 {} 个片段", json.size());
    }

    private List<Chunk> loadJson() {
        try (InputStream in = new ClassPathResource("ai/outfit-knowledge.json").getInputStream()) {
            List<Chunk> json = objectMapper.readValue(in, new TypeReference<List<Chunk>>() {
            });
            log.info("从 JSON 加载穿搭知识 {} 个片段", json.size());
            return json;
        } catch (Exception ex) {
            log.warn("穿搭知识库加载失败，RAG 将退化为空上下文: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<String> parseTags(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }

    private int score(Chunk chunk, Set<String> grams, String query) {
        String haystack = chunk.title + " " + String.join(" ", chunk.tags()) + " " + chunk.content;
        int score = 0;
        for (String gram : grams) {
            if (haystack.contains(gram)) {
                score += chunk.title.contains(gram) ? 3 : 1;
            }
        }
        for (String tag : chunk.tags()) {
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
