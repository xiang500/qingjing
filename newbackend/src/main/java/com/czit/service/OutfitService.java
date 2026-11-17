package com.czit.service;

import com.czit.vo.OutfitVO;

import java.util.List;

/**
 * 穿搭套装业务接口：查询、保存（工作台 / AI 推荐卡）、删除。
 *
 * <p>实现见 {@link com.czit.impl.OutfitServiceImpl}。</p>
 */
public interface OutfitService {

    record SaveOutfitRequest(
            String name,
            List<Long> itemIds,
            String occasion,
            String season,
            String reason,
            Integer score) {
    }

    List<OutfitVO> list();

    /** 保存一套搭配：只允许使用自己衣橱里的单品。 */
    OutfitVO create(SaveOutfitRequest request);

    /** 删除自己的套装（逻辑删除）。 */
    void delete(long outfitId);
}
