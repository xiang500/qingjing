package com.qingjing.controller;

import com.qingjing.common.ApiResult;
import com.qingjing.common.PageResult;
import com.qingjing.service.CommunityService;
import com.qingjing.vo.PostVO;
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
    public ApiResult<PageResult<PostVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "12") long size) {
        return ApiResult.ok(communityService.page(page, size));
    }

    @PostMapping("/posts")
    public ApiResult<PostVO> publish(@RequestBody CommunityService.PublishRequest request) {
        return ApiResult.ok(communityService.publish(request));
    }

    @PostMapping("/posts/{id}/like")
    public ApiResult<Map<String, Object>> like(@PathVariable long id) {
        return ApiResult.ok(communityService.toggleLike(id));
    }

    @DeleteMapping("/posts/{id}")
    public ApiResult<Void> delete(@PathVariable long id) {
        communityService.delete(id);
        return ApiResult.ok();
    }
}