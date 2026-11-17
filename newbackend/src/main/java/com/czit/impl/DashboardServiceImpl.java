package com.czit.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.czit.common.ClothingCategory;
import com.czit.common.JsonUtils;
import com.czit.common.UserContext;
import com.czit.entity.ChatSession;
import com.czit.entity.Clothing;
import com.czit.entity.Outfit;
import com.czit.entity.Post;
import com.czit.entity.WearLog;
import com.czit.mapper.ChatSessionMapper;
import com.czit.mapper.ClothingMapper;
import com.czit.mapper.OutfitMapper;
import com.czit.mapper.PostMapper;
import com.czit.mapper.WearLogMapper;
import com.czit.service.DashboardService;
import com.czit.service.RecommendationService;
import com.czit.vo.DashboardVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 数据看板聚合实现：全部在内存里按当前用户聚合，数据量不大，
 * 避免为看板单独写一堆 SQL。
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final int TREND_DAYS = 14;
    /** 热力图窗口：今天 + 往前 364 天，共 365 天 */
    private static final int HEATMAP_DAYS = 365;
    /** 单品 CPW 排行最多返回条数 */
    private static final int CPW_ITEM_LIMIT = 8;
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter FULL_DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ClothingMapper clothingMapper;
    private final OutfitMapper outfitMapper;
    private final PostMapper postMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final WearLogMapper wearLogMapper;
    private final RecommendationService recommendationService;

    public DashboardServiceImpl(ClothingMapper clothingMapper,
                                OutfitMapper outfitMapper,
                                PostMapper postMapper,
                                ChatSessionMapper chatSessionMapper,
                                WearLogMapper wearLogMapper,
                                RecommendationService recommendationService) {
        this.clothingMapper = clothingMapper;
        this.outfitMapper = outfitMapper;
        this.postMapper = postMapper;
        this.chatSessionMapper = chatSessionMapper;
        this.wearLogMapper = wearLogMapper;
        this.recommendationService = recommendationService;
    }

    @Override
    public DashboardVO dashboard() {
        long userId = UserContext.requireUserId();

        List<Clothing> clothing = clothingMapper.selectList(new LambdaQueryWrapper<Clothing>()
                .eq(Clothing::getUserId, userId));
        List<Post> posts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .eq(Post::getStatus, 1));
        List<ChatSession> sessions = chatSessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId));
        long outfitCount = outfitMapper.selectCount(new LambdaQueryWrapper<Outfit>()
                .eq(Outfit::getUserId, userId));

        long wearTotal = 0;
        long favoriteCount = 0;
        long unwornCount = 0;
        BigDecimal totalValue = BigDecimal.ZERO;
        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        for (ClothingCategory category : ClothingCategory.values()) {
            categoryCounts.put(category.getLabel(), 0L);
        }
        Map<String, Long> tagCounts = new LinkedHashMap<>();
        Map<String, BigDecimal> valueByCategory = new LinkedHashMap<>();
        for (ClothingCategory category : ClothingCategory.values()) {
            valueByCategory.put(category.getLabel(), BigDecimal.ZERO);
        }

        for (Clothing item : clothing) {
            int wearCount = item.getWearCount() == null ? 0 : item.getWearCount();
            wearTotal += wearCount;
            if (Boolean.TRUE.equals(item.getFavorite())) {
                favoriteCount++;
            }
            if (wearCount == 0) {
                unwornCount++;
            }
            if (item.getPrice() != null) {
                totalValue = totalValue.add(item.getPrice());
            }
            String label = ClothingCategory.labelOf(item.getCategory());
            categoryCounts.merge(label, 1L, Long::sum);
            if (item.getPrice() != null) {
                valueByCategory.merge(label, item.getPrice(), BigDecimal::add);
            }
            for (String tag : JsonUtils.parseStringList(item.getStyleTags())) {
                tagCounts.merge(tag, 1L, Long::sum);
            }
        }

        List<DashboardVO.NamedValue> categoryDistribution = categoryCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new DashboardVO.NamedValue(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(DashboardVO.NamedValue::value).reversed())
                .toList();

        List<DashboardVO.NamedValue> styleTags = tagCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(entry -> new DashboardVO.NamedValue(entry.getKey(), entry.getValue()))
                .toList();

        List<DashboardVO.NamedValue> valueByCategoryList = valueByCategory.entrySet().stream()
                .filter(entry -> entry.getValue().signum() > 0)
                .map(entry -> new DashboardVO.NamedValue(entry.getKey(),
                        entry.getValue().setScale(0, RoundingMode.HALF_UP).longValue()))
                .sorted(Comparator.comparingLong(DashboardVO.NamedValue::value).reversed())
                .toList();

        List<DashboardVO.HotItem> topWorn = clothing.stream()
                .sorted(Comparator.comparingInt((Clothing item) ->
                        item.getWearCount() == null ? 0 : item.getWearCount()).reversed())
                .limit(6)
                .map(item -> new DashboardVO.HotItem(
                        item.getId(),
                        item.getName(),
                        item.getImageUrl(),
                        ClothingCategory.labelOf(item.getCategory()),
                        item.getColorHex(),
                        item.getWearCount() == null ? 0 : item.getWearCount()))
                .toList();

        Map<LocalDate, long[]> daily = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            daily.put(today.minusDays(i), new long[3]);
        }
        for (ChatSession session : sessions) {
            addIfInRange(daily, session.getCreatedAt() == null ? null : session.getCreatedAt().toLocalDate(), 0);
        }
        for (Post post : posts) {
            addIfInRange(daily, post.getCreatedAt() == null ? null : post.getCreatedAt().toLocalDate(), 1);
        }
        for (Clothing item : clothing) {
            addIfInRange(daily, item.getLastWornAt(), 2);
        }
        List<DashboardVO.TrendPoint> trends = daily.entrySet().stream()
                .map(entry -> new DashboardVO.TrendPoint(
                        entry.getKey().format(DAY_FORMAT),
                        entry.getValue()[0],
                        entry.getValue()[1],
                        entry.getValue()[2]))
                .toList();

        long likeSum = 0;
        long commentSum = 0;
        for (Post post : posts) {
            likeSum += post.getLikeCount() == null ? 0 : post.getLikeCount();
            commentSum += post.getCommentCount() == null ? 0 : post.getCommentCount();
        }

        RecommendationService.AdoptionStats adoption = recommendationService.stats(userId);

        DashboardVO.Overview overview = new DashboardVO.Overview(
                clothing.size(),
                outfitCount,
                wearTotal,
                favoriteCount,
                unwornCount,
                sessions.size(),
                posts.size(),
                likeSum + commentSum,
                adoption.shown(),
                adoption.adopted(),
                adoption.rate(),
                totalValue.setScale(2, RoundingMode.HALF_UP));

        // ===== CPW（单次穿着成本）：单品 CPW = price / max(wearCount, 1) =====
        List<DashboardVO.CpwItem> cpwCandidates = new ArrayList<>();
        for (Clothing item : clothing) {
            if (item.getPrice() == null) {
                continue;
            }
            int wearCount = item.getWearCount() == null ? 0 : item.getWearCount();
            BigDecimal cpw = item.getPrice()
                    .divide(BigDecimal.valueOf(Math.max(wearCount, 1)), 2, RoundingMode.HALF_UP);
            cpwCandidates.add(new DashboardVO.CpwItem(
                    item.getId(),
                    item.getName(),
                    ClothingCategory.labelOf(item.getCategory()),
                    item.getImageUrl(),
                    item.getColorHex(),
                    item.getPrice(),
                    wearCount,
                    cpw));
        }
        // 排行按 CPW 升序，最划算的排前面
        List<DashboardVO.CpwItem> cpwItems = cpwCandidates.stream()
                .sorted(Comparator.comparing(DashboardVO.CpwItem::cpw))
                .limit(CPW_ITEM_LIMIT)
                .toList();
        // 最划算 / 最贵只统计真正穿过且标了价格的单品，避免 wearCount=0 拉出 0 元或极端值
        List<DashboardVO.CpwItem> wornCpwItems = cpwCandidates.stream()
                .filter(item -> item.wearCount() > 0)
                .toList();
        DashboardVO.CpwItem bestValue = wornCpwItems.stream()
                .min(Comparator.comparing(DashboardVO.CpwItem::cpw))
                .orElse(null);
        DashboardVO.CpwItem worstValue = wornCpwItems.stream()
                .max(Comparator.comparing(DashboardVO.CpwItem::cpw))
                .orElse(null);
        DashboardVO.Cpw cpw = new DashboardVO.Cpw(
                totalValue.setScale(2, RoundingMode.HALF_UP),
                wearTotal,
                totalValue.divide(BigDecimal.valueOf(Math.max(wearTotal, 1)), 2, RoundingMode.HALF_UP),
                bestValue,
                worstValue);

        // ===== 365 天穿着热力图：按天聚合 wear_log，只返回有记录的天 =====
        LocalDate heatFrom = today.minusDays(HEATMAP_DAYS - 1L);
        List<WearLog> wearLogs = wearLogMapper.selectList(new LambdaQueryWrapper<WearLog>()
                .eq(WearLog::getUserId, userId)
                .ge(WearLog::getWornDate, heatFrom)
                .le(WearLog::getWornDate, today));

        Map<LocalDate, Long> countByDay = new TreeMap<>();
        for (WearLog wearLog : wearLogs) {
            if (wearLog.getWornDate() == null) {
                continue;
            }
            countByDay.merge(wearLog.getWornDate(), 1L, Long::sum);
        }
        List<DashboardVO.HeatCell> wearHeatmap = countByDay.entrySet().stream()
                .map(entry -> new DashboardVO.HeatCell(
                        entry.getKey().format(FULL_DAY_FORMAT), entry.getValue()))
                .toList();

        long maxStreak = 0;
        long streak = 0;
        long heatTotalWears = 0;
        LocalDate previousDay = null;
        for (Map.Entry<LocalDate, Long> entry : countByDay.entrySet()) {
            LocalDate day = entry.getKey();
            streak = (previousDay != null && day.equals(previousDay.plusDays(1))) ? streak + 1 : 1;
            maxStreak = Math.max(maxStreak, streak);
            heatTotalWears += entry.getValue();
            previousDay = day;
        }
        DashboardVO.HeatSummary heatmapSummary = new DashboardVO.HeatSummary(
                countByDay.size(),
                maxStreak,
                heatTotalWears,
                heatFrom.format(FULL_DAY_FORMAT),
                today.format(FULL_DAY_FORMAT));

        return new DashboardVO(overview, categoryDistribution, styleTags, valueByCategoryList, topWorn, trends,
                cpw, cpwItems, wearHeatmap, heatmapSummary);
    }

    private void addIfInRange(Map<LocalDate, long[]> daily, LocalDate date, int index) {
        if (date == null) {
            return;
        }
        long[] slot = daily.get(date);
        if (slot != null) {
            slot[index]++;
        }
    }
}
