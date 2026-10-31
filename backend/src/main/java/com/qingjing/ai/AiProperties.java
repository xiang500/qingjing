package com.qingjing.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 通道配置。api-key 为空时自动退化为本地离线策略引擎（OfflineReplyClient），
 * 保证没有 Key 的演示环境也能跑通完整的 Agent 管线。
 */
@ConfigurationProperties(prefix = "qingjing.ai")
public class AiProperties {

    /** DeepSeek（或任意 OpenAI 兼容服务）的 API Key */
    private String apiKey = "";
    /** 兼容 OpenAI Chat Completions 的 base url */
    private String baseUrl = "https://api.deepseek.com";
    private String model = "deepseek-chat";
    private double temperature = 0.7;
    private int maxTokens = 900;
    private int timeoutSeconds = 60;
    /** RAG 注入的知识片段数 */
    private int knowledgeTopK = 3;
    /** 单次对话最多几轮工具调用 */
    private int maxToolRounds = 3;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getKnowledgeTopK() { return knowledgeTopK; }
    public void setKnowledgeTopK(int knowledgeTopK) { this.knowledgeTopK = knowledgeTopK; }
    public int getMaxToolRounds() { return maxToolRounds; }
    public void setMaxToolRounds(int maxToolRounds) { this.maxToolRounds = maxToolRounds; }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}