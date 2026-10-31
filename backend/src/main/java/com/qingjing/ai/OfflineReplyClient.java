package com.qingjing.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 没有配置 API Key 时的本地策略引擎。
 *
 * <p>它走的是与真实模型完全相同的 Agent 管线：先调用工具（天气 + 检索衣橱），
 * 拿到结果后组织中文文案，再通过 recommend_outfit 产出结构化搭配。
 * 因此演示环境不会因为缺 Key 而「点不动」。</p>
 */
@Component
public class OfflineReplyClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OfflineReplyClient.class);
    private static final List<String> CITIES =
            List.of("北京", "上海", "广州", "深圳", "杭州", "成都", "武汉", "西安", "南京", "重庆");

    private final ObjectMapper objectMapper;

    public OfflineReplyClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "offline-rule-engine";
    }

    @Override
    public void stream(List<Map<String, Object>> messages, List<Map<String, Object>> tools, LlmSink sink) {
        String question = lastUserMessage(messages);
        long toolMessages = messages.stream().filter(message -> "tool".equals(message.get("role"))).count();
        if (toolMessages == 0) {
            String city = detectCity(question);
            sink.toolCall(new LlmToolCall("call_weather", "get_weather", "{\"city\":\"" + city + "\"}"));
            sink.toolCall(new LlmToolCall("call_wardrobe", "search_wardrobe", "{\"limit\":12}"));
            return;
        }

        JsonNode weather = null;
        List<JsonNode> items = new ArrayList<>();
        for (Map<String, Object> message : messages) {
            if (!"tool".equals(message.get("role"))) {
                continue;
            }
            try {
                JsonNode payload = objectMapper.readTree(String.valueOf(message.get("content")));
                if (payload.has("temperature")) {
                    weather = payload;
                }
                JsonNode list = payload.path("items");
                if (list.isArray()) {
                    list.forEach(items::add);
                }
            } catch (Exception ex) {
                log.debug("离线引擎跳过无法解析的工具结果", ex);
            }
        }

        if (items.isEmpty()) {
            streamText("衣橱里还没有可用的单品。先去「数字衣橱」录入几件常穿的衣服，"
                    + "我就能按天气和场合帮你组合出搭配了。", sink);
            return;
        }

        String city = weather == null ? "本地" : weather.path("city").asText("本地");
        int temperature = weather == null ? 22 : weather.path("temperature").asInt(22);
        String condition = weather == null ? "多云" : weather.path("condition").asText("多云");
        String occasion = detectOccasion(question);

        List<JsonNode> picked = pickOutfit(items, occasion, temperature);
        if (picked.isEmpty()) {
            streamText("衣橱里的单品还不够组成一套完整搭配，先补一件下装或鞋履再来找我吧。", sink);
            return;
        }

        List<Long> ids = picked.stream().map(item -> item.path("id").asLong()).toList();
        String names = picked.stream().map(item -> item.path("name").asText()).reduce((a, b) -> a + " + " + b).orElse("");
        Optional<JsonNode> reborn = picked.stream()
                .filter(item -> item.path("wearCount").asInt(0) <= 3)
                .findFirst();

        StringBuilder text = new StringBuilder();
        text.append("现在").append(city).append(" ").append(temperature).append("℃、").append(condition);
        if (condition.contains("雨")) {
            text.append("，出门记得带伞，鞋子尽量避开翻毛皮");
        }
        text.append("。\n\n");
        text.append("建议这样穿：").append(names).append("。\n\n");
        text.append(occasionLine(occasion, temperature));
        reborn.ifPresent(item -> {
            int wears = item.path("wearCount").asInt(0);
            text.append("其中「").append(item.path("name").asText()).append("」");
            text.append(wears == 0 ? "还一次没穿过" : "只穿过 " + wears + " 次");
            text.append("，这次让它重新上场。");
        });
        text.append(temperature < 18
                ? "早晚有温差，外套别急着收，内搭保持贴身但不紧绷。"
                : "气温舒适，材质尽量选透气的棉麻，版型留一点余量更显松弛。");
        text.append("配饰只留一处亮点，其余交给质感。");

        streamText(text.toString(), sink);

        int score = Math.min(94, 78 + picked.size() * 3 + (reborn.isPresent() ? 2 : 0));
        String reason = occasion + "场景下，" + temperature + "℃ 的" + condition
                + "天气里兼顾了得体与舒适" + (picked.size() >= 4 ? "，层次完整。" : "。");
        String name = picked.get(0).path("name").asText() + "为主角";
        String args = "{\"name\":\"" + escape(name) + "\",\"itemIds\":" + ids
                + ",\"score\":" + score + ",\"reason\":\"" + escape(reason)
                + "\",\"occasion\":\"" + escape(occasion) + "\"}";
        sink.toolCall(new LlmToolCall("call_recommend", "recommend_outfit", args));
    }

    /**
     * 通勤优先上下装，约会优先连衣裙；温度低于 18℃ 才加外套。
     */
    private List<JsonNode> pickOutfit(List<JsonNode> items, String occasion, int temperature) {
        List<JsonNode> picked = new ArrayList<>();
        boolean dateStyle = "约会".equals(occasion) || "聚会".equals(occasion);
        boolean dressFirst = dateStyle && items.stream().anyMatch(item -> "DRESS".equals(item.path("category").asText()));

        if (dressFirst) {
            pick(items, "DRESS").ifPresent(picked::add);
        } else {
            pick(items, "TOP").ifPresent(picked::add);
            Optional<JsonNode> bottom = pick(items, "BOTTOM");
            if (bottom.isPresent()) {
                picked.add(bottom.get());
            } else {
                pick(items, "DRESS").ifPresent(picked::add);
            }
        }
        if (temperature < 18) {
            pick(items, "OUTER").ifPresent(picked::add);
        }
        pick(items, "SHOES").ifPresent(picked::add);
        pick(items, "ACCESSORY").ifPresent(picked::add);
        if (!dateStyle) {
            pick(items, "BAG").ifPresent(picked::add);
        }
        return picked;
    }

    private String occasionLine(String occasion, int temperature) {
        return switch (occasion) {
            case "通勤" -> "通勤场景把露肤度收一收，衬衫配直筒下装最稳，鞋子选能走路的款式。";
            case "约会" -> "约会不用太用力，柔软材质比硬挺面料更有亲和力，颜色可以比通勤亮一档。";
            case "运动" -> "运动场景以舒展和排汗为先，避免有束缚感的硬挺面料。";
            case "居家" -> "居家舒适优先，宽松针织和软底鞋就够，不必追求层次。";
            default -> temperature < 18
                    ? "日常出门按两层来穿，外层挡风、内层锁温，颜色由浅到深过渡。"
                    : "日常出门保持两到三件即可，主色不超过三个，其中两个用中性色压住。";
        };
    }

    private String detectOccasion(String question) {
        if (question == null) {
            return "日常";
        }
        if (question.contains("上班") || question.contains("通勤") || question.contains("会议")
                || question.contains("面试") || question.contains("职场") || question.contains("客户")
                || question.contains("拜访") || question.contains("出差") || question.contains("汇报")
                || question.contains("提案") || question.contains("开会")) {
            return "通勤";
        }
        if (question.contains("约会") || question.contains("聚会") || question.contains("晚餐")
                || question.contains("生日")) {
            return "约会";
        }
        if (question.contains("运动") || question.contains("跑步") || question.contains("健身")) {
            return "运动";
        }
        if (question.contains("居家") || question.contains("在家")) {
            return "居家";
        }
        return "日常";
    }

    private Optional<JsonNode> pick(List<JsonNode> items, String category) {
        return items.stream()
                .filter(item -> category.equals(item.path("category").asText()))
                .min(Comparator.comparingInt(item -> item.path("wearCount").asInt(0)));
    }

    private void streamText(String text, LlmSink sink) {
        int index = 0;
        while (index < text.length()) {
            int end = Math.min(text.length(), index + 3);
            sink.text(text.substring(index, end));
            index = end;
            try {
                Thread.sleep(16);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private String lastUserMessage(List<Map<String, Object>> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).get("role"))) {
                return String.valueOf(messages.get(i).get("content"));
            }
        }
        return "";
    }

    private String detectCity(String text) {
        if (text == null) {
            return "上海";
        }
        return CITIES.stream().filter(text::contains).findFirst().orElse("上海");
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}