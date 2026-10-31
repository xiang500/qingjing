package com.qingjing.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 衣橱单品展示对象。前端 types/clothing.ts 与此字段一一对应。
 */
public record ClothingVO(
        Long id,
        String name,
        String category,
        String categoryLabel,
        String accentColor,
        String subCategory,
        String color,
        String colorHex,
        List<String> season,
        List<String> styleTags,
        String material,
        String brand,
        String imageUrl,
        String modelUrl,
        BigDecimal price,
        LocalDate purchaseDate,
        Integer wearCount,
        LocalDate lastWornAt,
        Boolean favorite,
        String note,
        LocalDateTime createdAt
) {
}