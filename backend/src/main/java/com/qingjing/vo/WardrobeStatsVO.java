package com.qingjing.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 衣橱概览统计，驱动前端顶部的数据看板。
 */
public record WardrobeStatsVO(
        int totalCount,
        int favoriteCount,
        int unwornCount,
        int categoryCount,
        BigDecimal totalValue,
        double avgWearCount,
        Map<String, Integer> categoryCounts,
        List<String> topWornNames
) {
}