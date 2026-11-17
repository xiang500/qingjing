package com.czit.controller;

import com.czit.common.Result;
import com.czit.common.UserContext;
import com.czit.service.KnowledgeService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库接口：用户端只读启用的片段，管理端可增删改。
 * 管理端（/api/admin/knowledge）仅 ADMIN 可访问。
 */
@RestController
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/api/knowledge")
    public Result listEnabled() {
        return Result.ok(knowledgeService.listEnabled());
    }

    @GetMapping("/api/admin/knowledge")
    public Result listAll() {
        UserContext.requireAdmin();
        return Result.ok(knowledgeService.listAll());
    }

    @PostMapping("/api/admin/knowledge")
    public Result create(@RequestBody KnowledgeService.SaveRequest request) {
        UserContext.requireAdmin();
        return Result.ok(knowledgeService.create(request));
    }

    @PutMapping("/api/admin/knowledge/{id}")
    public Result update(@PathVariable long id, @RequestBody KnowledgeService.SaveRequest request) {
        UserContext.requireAdmin();
        return Result.ok(knowledgeService.update(id, request));
    }

    @DeleteMapping("/api/admin/knowledge/{id}")
    public Result delete(@PathVariable long id) {
        UserContext.requireAdmin();
        knowledgeService.delete(id);
        return Result.ok();
    }
}
