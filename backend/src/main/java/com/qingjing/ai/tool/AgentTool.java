package com.qingjing.ai.tool;

import java.util.Map;

/**
 * Agent 可调用的工具。execute 的返回值会被序列化后回填给大模型。
 */
public interface AgentTool {

    String name();

    String description();

    /** JSON Schema：描述参数 */
    Map<String, Object> parameters();

    Object execute(Map<String, Object> arguments);
}