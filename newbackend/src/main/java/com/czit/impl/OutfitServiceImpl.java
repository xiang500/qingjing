package com.czit.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.czit.common.BizException;
import com.czit.common.ErrorCode;
import com.czit.common.JsonUtils;
import com.czit.common.UserContext;
import com.czit.entity.Clothing;
import com.czit.entity.Outfit;
import com.czit.mapper.ClothingMapper;
import com.czit.mapper.OutfitMapper;
import com.czit.service.OutfitService;
import com.czit.vo.OutfitVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 穿搭套装实现：查询、保存（工作台 / AI 推荐卡）、删除。
 */
@Service
public class OutfitServiceImpl implements OutfitService {

    private final OutfitMapper outfitMapper;
    private final ClothingMapper clothingMapper;

    public OutfitServiceImpl(OutfitMapper outfitMapper, ClothingMapper clothingMapper) {
        this.outfitMapper = outfitMapper;
        this.clothingMapper = clothingMapper;
    }

    @Override
    public List<OutfitVO> list() {
        long userId = UserContext.requireUserId();
        List<Outfit> outfits = outfitMapper.selectList(new LambdaQueryWrapper<Outfit>()
                .eq(Outfit::getUserId, userId)
                .orderByDesc(Outfit::getCreatedAt));

        if (outfits.isEmpty()) {
            return List.of();
        }
        Map<Long, Clothing> clothingById = loadClothing(outfits.stream()
                .flatMap(outfit -> JsonUtils.parseLongList(outfit.getItemIds()).stream())
                .distinct()
                .toList());
        return outfits.stream().map(outfit -> toVO(outfit, clothingById)).toList();
    }

    @Override
    @Transactional
    public OutfitVO create(SaveOutfitRequest request) {
        long userId = UserContext.requireUserId();
        if (request == null) {
            throw new BizException("参数不能为空");
        }
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new BizException("给这套搭配起个名字吧");
        }
        if (name.length() > 60) {
            throw new BizException("套装名称最多 60 字");
        }
        List<Long> itemIds = request.itemIds() == null ? List.of()
                : request.itemIds().stream().filter(id -> id != null && id > 0).distinct().toList();
        if (itemIds.isEmpty()) {
            throw new BizException("至少选择一件单品");
        }
        if (itemIds.size() > 12) {
            throw new BizException("一套搭配最多 12 件单品");
        }

        // 只能保存自己衣橱里的单品，防止越权引用别人的 clothes id
        Map<Long, Clothing> clothingById = loadClothing(itemIds);
        if (clothingById.size() != itemIds.size()) {
            throw new BizException(ErrorCode.NOT_FOUND, "部分单品不存在或不在你的衣橱里");
        }

        Outfit outfit = new Outfit();
        outfit.setUserId(userId);
        outfit.setName(name);
        outfit.setCoverUrl(clothingById.get(itemIds.get(0)).getImageUrl());
        outfit.setOccasion(trimToNull(request.occasion()));
        outfit.setSeason(trimToNull(request.season()));
        outfit.setItemIds(JsonUtils.write(itemIds));
        outfit.setScore(request.score());
        outfit.setAiGenerated(false);
        outfit.setAiReason(trimToNull(request.reason()));
        outfit.setDeleted(0);
        outfitMapper.insert(outfit);

        return toVO(outfit, clothingById);
    }

    @Override
    @Transactional
    public void delete(long outfitId) {
        long userId = UserContext.requireUserId();
        Outfit outfit = outfitMapper.selectById(outfitId);
        if (outfit == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "套装不存在");
        }
        if (outfit.getUserId() == null || outfit.getUserId() != userId) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只能删除自己的套装");
        }
        outfitMapper.deleteById(outfitId);
    }

    private Map<Long, Clothing> loadClothing(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Map.of();
        }
        long userId = UserContext.requireUserId();
        return clothingMapper.selectList(new LambdaQueryWrapper<Clothing>()
                        .eq(Clothing::getUserId, userId)
                        .in(Clothing::getId, itemIds))
                .stream()
                .collect(Collectors.toMap(Clothing::getId, item -> item, (a, b) -> a));
    }

    private OutfitVO toVO(Outfit outfit, Map<Long, Clothing> clothingById) {
        List<Long> itemIds = JsonUtils.parseLongList(outfit.getItemIds());
        List<String> itemNames = new ArrayList<>();
        for (Long id : itemIds) {
            Clothing item = clothingById.get(id);
            itemNames.add(item == null ? "已移除单品" : item.getName());
        }
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
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
