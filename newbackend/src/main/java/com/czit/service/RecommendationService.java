package com.czit.service;

import java.util.List;

/**
 * AI 推荐采纳闭环：记录每次推荐曝光，以及用户是否真的试穿 / 保存。
 *
 * <p>实现见 {@link com.czit.impl.RecommendationServiceImpl}。</p>
 */
public interface RecommendationService {

    /** 曝光 / 采纳统计 */
    record AdoptionStats(long shown, long adopted, double rate) {
    }

    /** 记录一次推荐曝光，返回推荐记录 ID（会随 SSE recommendation 事件下发）。 */
    long record(long userId, Long sessionId, Long messageId, String name, List<Long> itemIds, Integer score);

    /** 用户采纳：action = TRY_ON（去试衣镜）或 SAVE（保存套装）。 */
    void adopt(long recommendationId, String action);

    AdoptionStats stats(long userId);
}
