package com.qingjing.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingjing.common.ClothingCategory;
import com.qingjing.common.UserContext;
import com.qingjing.entity.Clothing;
import com.qingjing.mapper.ClothingMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 衣橱统计工具：给模型一个「衣橱体检报告」，
 * 让它能回答「我有哪些从没穿过的衣服」「这个月买了多少钱」这类问题。
 */
@Component
public class WardrobeStatsTool implements AgentTool {

    private final ClothingMapper clothingMapper;

    public WardrobeStatsTool(ClothingMapper clothingMapper) {
        this.clothingMapper = clothingMapper;
    }

    @Override
    public String name() {
        return "wardrobe_stats";
    }

    @Override
    public String description() {
        return "统计当前用户衣橱的总体情况：总件数、收藏数、从未穿着件数、总价值、各分类数量与最常穿单品。";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of("type", "object", "properties", Map.of(), "required", List.of());
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        long userId = UserContext.requireUserId();
        List<Clothing> all = clothingMapper.selectList(new LambdaQueryWrapper<Clothing>()
                .eq(Clothing::getUserId, userId));

        Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        int favorite = 0;
        int unworn = 0;
        BigDecimal value = BigDecimal.ZERO;
        for (Clothing item : all) {
            categoryCounts.merge(ClothingCategory.labelOf(item.getCategory()), 1, Integer::sum);
            if (Boolean.TRUE.equals(item.getFavorite())) {
                favorite++;
            }
            if (item.getWearCount() == null || item.getWearCount() == 0) {
                unworn++;
            }
            if (item.getPrice() != null) {
                value = value.add(item.getPrice());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", all.size());
        result.put("favorite", favorite);
        result.put("unworn", unworn);
        result.put("totalValue", value.doubleValue());
        result.put("categoryCounts", categoryCounts);
        result.put("unwornNames", all.stream()
                .filter(item -> item.getWearCount() == null || item.getWearCount() == 0)
                .map(Clothing::getName)
                .toList());
        return result;
    }
}