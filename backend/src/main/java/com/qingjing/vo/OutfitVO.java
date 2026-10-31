package com.qingjing.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 穿搭套装展示对象，itemNames 用于列表页快速预览。
 */
public record OutfitVO(
        Long id,
        String name,
        String coverUrl,
        String occasion,
        String season,
        List<Long> itemIds,
        List<String> itemNames,
        Integer score,
        Boolean aiGenerated,
        String aiReason,
        LocalDateTime createdAt
) {
}