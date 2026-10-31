package com.qingjing.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingjing.ai.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 天气工具：优先调用免 Key 的 Open-Meteo；网络不可用时按月份回退到季节气候值。
 * 真实工具调用是 Agent 的关键得分点——大模型先问天气，再决定推荐外套还是短袖。
 */
@Component
public class WeatherTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(WeatherTool.class);

    /** 城市 → 经纬度 */
    private static final Map<String, double[]> CITIES = Map.ofEntries(
            Map.entry("北京", new double[]{39.90, 116.41}),
            Map.entry("上海", new double[]{31.23, 121.47}),
            Map.entry("广州", new double[]{23.13, 113.26}),
            Map.entry("深圳", new double[]{22.54, 114.06}),
            Map.entry("杭州", new double[]{30.27, 120.16}),
            Map.entry("成都", new double[]{30.57, 104.07}),
            Map.entry("武汉", new double[]{30.59, 114.31}),
            Map.entry("西安", new double[]{34.34, 108.94}),
            Map.entry("南京", new double[]{32.06, 118.80}),
            Map.entry("重庆", new double[]{29.56, 106.55}));

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WeatherTool(ObjectMapper objectMapper, AiProperties properties) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
    }

    @Override
    public String name() {
        return "get_weather";
    }

    @Override
    public String description() {
        return "查询指定城市当前的天气与温度，用于判断今天适合穿什么。";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "city", Map.of(
                                "type", "string",
                                "description", "城市名，例如 上海、北京、杭州")),
                "required", java.util.List.of("city"));
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        String city = String.valueOf(arguments.getOrDefault("city", "上海")).trim();
        double[] coordinate = CITIES.getOrDefault(city, CITIES.get("上海"));
        try {
            String url = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f"
                            + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
                            + "&timezone=Asia%%2FShanghai",
                    coordinate[0], coordinate[1]);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                JsonNode current = objectMapper.readTree(response.body()).path("current");
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("city", city);
                result.put("temperature", current.path("temperature_2m").asInt(22));
                result.put("humidity", current.path("relative_humidity_2m").asInt(60));
                result.put("wind", current.path("wind_speed_10m").asInt(10));
                result.put("condition", conditionOf(current.path("weather_code").asInt(2)));
                result.put("source", "open-meteo");
                result.put("tip", tipOf(current.path("temperature_2m").asInt(22)));
                return result;
            }
            log.warn("天气接口返回 {}，使用季节回退数据", response.statusCode());
        } catch (Exception ex) {
            log.warn("天气接口不可用，使用季节回退数据: {}", ex.getMessage());
        }
        return fallback(city);
    }

    private Map<String, Object> fallback(String city) {
        int month = LocalDate.now().getMonthValue();
        int[] monthly = {5, 7, 11, 17, 22, 26, 30, 30, 26, 21, 15, 9};
        int temperature = monthly[month - 1];
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("city", city);
        result.put("temperature", temperature);
        result.put("humidity", 62);
        result.put("wind", 12);
        result.put("condition", "多云");
        result.put("source", "offline-fallback");
        result.put("tip", tipOf(temperature));
        return result;
    }

    private String tipOf(int temperature) {
        if (temperature <= 8) {
            return "需要厚外套或羽绒服，注意保暖";
        }
        if (temperature <= 17) {
            return "建议叠穿，外套别收";
        }
        if (temperature <= 26) {
            return "单穿长袖刚好，早晚可加薄外套";
        }
        return "注意透气与防晒，选轻薄材质";
    }

    private String conditionOf(int code) {
        if (code == 0) {
            return "晴";
        }
        if (code <= 3) {
            return code == 1 ? "晴间多云" : "多云";
        }
        if (code == 45 || code == 48) {
            return "雾";
        }
        if (code <= 57) {
            return "毛毛雨";
        }
        if (code <= 67) {
            return "雨";
        }
        if (code <= 77) {
            return "雪";
        }
        if (code <= 82) {
            return "阵雨";
        }
        if (code <= 86) {
            return "阵雪";
        }
        return "雷雨";
    }
}