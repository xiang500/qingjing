package com.qingjing.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingjing.ai.tool.AgentTool;
import com.qingjing.ai.tool.AgentToolRegistry;
import com.qingjing.common.ClothingCategory;
import com.qingjing.common.JsonUtils;
import com.qingjing.entity.Clothing;
import com.qingjing.entity.User;
import com.qingjing.mapper.ClothingMapper;
import com.qingjing.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 穿搭 Agent 编排：系统提示词（含 RAG 片段与衣橱概况） → 工具调用循环 → 结构化推荐。
 */
@Component
public class OutfitAgent {

    private static final Logger log = LoggerFactory.getLogger(OutfitAgent.class);

    private final LlmRouter llmRouter;
    private final AgentToolRegistry toolRegistry;
    private final OutfitKnowledge knowledge;
    private final ClothingMapper clothingMapper;
    private final UserMapper userMapper;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public OutfitAgent(LlmRouter llmRouter,
                       AgentToolRegistry toolRegistry,
                       OutfitKnowledge knowledge,
                       ClothingMapper clothingMapper,
                       UserMapper userMapper,
                       AiProperties properties,
                       ObjectMapper objectMapper) {
        this.llmRouter = llmRouter;
        this.toolRegistry = toolRegistry;
        this.knowledge = knowledge;
        this.clothingMapper = clothingMapper;
        this.userMapper = userMapper;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public interface Listener {
        void status(String phase, String text);

        void delta(String text);

        void recommendation(Map<String, Object> payload);
    }

    public record Result(String content, Map<String, Object> recommendation, List<String> usedTools) {
    }

    public Result run(long userId, String question, List<Map<String, Object>> history, Listener listener) {
        LlmClient client = llmRouter.active();
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt(userId, question)));
        messages.addAll(history);
        messages.add(Map.of("role", "user", "content", question));

        StringBuilder answer = new StringBuilder();
        Map<String, Object> recommendation = null;
        List<String> usedTools = new ArrayList<>();

        listener.status("thinking", "正在理解你的需求…");

        for (int round = 0; round < properties.getMaxToolRounds(); round++) {
            List<LlmClient.LlmToolCall> calls = new ArrayList<>();
            client.stream(messages, toolRegistry.schemas(), new LlmClient.LlmSink() {
                @Override
                public void text(String delta) {
                    answer.append(delta);
                    listener.delta(delta);
                }

                @Override
                public void toolCall(LlmClient.LlmToolCall call) {
                    calls.add(call);
                }
            });

            if (calls.isEmpty()) {
                break;
            }

            messages.add(assistantToolCallMessage(calls));
            for (LlmClient.LlmToolCall call : calls) {
                usedTools.add(call.name());
                if ("recommend_outfit".equals(call.name())) {
                    recommendation = handleRecommendation(userId, call.arguments(), listener);
                    messages.add(toolMessage(call.id(), "{\"ok\":true}"));
                    continue;
                }
                messages.add(toolMessage(call.id(), executeTool(call, listener)));
            }

            // 已经产出结构化搭配就不再继续要工具，避免模型重复推荐
            if (recommendation != null) {
                break;
            }
        }

        if (answer.length() == 0 && recommendation == null) {
            // 兜底：工具轮次耗尽或模型只调工具不说话时，再要一段纯文本结论
            client.stream(messages, null, new LlmClient.LlmSink() {
                @Override
                public void text(String delta) {
                    answer.append(delta);
                    listener.delta(delta);
                }

                @Override
                public void toolCall(LlmClient.LlmToolCall call) {
                    // ignore
                }
            });
        }

        if (answer.length() == 0) {
            String fallback = "我暂时没能整理出合适的搭配，可以再补充一下场合或风格偏好吗？";
            answer.append(fallback);
            listener.delta(fallback);
        }
        return new Result(answer.toString(), recommendation, usedTools);
    }

    private String executeTool(LlmClient.LlmToolCall call, Listener listener) {
        Optional<AgentTool> tool = toolRegistry.find(call.name());
        if (tool.isEmpty()) {
            return "{\"error\":\"未知工具 " + call.name() + "\"}";
        }
        listener.status(call.name(), statusText(call.name()));
        try {
            Map<String, Object> arguments = parseArguments(call.arguments());
            Object result = tool.get().execute(arguments);
            String json = objectMapper.writeValueAsString(result);
            return json.length() > 6000 ? json.substring(0, 6000) : json;
        } catch (Exception ex) {
            log.warn("工具 {} 执行失败", call.name(), ex);
            return "{\"error\":\"" + ex.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private String statusText(String toolName) {
        return switch (toolName) {
            case "get_weather" -> "正在查询今天天气…";
            case "search_wardrobe" -> "正在翻你的衣橱…";
            case "wardrobe_stats" -> "正在统计衣橱数据…";
            default -> "正在处理…";
        };
    }

    private Map<String, Object> handleRecommendation(long userId, String arguments, Listener listener) {
        listener.status("recommend", "正在整理这套搭配…");
        try {
            Map<String, Object> payload = parseArguments(arguments);
            List<Long> ids = JsonUtils.parseLongList(objectMapper.writeValueAsString(payload.get("itemIds")));
            List<Clothing> items = ids.isEmpty() ? List.of() : clothingMapper.selectBatchIds(ids);
            Map<Long, Clothing> indexed = new HashMap<>();
            items.forEach(item -> indexed.put(item.getId(), item));

            List<Map<String, Object>> itemViews = new ArrayList<>();
            for (Long id : ids) {
                Clothing item = indexed.get(id);
                if (item == null || !userIdEquals(item, userId)) {
                    continue;
                }
                Map<String, Object> view = new LinkedHashMap<>();
                view.put("id", item.getId());
                view.put("name", item.getName());
                view.put("category", item.getCategory());
                view.put("categoryLabel", ClothingCategory.labelOf(item.getCategory()));
                view.put("color", item.getColor());
                view.put("colorHex", item.getColorHex());
                view.put("imageUrl", item.getImageUrl());
                view.put("wearCount", item.getWearCount());
                view.put("favorite", item.getFavorite());
                itemViews.add(view);
            }

            Map<String, Object> recommendation = new LinkedHashMap<>();
            recommendation.put("name", payload.getOrDefault("name", "今日搭配"));
            recommendation.put("occasion", payload.getOrDefault("occasion", "日常"));
            recommendation.put("score", payload.getOrDefault("score", 85));
            recommendation.put("reason", payload.getOrDefault("reason", ""));
            recommendation.put("items", itemViews);
            listener.recommendation(recommendation);
            return recommendation;
        } catch (Exception ex) {
            log.warn("解析推荐结果失败: {}", arguments, ex);
            return null;
        }
    }

    private boolean userIdEquals(Clothing item, long userId) {
        return item.getUserId() != null && item.getUserId() == userId;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String raw) throws Exception {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(raw, Map.class);
    }

    private Map<String, Object> toolMessage(String callId, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "tool");
        message.put("tool_call_id", callId);
        message.put("content", content);
        return message;
    }

    private Map<String, Object> assistantToolCallMessage(List<LlmClient.LlmToolCall> calls) {
        List<Map<String, Object>> toolCalls = calls.stream().map(call -> Map.<String, Object>of(
                "id", call.id(),
                "type", "function",
                "function", Map.of("name", call.name(), "arguments", call.arguments()))).toList();
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "assistant");
        message.put("content", "");
        message.put("tool_calls", toolCalls);
        return message;
    }

    private String systemPrompt(long userId, String question) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是「轻境」（Lightness）的 AI 穿搭顾问，服务对象是注重质感、时间有限的都市用户。\n\n");
        prompt.append("工作方式：\n");
        prompt.append("1. 缺信息先查：天气用 get_weather；有哪些衣服必须用 search_wardrobe 查真实数据，绝不编造单品或 ID。\n");
        prompt.append("2. 判断衣橱结构、久未穿着、花费时用 wardrobe_stats。\n");
        prompt.append("3. 推荐时优先选久未穿着或低频单品，让旧衣重新上场，并说明取舍。\n");
        prompt.append("4. 最终必须调用 recommend_outfit 输出结构化搭配，itemIds 只能来自检索结果，按上装→下装→鞋履→配饰排序。\n\n");
        prompt.append("表达要求：中文；先给结论再给理由；180-320 字，用空行分成 2-3 段；");
        prompt.append("语气克制具体，不用「亲」「宝子」，不堆砌形容词；必须回应天气、场合、体型等已知条件。\n\n");

        User user = userMapper.selectById(userId);
        if (user != null) {
            prompt.append("用户档案：身高 ").append(user.getHeightCm()).append("cm，体重 ")
                    .append(user.getWeightKg()).append("kg，体型 ")
                    .append(user.getBodyType() == null ? "未知" : user.getBodyType())
                    .append(" 型，风格偏好 ")
                    .append(String.join("/", JsonUtils.parseStringList(user.getStylePreference())))
                    .append("。\n");
        }

        List<Clothing> all = clothingMapper.selectList(new LambdaQueryWrapper<Clothing>()
                .eq(Clothing::getUserId, userId));
        if (!all.isEmpty()) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            List<String> unworn = new ArrayList<>();
            for (Clothing item : all) {
                counts.merge(ClothingCategory.labelOf(item.getCategory()), 1, Integer::sum);
                if (item.getWearCount() == null || item.getWearCount() == 0) {
                    unworn.add(item.getName());
                }
            }
            prompt.append("衣橱概况：共 ").append(all.size()).append(" 件；");
            counts.forEach((label, count) -> prompt.append(label).append(" ").append(count).append("、"));
            prompt.setLength(prompt.length() - 1);
            prompt.append("；从未穿着 ").append(unworn.size()).append(" 件");
            if (!unworn.isEmpty()) {
                prompt.append("（").append(String.join("、", unworn)).append("）");
            }
            prompt.append("。\n");
        }

        List<OutfitKnowledge.Chunk> chunks = knowledge.search(question, properties.getKnowledgeTopK());
        if (!chunks.isEmpty()) {
            prompt.append("\n【穿搭知识片段】\n");
            for (OutfitKnowledge.Chunk chunk : chunks) {
                prompt.append("（").append(chunk.title()).append("）").append(chunk.content()).append("\n");
            }
        }
        return prompt.toString();
    }
}