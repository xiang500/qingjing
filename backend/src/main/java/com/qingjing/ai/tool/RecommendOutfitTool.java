package com.qingjing.ai.tool;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 结构化推荐工具：模型「想清楚」之后必须调用它输出搭配结果。
 *
 * <p>相比让模型在自然语言里塞 JSON，这种 Function Calling 方式更稳：
 * 前端拿到的一定是合法的单品 ID 数组，可以直接驱动换装渲染。</p>
 */
@Component
public class RecommendOutfitTool implements AgentTool {

    @Override
    public String name() {
        return "recommend_outfit";
    }

    @Override
    public String description() {
        return "输出最终推荐的一套搭配。必须在回答末尾调用一次，itemIds 只能来自 search_wardrobe 返回的单品 ID。";
    }

    @Override
    public Map<String, Object> parameters() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("name", Map.of("type", "string", "description", "这套搭配的名称，10 个字以内"));
        properties.put("itemIds", Map.of(
                "type", "array",
                "items", Map.of("type", "integer"),
                "description", "单品 ID 数组，按上装→下装→鞋履→配饰的顺序"));
        properties.put("score", Map.of("type", "integer", "description", "搭配契合度 0-100"));
        properties.put("reason", Map.of("type", "string", "description", "一句话说明为什么这样搭"));
        properties.put("occasion", Map.of("type", "string", "description", "适用场合，例如 通勤、约会、居家"));
        return Map.of("type", "object", "properties", properties,
                "required", List.of("name", "itemIds", "score", "reason"));
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("receivedItemIds", arguments.get("itemIds"));
        return result;
    }
}