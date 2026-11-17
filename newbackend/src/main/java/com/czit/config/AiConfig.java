package com.czit.config;

import com.czit.ai.AiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 相关装配：配置绑定 + SSE 生成任务线程池。
 *
 * <p>项目跑在 Java 17，这里用可回收的守护线程池承载 SSE 生成任务；阻塞式的模型调用与工具执行
 * 不会占用 Tomcat 工作线程。若后续把项目切到 Java 21，只需把这里换成
 * {@code Executors.newVirtualThreadPerTaskExecutor()}，其余代码不用改。</p>
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService aiExecutor() {
        return Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "ai-sse");
            thread.setDaemon(true);
            return thread;
        });
    }
}
