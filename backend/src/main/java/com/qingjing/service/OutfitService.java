package com.qingjing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingjing.common.JsonUtils;
import com.qingjing.common.UserContext;
import com.qingjing.entity.Clothing;
import com.qingjing.entity.Outfit;
import com.qingjing.mapper.ClothingMapper;
import com.qingjing.mapper.OutfitMapper;
import com.qingjing.vo.OutfitVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 穿搭套装查询（阶段一只读，写入由阶段三的 AI Agent 产生）。
 */
@Service
public class OutfitService {

    private final OutfitMapper outfitMapper;
    private final ClothingMapper clothingMapper;

    public OutfitService(OutfitMapper outfitMapper, ClothingMapper clothingMapper) {
        this.outfitMapper = outfitMapper;
        this.clothingMapper = clothingMapper;
    }

    public List<OutfitVO> list() {
        long userId = UserContext.requireUserId();
        List<Outfit> outfits = outfitMapper.selectList(new LambdaQueryWrapper<Outfit>()
                .eq(Outfit::getUserId, userId)
                .orderByDesc(Outfit::getCreatedAt));

        List<Long> allItemIds = outfits.stream()
                .flatMap(outfit -> JsonUtils.parseLongList(outfit.getItemIds()).stream())
                .distinct()
                .toList();

        Map<Long, String> nameById = allItemIds.isEmpty()
                ? Map.of()
                : clothingMapper.selectBatchIds(allItemIds).stream()
                        .collect(Collectors.toMap(Clothing::getId, Clothing::getName, (a, b) -> a));

        return outfits.stream().map(outfit -> {
            List<Long> itemIds = JsonUtils.parseLongList(outfit.getItemIds());
            List<String> itemNames = itemIds.stream()
                    .map(id -> nameById.getOrDefault(id, "已移除单品"))
                    .toList();
            return new OutfitVO(
                    outfit.getId(),
                    outfit.getName(),
                    outfit.getCoverUrl(),
                    outfit.getOccasion(),
                    outfit.getSeason(),
                    itemIds,
                    itemNames,
                    outfit.getScore(),
                    outfit.getAiGenerated(),
                    outfit.getAiReason(),
                    outfit.getCreatedAt());
        }).toList();
    }
}