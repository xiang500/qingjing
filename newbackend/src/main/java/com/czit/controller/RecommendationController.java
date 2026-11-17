package com.czit.controller;

import com.czit.common.Result;
import com.czit.service.RecommendationService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 推荐采纳接口：AI 推荐卡上的「试穿 / 保存套装」都会回调这里，
 * 用于计算看板里的推荐采纳率。
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    public record AdoptRequest(String action) {
    }

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/{id}/adopt")
    public Result adopt(@PathVariable long id, @RequestBody(required = false) AdoptRequest request) {
        recommendationService.adopt(id, request == null ? null : request.action());
        return Result.ok();
    }
}
