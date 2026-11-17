package com.czit.controller;

import com.czit.common.Result;
import com.czit.common.PageResult;
import com.czit.service.CommunityService;
import com.czit.vo.PostVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 社区接口：信息流、发布、点赞、删除。
 */
@RestController
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/posts")
    public Result page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "12") long size) {
        return Result.ok(communityService.page(page, size));
    }

    @PostMapping("/posts")
    public Result publish(@RequestBody CommunityService.PublishRequest request) {
        return Result.ok(communityService.publish(request));
    }

    @PostMapping("/posts/{id}/like")
    public Result like(@PathVariable long id) {
        return Result.ok(communityService.toggleLike(id));
    }

    @DeleteMapping("/posts/{id}")
    public Result delete(@PathVariable long id) {
        communityService.delete(id);
        return Result.ok();
    }

    @GetMapping("/posts/{id}/comments")
    public Result comments(@PathVariable long id) {
        return Result.ok(communityService.comments(id));
    }

    @PostMapping("/posts/{id}/comments")
    public Result addComment(@PathVariable long id,
                             @RequestBody CommunityService.CommentRequest request) {
        return Result.ok(communityService.addComment(id, request));
    }

    @DeleteMapping("/comments/{id}")
    public Result deleteComment(@PathVariable long id) {
        communityService.deleteComment(id);
        return Result.ok();
    }
}