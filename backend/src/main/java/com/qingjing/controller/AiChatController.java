package com.qingjing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingjing.ai.LlmClient;
import com.qingjing.ai.LlmRouter;
import com.qingjing.ai.OutfitAgent;
import com.qingjing.ai.OutfitKnowledge;
import com.qingjing.ai.tool.AgentToolRegistry;
import com.qingjing.common.BizException;
import com.qingjing.common.JsonUtils;
import com.qingjing.common.UserContext;
import com.qingjing.entity.ChatSession;
import com.qingjing.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * AI 穿搭顾问：SSE 流式对话 + 会话历史。
 *
 * <p>事件协议：meta → status* → delta* → recommendation? → done；异常走 error。</p>
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    private final ChatService chatService;
    private final OutfitAgent outfitAgent;
    private final LlmRouter llmRouter;
    private final AgentToolRegistry toolRegistry;
    private final OutfitKnowledge knowledge;
    private final ExecutorService aiExecutor;
    private final ObjectMapper objectMapper;

    public AiChatController(ChatService chatService,
                            OutfitAgent outfitAgent,
                            LlmRouter llmRouter,
                            AgentToolRegistry toolRegistry,
                            OutfitKnowledge knowledge,
                            ExecutorService aiExecutor,
                            ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.outfitAgent = outfitAgent;
        this.llmRouter = llmRouter;
        this.toolRegistry = toolRegistry;
        this.knowledge = knowledge;
        this.aiExecutor = aiExecutor;
        this.objectMapper = objectMapper;
    }

    public record ChatRequest(Long sessionId, String message, String scene) {
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest request) {
        long userId = UserContext.requireUserId();
        if (request.message() == null || request.message().isBlank()) {
            throw new BizException("想说点什么？先输入你的问题吧");
        }
        String question = request.message().trim();
        SseEmitter emitter = new SseEmitter(180_000L);

        aiExecutor.submit(() -> {
            UserContext.set(userId);
            Long[] sessionHolder = new Long[1];
            StringBuilder partial = new StringBuilder();
            try {
                ChatSession session = chatService.ensureSession(userId, request.sessionId(), request.scene(), question);
                sessionHolder[0] = session.getId();
                List<Map<String, Object>> history = chatService.history(session.getId(), 8);
                chatService.append(session.getId(), "user", question, null, null);

                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("sessionId", session.getId());
                meta.put("provider", llmRouter.describe());
                meta.put("tools", toolRegistry.names());
                meta.put("knowledgeChunks", knowledge.size());
                emit(emitter, "meta", meta);

                OutfitAgent.Result result = outfitAgent.run(userId, question, history, new OutfitAgent.Listener() {
                    @Override
                    public void status(String phase, String text) {
                        Map<String, Object> payload = new LinkedHashMap<>();
                        payload.put("phase", phase);
                        payload.put("text", text);
                        emit(emitter, "status", payload);
                    }

                    @Override
                    public void delta(String text) {
                        partial.append(text);
                        Map<String, Object> payload = new LinkedHashMap<>();
                        payload.put("text", text);
                        emit(emitter, "delta", payload);
                    }

                    @Override
                    public void recommendation(Map<String, Object> payload) {
                        emit(emitter, "recommendation", Map.of("recommendation", payload));
                    }
                });

                Long messageId = chatService.append(session.getId(), "assistant", result.content(),
                        JsonUtils.write(result.usedTools()), JsonUtils.write(result.recommendation()));

                Map<String, Object> done = new LinkedHashMap<>();
                done.put("sessionId", session.getId());
                done.put("messageId", messageId);
                done.put("tools", result.usedTools());
                emit(emitter, "done", done);
                emitter.complete();
            } catch (RuntimeException ex) {
                log.warn("流式对话中断: {}", ex.getMessage());
                // 客户端中途断开时，把已经生成的内容落库，历史会话不会只剩半句问话
                if (sessionHolder[0] != null && !partial.isEmpty()) {
                    try {
                        chatService.append(sessionHolder[0], "assistant", partial + "\n\n（本次生成被中断）", null, null);
                    } catch (Exception persistError) {
                        log.warn("中断内容落库失败: {}", persistError.getMessage());
                    }
                }
                emitError(emitter, friendlyMessage(ex));
                emitter.complete();
            } finally {
                UserContext.clear();
            }
        });

        return emitter;
    }

    @GetMapping("/sessions")
    public Map<String, Object> sessions() {
        long userId = UserContext.requireUserId();
        List<Map<String, Object>> list = chatService.sessions(userId).stream().map(session -> {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", session.getId());
            view.put("title", session.getTitle());
            view.put("scene", session.getScene());
            view.put("updatedAt", session.getUpdatedAt());
            return view;
        }).toList();
        return ok(list);
    }

    @GetMapping("/sessions/{id}/messages")
    public Map<String, Object> messages(@PathVariable long id) {
        return ok(chatService.messages(id, UserContext.requireUserId()));
    }

    @DeleteMapping("/sessions/{id}")
    public Map<String, Object> deleteSession(@PathVariable long id) {
        chatService.delete(id, UserContext.requireUserId());
        return ok(null);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("provider", llmRouter.describe());
        body.put("remote", llmRouter.isRemote());
        body.put("tools", toolRegistry.names());
        body.put("knowledgeChunks", knowledge.size());
        return ok(body);
    }

    private void emit(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event)
                    .data(objectMapper.writeValueAsString(data), MediaType.TEXT_PLAIN));
        } catch (IOException | IllegalStateException ex) {
            throw new ClientGoneException();
        }
    }

    private void emitError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error")
                    .data(objectMapper.writeValueAsString(Map.of("message", message)), MediaType.TEXT_PLAIN));
        } catch (Exception ignored) {
            // 客户端已断开，无需再通知
        }
    }

    private String friendlyMessage(RuntimeException ex) {
        if (ex instanceof ClientGoneException) {
            return "客户端已断开";
        }
        if (ex instanceof LlmClient.LlmException) {
            return ex.getMessage() + "（可清空 DEEPSEEK_API_KEY 使用本地策略引擎）";
        }
        return ex.getMessage() == null ? "生成失败，请稍后重试" : ex.getMessage();
    }

    private Map<String, Object> ok(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 0);
        body.put("message", "ok");
        body.put("data", data);
        return body;
    }

    private static class ClientGoneException extends RuntimeException {
        ClientGoneException() {
            super("client gone");
        }
    }
}