package com.czit.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 文档配置。
 *
 * <p>启动后访问 {@code /swagger-ui.html} 查看可调试的接口文档，
 * {@code /v3/api-docs} 拿到 OpenAPI JSON。</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI qingjingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("轻境 · 后端接口文档")
                .description("AI 实时穿搭与数字衣橱系统的业务接口：衣橱、套装、社区、AI 对话与数据看板")
                .version("0.2.0"));
    }
}
