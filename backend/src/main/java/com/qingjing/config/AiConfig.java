package com.qingjing.config;

import com.qingjing.ai.AiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 相关装配：配置绑定 + 虚拟线程执行器。
 *
 * <p>SSE 的生成任务跑在虚拟线程上，阻塞式的模型调用与工具执行不会占用 Tomcat 工作线程，
 * 并发几十路流式对话也不会把容器线程池打满。</p>
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    @Bean(destroyMethod = "close")
    public ExecutorService aiExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}