package com.qingjing.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingjing.common.JsonUtils;
import com.qingjing.common.UserContext;
import com.qingjing.entity.Clothing;
import com.qingjing.mapper.ClothingMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 衣橱检索工具：让模型按分类 / 季节 / 关键词 / 场景标签查真实单品，
 * 而不是凭空编造不存在的衣服。
 */
@Component
public class WardrobeSearchTool implements AgentTool {

    private final ClothingMapper clothingMapper;

    public WardrobeSearchTool(ClothingMapper clothingMapper) {
        this.clothingMapper = clothingMapper;
    }

    @Override
    public String name() {
        return "search_wardrobe";
    }

    @Override
    public String description() {
        return "在当前用户的数字衣橱中检索单品，可按分类、季节、关键词、风格标签过滤。返回结果按穿着次数升序（优先推荐久未穿着的单品）。";
    }

    @Override
    public Map<String, Object> parameters() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("category", Map.of(
                "type", "string",
                "enum", List.of("TOP", "BOTTOM", "OUTER", "DRESS", "SHOES", "BAG", "ACCESSORY"),
                "description", "分类编码"));
        properties.put("season", Map.of(
                "type", "string",
                "enum", List.of("SPRING", "SUMMER", "AUTUMN", "WINTER"),
                "description", "适穿季节"));
        properties.put("keyword", Map.of("type", "string", "description", "名称、品牌或颜色关键词"));
        properties.put("tag", Map.of("type", "string", "description", "风格标签，例如 通勤、约会、法式"));
        properties.put("limit", Map.of("type", "integer", "description", "返回条数，默认 8，最多 12"));
        return Map.of("type", "object", "properties", properties, "required", List.of());
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        long userId = UserContext.requireUserId();
        LambdaQueryWrapper<Clothing> wrapper = new LambdaQueryWrapper<Clothing>()
                .eq(Clothing::getUserId, userId);

        String category = text(arguments.get("category"));
        if (category != null) {
            wrapper.eq(Clothing::getCategory, category.toUpperCase());
        }
        String season = text(arguments.get("season"));
        if (season != null) {
            wrapper.like(Clothing::getSeason, season.toUpperCase());
        }
        String keyword = text(arguments.get("keyword"));
        if (keyword != null) {
            wrapper.and(inner -> inner.like(Clothing::getName, keyword)
                    .or().like(Clothing::getBrand, keyword)
                    .or().like(Clothing::getColor, keyword));
        }
        String tag = text(arguments.get("tag"));
        if (tag != null) {
            wrapper.like(Clothing::getStyleTags, tag);
        }

        int limit = arguments.get("limit") instanceof Number number ? number.intValue() : 8;
        limit = Math.max(1, Math.min(12, limit));

        List<Clothing> found = clothingMapper.selectList(wrapper);
        List<Clothing> ordered = new ArrayList<>(found);
        ordered.sort(Comparator.comparingInt((Clothing item) -> item.getWearCount() == null ? 0 : item.getWearCount())
                .thenComparing(Clothing::getId));

        List<Map<String, Object>> items = ordered.stream().limit(limit).map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("name", item.getName());
            row.put("category", item.getCategory());
            row.put("subCategory", item.getSubCategory());
            row.put("color", item.getColor());
            row.put("colorHex", item.getColorHex());
            row.put("season", JsonUtils.splitCsv(item.getSeason()));
            row.put("styleTags", JsonUtils.parseStringList(item.getStyleTags()));
            row.put("wearCount", item.getWearCount());
            row.put("lastWornAt", item.getLastWornAt());
            row.put("favorite", item.getFavorite());
            row.put("imageUrl", item.getImageUrl());
            return row;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("matched", found.size());
        result.put("returned", items.size());
        result.put("items", items);
        return result;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}