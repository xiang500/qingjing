package com.czit.service;

import com.czit.common.PageResult;
import com.czit.dto.ClothingQuery;
import com.czit.dto.ClothingSaveRequest;
import com.czit.entity.Clothing;
import com.czit.vo.ClothingVO;
import com.czit.vo.WardrobeStatsVO;

import java.util.List;

/**
 * 衣橱单品业务接口：分页检索、增删改、统计。
 *
 * <p>所有查询强制带 user_id 条件，避免越权访问他人衣橱。</p>
 *
 * <p>实现见 {@link com.czit.impl.ClothingServiceImpl}。</p>
 */
public interface ClothingService {

    PageResult<ClothingVO> page(ClothingQuery query);

    ClothingVO detail(long id);

    ClothingVO create(ClothingSaveRequest request);

    ClothingVO update(long id, ClothingSaveRequest request);

    ClothingVO toggleFavorite(long id);

    /** 记录一次穿着：穿着次数 +1，最近穿着日期置为今天。 */
    ClothingVO markWorn(long id);

    void remove(long id);

    WardrobeStatsVO stats();

    /** 供 AI Agent / 换装渲染按 ID 批量取单品。 */
    List<Clothing> findOwnedByIds(List<Long> ids);
}
