package com.czit.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.czit.common.BizException;
import com.czit.common.ErrorCode;
import com.czit.common.JsonUtils;
import com.czit.common.UserContext;
import com.czit.entity.AiRecommendation;
import com.czit.mapper.AiRecommendationMapper;
import com.czit.service.RecommendationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 推荐采纳闭环实现。
 */
@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final AiRecommendationMapper recommendationMapper;

    public RecommendationServiceImpl(AiRecommendationMapper recommendationMapper) {
        this.recommendationMapper = recommendationMapper;
    }

    @Override
    public long record(long userId, Long sessionId, Long messageId, String name, List<Long> itemIds, Integer score) {
        AiRecommendation recommendation = new AiRecommendation();
        recommendation.setUserId(userId);
        recommendation.setSessionId(sessionId);
        recommendation.setMessageId(messageId);
        recommendation.setName(name == null || name.isBlank() ? "AI 搭配" : name.trim());
        recommendation.setItemIds(JsonUtils.write(itemIds == null ? List.of() : itemIds));
        recommendation.setScore(score);
        recommendation.setStatus("SHOWN");
        recommendation.setCreatedAt(LocalDateTime.now());
        recommendation.setDeleted(0);
        recommendationMapper.insert(recommendation);
        return recommendation.getId();
    }

    @Override
    public void adopt(long recommendationId, String action) {
        long userId = UserContext.requireUserId();
        AiRecommendation recommendation = recommendationMapper.selectById(recommendationId);
        if (recommendation == null || recommendation.getUserId() == null || recommendation.getUserId() != userId) {
            throw new BizException(ErrorCode.NOT_FOUND, "推荐记录不存在");
        }
        String normalized = action == null ? "" : action.trim().toUpperCase();
        if (!"TRY_ON".equals(normalized) && !"SAVE".equals(normalized)) {
            throw new BizException("未知的采纳动作");
        }
        // 已经采纳过就不重复计数（一次推荐只算一次）
        if (!"SHOWN".equals(recommendation.getStatus())) {
            return;
        }
        recommendation.setStatus(normalized);
        recommendationMapper.updateById(recommendation);
    }

    @Override
    public AdoptionStats stats(long userId) {
        List<AiRecommendation> all = recommendationMapper.selectList(
                new LambdaQueryWrapper<AiRecommendation>().eq(AiRecommendation::getUserId, userId));
        long shown = all.size();
        long adopted = all.stream().filter(item -> !"SHOWN".equals(item.getStatus())).count();
        double rate = shown == 0 ? 0d : Math.round(adopted * 1000.0 / shown) / 10.0;
        return new AdoptionStats(shown, adopted, rate);
    }
}
