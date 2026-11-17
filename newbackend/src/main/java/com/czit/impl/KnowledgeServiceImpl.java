package com.czit.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.czit.ai.OutfitKnowledge;
import com.czit.common.BizException;
import com.czit.common.ErrorCode;
import com.czit.entity.KnowledgeChunk;
import com.czit.mapper.KnowledgeChunkMapper;
import com.czit.service.KnowledgeService;
import com.czit.vo.KnowledgeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 知识库管理实现：写操作后通知 RAG 重新加载内存索引。
 */
@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeChunkMapper knowledgeMapper;
    private final OutfitKnowledge outfitKnowledge;

    public KnowledgeServiceImpl(KnowledgeChunkMapper knowledgeMapper, OutfitKnowledge outfitKnowledge) {
        this.knowledgeMapper = knowledgeMapper;
        this.outfitKnowledge = outfitKnowledge;
    }

    @Override
    public List<KnowledgeVO> listAll() {
        return mapperList(null);
    }

    @Override
    public List<KnowledgeVO> listEnabled() {
        return mapperList(1);
    }

    @Override
    @Transactional
    public KnowledgeVO create(SaveRequest request) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        apply(chunk, request);
        chunk.setDeleted(0);
        knowledgeMapper.insert(chunk);
        outfitKnowledge.refresh();
        return toVO(knowledgeMapper.selectById(chunk.getId()));
    }

    @Override
    @Transactional
    public KnowledgeVO update(long id, SaveRequest request) {
        KnowledgeChunk chunk = knowledgeMapper.selectById(id);
        if (chunk == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "知识片段不存在");
        }
        apply(chunk, request);
        chunk.setUpdatedAt(LocalDateTime.now());
        knowledgeMapper.updateById(chunk);
        outfitKnowledge.refresh();
        return toVO(knowledgeMapper.selectById(id));
    }

    @Override
    @Transactional
    public void delete(long id) {
        KnowledgeChunk chunk = knowledgeMapper.selectById(id);
        if (chunk == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "知识片段不存在");
        }
        knowledgeMapper.deleteById(id);
        outfitKnowledge.refresh();
    }

    private List<KnowledgeVO> mapperList(Integer enabled) {
        LambdaQueryWrapper<KnowledgeChunk> wrapper = new LambdaQueryWrapper<KnowledgeChunk>()
                .orderByDesc(KnowledgeChunk::getId);
        if (enabled != null) {
            wrapper.eq(KnowledgeChunk::getEnabled, enabled);
        }
        return knowledgeMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    private void apply(KnowledgeChunk chunk, SaveRequest request) {
        if (request == null) {
            throw new BizException("参数不能为空");
        }
        String title = request.title() == null ? "" : request.title().trim();
        String content = request.content() == null ? "" : request.content().trim();
        if (title.isEmpty()) {
            throw new BizException("标题不能为空");
        }
        if (title.length() > 128) {
            throw new BizException("标题最多 128 字");
        }
        if (content.isEmpty()) {
            throw new BizException("内容不能为空");
        }
        if (content.length() > 5000) {
            throw new BizException("内容最多 5000 字");
        }
        chunk.setTitle(title);
        chunk.setCategory(request.category() == null || request.category().isBlank()
                ? "通用" : request.category().trim());
        List<String> tags = request.tags() == null ? List.of() : request.tags().stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        chunk.setTags(tags.isEmpty() ? null : String.join(",", tags));
        chunk.setContent(content);
        chunk.setEnabled(Boolean.FALSE.equals(request.enabled()) ? 0 : 1);
    }

    private KnowledgeVO toVO(KnowledgeChunk chunk) {
        List<String> tags = new ArrayList<>();
        if (chunk.getTags() != null && !chunk.getTags().isBlank()) {
            tags = Arrays.stream(chunk.getTags().split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .toList();
        }
        return new KnowledgeVO(
                chunk.getId(),
                chunk.getTitle(),
                chunk.getCategory(),
                tags,
                chunk.getContent(),
                chunk.getEnabled() == null || chunk.getEnabled() == 1,
                chunk.getCreatedAt());
    }
}
