package com.czit.service;

import com.czit.vo.KnowledgeVO;

import java.util.List;

/**
 * 穿搭知识库管理（RAG 数据源）。
 *
 * <p>实现见 {@link com.czit.impl.KnowledgeServiceImpl}。</p>
 */
public interface KnowledgeService {

    record SaveRequest(String title, String category, List<String> tags, String content, Boolean enabled) {
    }

    /** 管理端：全部片段（含停用）。 */
    List<KnowledgeVO> listAll();

    /** 用户端 / RAG：仅启用片段。 */
    List<KnowledgeVO> listEnabled();

    KnowledgeVO create(SaveRequest request);

    KnowledgeVO update(long id, SaveRequest request);

    void delete(long id);
}
