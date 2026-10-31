package com.qingjing.ai;

import java.util.List;
import java.util.Map;

/**
 * 大模型流式通道。DeepSeek 与本地离线引擎实现同一接口，
 * Agent 编排层因此只依赖「文本增量 + 工具调用」两种事件。
 */
public interface LlmClient {

    String name();

    /**
     * 同步消费一次模型输出，边生成边通过 sink 回调。
     *
     * @param messages OpenAI 格式的消息数组（system/user/assistant/tool）
     * @param tools    工具 schema，可为空
     */
    void stream(List<Map<String, Object>> messages, List<Map<String, Object>> tools, LlmSink sink);

    interface LlmSink {
        void text(String delta);

        void toolCall(LlmToolCall call);
    }

    record LlmToolCall(String id, String name, String arguments) {
    }

    class LlmException extends RuntimeException {
        public LlmException(String message) {
            super(message);
        }

        public LlmException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}