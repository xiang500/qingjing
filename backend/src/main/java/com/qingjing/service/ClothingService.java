package com.qingjing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qingjing.common.BizException;
import com.qingjing.common.ClothingCategory;
import com.qingjing.common.ErrorCode;
import com.qingjing.common.JsonUtils;
import com.qingjing.common.PageResult;
import com.qingjing.common.UserContext;
import com.qingjing.dto.ClothingQuery;
import com.qingjing.dto.ClothingSaveRequest;
import com.qingjing.entity.Clothing;
import com.qingjing.mapper.ClothingMapper;
import com.qingjing.vo.ClothingVO;
import com.qingjing.vo.WardrobeStatsVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 衣橱单品业务：分页检索、增删改、统计。
 *
 * <p>所有查询强制带 user_id 条件，避免越权访问他人衣橱。</p>
 */
@Service
public class ClothingService {

    private final ClothingMapper clothingMapper;

    public ClothingService(ClothingMapper clothingMapper) {
        this.clothingMapper = clothingMapper;
    }

    public PageResult<ClothingVO> page(ClothingQuery query) {
        long userId = UserContext.requireUserId();
        LambdaQueryWrapper<Clothing> wrapper = new LambdaQueryWrapper<Clothing>()
                .eq(Clothing::getUserId, userId);

        if (query.getCategory() != null && !query.getCategory().isBlank()) {
            ClothingCategory category = ClothingCategory.of(query.getCategory())
                    .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, "未知的衣物分类: " + query.getCategory()));
            wrapper.eq(Clothing::getCategory, category.name());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String keyword = query.getKeyword().trim();
            wrapper.and(inner -> inner.like(Clothing::getName, keyword)
                    .or().like(Clothing::getBrand, keyword)
                    .or().like(Clothing::getMaterial, keyword)
                    .or().like(Clothing::getColor, keyword));
        }
        if (query.getFavorite() != null) {
            wrapper.eq(Clothing::getFavorite, query.getFavorite());
        }
        if (query.getSeason() != null && !query.getSeason().isBlank()) {
            wrapper.like(Clothing::getSeason, query.getSeason().trim().toUpperCase());
        }
        applySort(wrapper, query.getSort());

        Page<Clothing> page = clothingMapper.selectPage(
                new Page<>(Math.max(1, query.getPage()), clampSize(query.getSize())), wrapper);

        List<ClothingVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public ClothingVO detail(long id) {
        return toVO(requireOwned(id));
    }

    @Transactional
    public ClothingVO create(ClothingSaveRequest request) {
        Clothing entity = new Clothing();
        entity.setUserId(UserContext.requireUserId());
        apply(entity, request);
        entity.setWearCount(0);
        entity.setFavorite(Boolean.TRUE.equals(request.favorite()));
        entity.setDeleted(0);
        clothingMapper.insert(entity);
        return toVO(requireOwned(entity.getId()));
    }

    @Transactional
    public ClothingVO update(long id, ClothingSaveRequest request) {
        Clothing existing = requireOwned(id);
        apply(existing, request);
        existing.setFavorite(Boolean.TRUE.equals(request.favorite()));
        clothingMapper.updateById(existing);
        return toVO(requireOwned(id));
    }

    @Transactional
    public ClothingVO toggleFavorite(long id) {
        Clothing existing = requireOwned(id);
        existing.setFavorite(!Boolean.TRUE.equals(existing.getFavorite()));
        clothingMapper.updateById(existing);
        return toVO(existing);
    }

    /** 记录一次穿着：穿着次数 +1，最近穿着日期置为今天。 */
    @Transactional
    public ClothingVO markWorn(long id) {
        Clothing existing = requireOwned(id);
        int wearCount = existing.getWearCount() == null ? 0 : existing.getWearCount();
        existing.setWearCount(wearCount + 1);
        existing.setLastWornAt(java.time.LocalDate.now());
        clothingMapper.updateById(existing);
        return toVO(existing);
    }

    @Transactional
    public void remove(long id) {
        requireOwned(id);
        clothingMapper.deleteById(id);
    }

    public WardrobeStatsVO stats() {
        long userId = UserContext.requireUserId();
        List<Clothing> all = clothingMapper.selectList(new LambdaQueryWrapper<Clothing>()
                .eq(Clothing::getUserId, userId));

        Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        for (ClothingCategory category : ClothingCategory.values()) {
            categoryCounts.put(category.getLabel(), 0);
        }

        int favoriteCount = 0;
        int unwornCount = 0;
        BigDecimal totalValue = BigDecimal.ZERO;
        int wearSum = 0;

        for (Clothing item : all) {
            String label = ClothingCategory.labelOf(item.getCategory());
            categoryCounts.merge(label, 1, Integer::sum);
            if (Boolean.TRUE.equals(item.getFavorite())) {
                favoriteCount++;
            }
            int wearCount = item.getWearCount() == null ? 0 : item.getWearCount();
            if (wearCount == 0) {
                unwornCount++;
            }
            wearSum += wearCount;
            if (item.getPrice() != null) {
                totalValue = totalValue.add(item.getPrice());
            }
        }

        List<String> topWornNames = all.stream()
                .sorted(Comparator.comparingInt((Clothing c) -> c.getWearCount() == null ? 0 : c.getWearCount()).reversed())
                .limit(3)
                .map(Clothing::getName)
                .toList();

        double avgWear = all.isEmpty() ? 0d : BigDecimal.valueOf(wearSum)
                .divide(BigDecimal.valueOf(all.size()), 1, RoundingMode.HALF_UP)
                .doubleValue();

        return new WardrobeStatsVO(
                all.size(),
                favoriteCount,
                unwornCount,
                categoryCounts.size(),
                totalValue.setScale(2, RoundingMode.HALF_UP),
                avgWear,
                categoryCounts,
                topWornNames);
    }

    private Clothing requireOwned(long id) {
        Clothing entity = clothingMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "单品不存在或已删除");
        }
        if (!entity.getUserId().equals(UserContext.requireUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "单品不存在或已删除");
        }
        return entity;
    }

    private void apply(Clothing entity, ClothingSaveRequest request) {
        entity.setName(request.name().trim());
        entity.setCategory(ClothingCategory.of(request.category())
                .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, "未知的衣物分类"))
                .name());
        entity.setSubCategory(request.subCategory());
        entity.setColor(request.color());
        entity.setColorHex(request.colorHex());
        entity.setSeason(JsonUtils.joinCsv(request.season()));
        entity.setStyleTags(JsonUtils.write(request.styleTags() == null ? List.of() : request.styleTags()));
        entity.setMaterial(request.material());
        entity.setBrand(request.brand());
        entity.setImageUrl(request.imageUrl());
        entity.setModelUrl(request.modelUrl());
        entity.setPrice(request.price());
        entity.setPurchaseDate(request.purchaseDate());
        entity.setNote(request.note());
    }

    private void applySort(LambdaQueryWrapper<Clothing> wrapper, String sort) {
        String key = sort == null ? "latest" : sort;
        switch (key) {
            case "wearCount" -> wrapper.orderByDesc(Clothing::getWearCount);
            case "priceAsc" -> wrapper.orderByAsc(Clothing::getPrice);
            case "priceDesc" -> wrapper.orderByDesc(Clothing::getPrice);
            default -> wrapper.orderByDesc(Clothing::getCreatedAt).orderByDesc(Clothing::getId);
        }
    }

    private long clampSize(long size) {
        if (size <= 0) {
            return 12;
        }
        return Math.min(size, 100);
    }

    private ClothingVO toVO(Clothing entity) {
        return new ClothingVO(
                entity.getId(),
                entity.getName(),
                entity.getCategory(),
                ClothingCategory.labelOf(entity.getCategory()),
                ClothingCategory.of(entity.getCategory()).map(ClothingCategory::getAccentColor).orElse("#9AA5B1"),
                entity.getSubCategory(),
                entity.getColor(),
                entity.getColorHex(),
                JsonUtils.splitCsv(entity.getSeason()),
                JsonUtils.parseStringList(entity.getStyleTags()),
                entity.getMaterial(),
                entity.getBrand(),
                entity.getImageUrl(),
                entity.getModelUrl(),
                entity.getPrice(),
                entity.getPurchaseDate(),
                entity.getWearCount(),
                entity.getLastWornAt(),
                entity.getFavorite(),
                entity.getNote(),
                entity.getCreatedAt());
    }

    /** 供 AI Agent / 换装渲染按 ID 批量取单品。 */
    public List<Clothing> findOwnedByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return clothingMapper.selectList(new LambdaQueryWrapper<Clothing>()
                .eq(Clothing::getUserId, UserContext.requireUserId())
                .in(Clothing::getId, ids));
    }
}