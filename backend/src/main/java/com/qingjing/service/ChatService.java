package com.qingjing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingjing.common.BizException;
import com.qingjing.common.ErrorCode;
import com.qingjing.entity.ChatMessage;
import com.qingjing.entity.ChatSession;
import com.qingjing.mapper.ChatMessageMapper;
import com.qingjing.mapper.ChatSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话会话与消息的读写。历史只回传 user/assistant 的自然语言，
 * 工具调用轨迹与结构化推荐单独存 JSON 列，避免把 prompt 撑爆。
 */
@Service
public class ChatService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    public ChatService(ChatSessionMapper sessionMapper,
                       ChatMessageMapper messageMapper,
                       ObjectMapper objectMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ChatSession ensureSession(long userId, Long sessionId, String scene, String firstMessage) {
        if (sessionId != null) {
            ChatSession existing = sessionMapper.selectById(sessionId);
            if (existing != null && existing.getUserId() == userId) {
                return existing;
            }
        }
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setScene(scene == null || scene.isBlank() ? "WARDROBE" : scene);
        session.setTitle(titleOf(firstMessage));
        session.setDeleted(0);
        sessionMapper.insert(session);
        return session;
    }

    public List<Map<String, Object>> history(long sessionId, int limit) {
        List<ChatMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .in(ChatMessage::getRole, List.of("user", "assistant"))
                .orderByDesc(ChatMessage::getId)
                .last("limit " + Math.max(1, Math.min(20, limit))));
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", message.getRole());
            item.put("content", message.getContent());
            result.add(item);
        }
        return result;
    }

    @Transactional
    public Long append(long sessionId, String role, String content, String toolCalls, String recommendation) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setToolCalls(toolCalls);
        message.setRecommendation(recommendation);
        messageMapper.insert(message);

        ChatSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
        return message.getId();
    }

    public List<ChatSession> sessions(long userId) {
        return sessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdatedAt)
                .last("limit 20"));
    }

    public List<Map<String, Object>> messages(long sessionId, long userId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || session.getUserId() != userId) {
            throw new BizException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        List<ChatMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getId));
        return messages.stream().map(message -> {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", message.getId());
            view.put("role", message.getRole());
            view.put("content", message.getContent());
            view.put("createdAt", message.getCreatedAt());
            if (message.getRecommendation() != null && !message.getRecommendation().isBlank()) {
                try {
                    view.put("recommendation", objectMapper.readValue(message.getRecommendation(), Map.class));
                } catch (Exception ignored) {
                    // 脏数据直接忽略，不影响历史回放
                }
            }
            return view;
        }).toList();
    }

    @Transactional
    public void delete(long sessionId, long userId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || session.getUserId() != userId) {
            throw new BizException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        sessionMapper.deleteById(sessionId);
    }

    private String titleOf(String content) {
        if (content == null || content.isBlank()) {
            return "新的穿搭咨询";
        }
        String trimmed = content.replaceAll("\\s+", " ").trim();
        return trimmed.length() <= 18 ? trimmed : trimmed.substring(0, 18) + "…";
    }
}