package com.qingjing.ai;

import org.springframework.stereotype.Component;

/**
 * 通道选择：配置了 API Key 就用真实大模型，否则用本地策略引擎。
 * 两条路径共享同一套 Agent 编排与工具，行为可对比。
 */
@Component
public class LlmRouter {

    private final AiProperties properties;
    private final OpenAiCompatibleClient remoteClient;
    private final OfflineReplyClient offlineClient;

    public LlmRouter(AiProperties properties,
                     OpenAiCompatibleClient remoteClient,
                     OfflineReplyClient offlineClient) {
        this.properties = properties;
        this.remoteClient = remoteClient;
        this.offlineClient = offlineClient;
    }

    public LlmClient active() {
        return properties.hasApiKey() ? remoteClient : offlineClient;
    }

    public boolean isRemote() {
        return properties.hasApiKey();
    }

    public String describe() {
        return isRemote()
                ? "DeepSeek · " + properties.getModel()
                : "本地策略引擎（未配置 DEEPSEEK_API_KEY）";
    }
}