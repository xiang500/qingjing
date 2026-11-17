package com.czit.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.TimeZone;

/**
 * 为业务代码提供 Jackson 2 的 {@link ObjectMapper}。
 *
 * <p>新后端模板是 Spring Boot 4，默认 JSON 实现是 Jackson 3（{@code tools.jackson}），
 * 而轻境的业务/AI 代码写的是 Jackson 2（{@code com.fasterxml.jackson}）。
 * 两者可以共存：这里只补一个业务代码用的 Jackson 2 实例，HTTP 响应仍交给 Boot 4 自带的 Jackson 3。</p>
 *
 * <p>序列化设置与原项目（application.yml 里的 {@code spring.jackson.*}）保持一致：
 * Asia/Shanghai 时区、yyyy-MM-dd HH:mm:ss 日期格式、null 字段不输出、忽略未知字段。</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .modulesToInstall(new JavaTimeModule())
                .timeZone(TimeZone.getTimeZone("Asia/Shanghai"))
                .simpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }
}
