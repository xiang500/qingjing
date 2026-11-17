package com.czit.service;

import com.czit.entity.ChatSession;

import java.util.List;
import java.util.Map;

/**
 * 对话会话与消息的读写接口。
 *
 * <p>历史只回传 user/assistant 的自然语言，工具调用轨迹与结构化推荐单独存 JSON 列，
 * 避免把 prompt 撑爆。</p>
 *
 * <p>实现见 {@link com.czit.impl.ChatServiceImpl}。</p>
 */
public interface ChatService {

    ChatSession ensureSession(long userId, Long sessionId, String scene, String firstMessage);

    List<Map<String, Object>> history(long sessionId, int limit);

    Long append(long sessionId, String role, String content, String toolCalls, String recommendation);

    List<ChatSession> sessions(long userId);

    List<Map<String, Object>> messages(long sessionId, long userId);

    void delete(long sessionId, long userId);

    /** 重命名会话（只允许自己的会话）。 */
    void rename(long sessionId, long userId, String title);
}
