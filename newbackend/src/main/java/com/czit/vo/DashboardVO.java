package com.czit.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数据看板聚合结果。
 *
 * <p>一次请求返回首页需要的全部数据：概览卡片、品类分布、风格标签、热门单品、
 * 以及最近 14 天的 AI / 社区 / 穿着活跃趋势，避免前端串行请求。</p>
 *
 * <p>cpw / cpwItems / wearHeatmap / heatmapSummary 为增量字段，统一追加在末尾，
 * 保证原有调用方按字段顺序或反序列化时不受影响。</p>
 */
public record DashboardVO(
        Overview overview,
        List<NamedValue> categoryDistribution,
        List<NamedValue> styleTags,
        List<NamedValue> valueByCategory,
        List<HotItem> topWorn,
        List<TrendPoint> trends,
        Cpw cpw,
        List<CpwItem> cpwItems,
        List<HeatCell> wearHeatmap,
        HeatSummary heatmapSummary
) {

    /** 概览卡片 */
    public record Overview(
            long clothingCount,
            long outfitCount,
            long wearTotal,
            long favoriteCount,
            long unwornCount,
            long aiSessions,
            long postCount,
            long interactions,
            long recommendShown,
            long recommendAdopted,
            double adoptRate,
            BigDecimal totalValue
    ) {
    }

    public record NamedValue(String name, long value) {
    }

    /** 热门单品（按穿着次数排序） */
    public record HotItem(
            long id,
            String name,
            String imageUrl,
            String categoryLabel,
            String colorHex,
            long wearCount
    ) {
    }

    /** 最近 14 天每天的活跃情况：AI 会话 / 社区发帖 / 记录穿着 */
    public record TrendPoint(String date, long ai, long community, long wear) {
    }

    /** 单次穿着成本概览：衣橱总价值 / 总穿着次数 / 平均 CPW / 最划算与最贵的单品 */
    public record Cpw(
            BigDecimal totalValue,
            long totalWears,
            BigDecimal avgCpw,
            CpwItem bestValue,
            CpwItem worstValue
    ) {
    }

    /** 单品 CPW：price / max(wearCount, 1)，保留 2 位小数 */
    public record CpwItem(
            long id,
            String name,
            String categoryLabel,
            String imageUrl,
            String colorHex,
            BigDecimal price,
            long wearCount,
            BigDecimal cpw
    ) {
    }

    /** 热力图单日格子：date 为 yyyy-MM-dd，count 为当天的穿着记录条数 */
    public record HeatCell(String date, long count) {
    }

    /** 热力图汇总：activeDays 活跃天数 / maxStreak 最长连续 / totalWears 总次数 / from~to 统计窗口 */
    public record HeatSummary(
            long activeDays,
            long maxStreak,
            long totalWears,
            String from,
            String to
    ) {
    }
}
