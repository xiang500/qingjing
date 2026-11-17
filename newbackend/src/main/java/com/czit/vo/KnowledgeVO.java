package com.czit.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库片段展示对象。
 */
public record KnowledgeVO(
        Long id,
        String title,
        String category,
        List<String> tags,
        String content,
        boolean enabled,
        LocalDateTime createdAt
) {
}
