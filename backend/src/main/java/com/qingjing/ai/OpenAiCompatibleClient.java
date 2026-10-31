package com.qingjing.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * OpenAI Chat Completions 协议的流式实现，默认指向 DeepSeek。
 *
 * <p>只需要替换 base-url 即可切换到通义千问兼容模式、Kimi、Moonshot 等同类服务。</p>
 */
@Component
public class OpenAiCompatibleClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleClient.class);

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String name() {
        return "deepseek:" + properties.getModel();
    }

    @Override
    public void stream(List<Map<String, Object>> messages, List<Map<String, Object>> tools, LlmSink sink) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", properties.getModel());
            body.put("messages", messages);
            body.put("stream", true);
            body.put("temperature", properties.getTemperature());
            body.put("max_tokens", properties.getMaxTokens());
            if (tools != null && !tools.isEmpty()) {
                body.put("tools", tools);
                body.put("tool_choice", "auto");
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimTrailingSlash(properties.getBaseUrl()) + "/chat/completions"))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 300) {
                String detail = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new LlmException("大模型接口返回 " + response.statusCode() + ": " + truncate(detail));
            }

            Map<Integer, ToolCallAccumulator> pendingCalls = new TreeMap<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String payload = line.substring(5).trim();
                    if (payload.isEmpty() || "[DONE]".equals(payload)) {
                        continue;
                    }
                    JsonNode chunk = objectMapper.readTree(payload);
                    JsonNode choices = chunk.path("choices");
                    if (!choices.isArray() || choices.isEmpty()) {
                        continue;
                    }
                    JsonNode delta = choices.get(0).path("delta");
                    String content = delta.path("content").asText("");
                    if (!content.isEmpty()) {
                        sink.text(content);
                    }
                    JsonNode toolCalls = delta.path("tool_calls");
                    if (toolCalls.isArray()) {
                        for (JsonNode call : toolCalls) {
                            int index = call.path("index").asInt(0);
                            ToolCallAccumulator accumulator =
                                    pendingCalls.computeIfAbsent(index, key -> new ToolCallAccumulator());
                            if (call.hasNonNull("id")) {
                                accumulator.id = call.get("id").asText();
                            }
                            JsonNode function = call.path("function");
                            if (function.hasNonNull("name")) {
                                accumulator.name = function.get("name").asText();
                            }
                            if (function.hasNonNull("arguments")) {
                                accumulator.arguments.append(function.get("arguments").asText());
                            }
                        }
                    }
                }
            }

            for (ToolCallAccumulator accumulator : pendingCalls.values()) {
                if (accumulator.name != null && !accumulator.name.isBlank()) {
                    String id = accumulator.id == null ? "call_" + accumulator.name : accumulator.id;
                    sink.toolCall(new LlmToolCall(id, accumulator.name, accumulator.arguments.toString()));
                }
            }
        } catch (LlmException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("调用大模型失败", ex);
            throw new LlmException("调用大模型失败：" + ex.getMessage(), ex);
        }
    }

    private String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String truncate(String text) {
        return text.length() <= 300 ? text : text.substring(0, 300) + "…";
    }

    private static final class ToolCallAccumulator {
        private String id;
        private String name;
        private final StringBuilder arguments = new StringBuilder();
    }
}