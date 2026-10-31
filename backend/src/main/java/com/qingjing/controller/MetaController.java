package com.qingjing.controller;

import com.qingjing.common.ApiResult;
import com.qingjing.common.ClothingCategory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 元信息接口：健康检查（含数据库连通性）与分类字典。
 */
@RestController
@RequestMapping("/api")
public class MetaController {

    private final JdbcTemplate jdbcTemplate;

    public MetaController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "qingjing-server");
        body.put("time", LocalDateTime.now().toString());
        try {
            Integer one = jdbcTemplate.queryForObject("select 1", Integer.class);
            body.put("database", Integer.valueOf(1).equals(one) ? "UP" : "DEGRADED");
        } catch (Exception ex) {
            body.put("database", "DOWN");
        }
        body.put("status", "UP");
        return ApiResult.ok(body);
    }

    @GetMapping("/meta/categories")
    public ApiResult<List<Map<String, String>>> categories() {
        return ApiResult.ok(Arrays.stream(ClothingCategory.values())
                .map(category -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("code", category.name());
                    item.put("label", category.getLabel());
                    item.put("accentColor", category.getAccentColor());
                    return item;
                })
                .toList());
    }
}