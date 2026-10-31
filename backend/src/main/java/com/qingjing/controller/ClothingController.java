package com.qingjing.controller;

import com.qingjing.common.ApiResult;
import com.qingjing.common.PageResult;
import com.qingjing.dto.ClothingQuery;
import com.qingjing.dto.ClothingSaveRequest;
import com.qingjing.service.ClothingService;
import com.qingjing.vo.ClothingVO;
import com.qingjing.vo.WardrobeStatsVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 衣橱单品接口。
 */
@RestController
@RequestMapping("/api/clothing")
public class ClothingController {

    private final ClothingService clothingService;

    public ClothingController(ClothingService clothingService) {
        this.clothingService = clothingService;
    }

    /** 分页检索：?category=TOP&keyword=衬衫&favorite=true&page=1&size=12&sort=latest */
    @GetMapping
    public ApiResult<PageResult<ClothingVO>> page(@ModelAttribute ClothingQuery query) {
        return ApiResult.ok(clothingService.page(query));
    }

    /** 衣橱数据看板 */
    @GetMapping("/stats")
    public ApiResult<WardrobeStatsVO> stats() {
        return ApiResult.ok(clothingService.stats());
    }

    @GetMapping("/{id}")
    public ApiResult<ClothingVO> detail(@PathVariable long id) {
        return ApiResult.ok(clothingService.detail(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<ClothingVO> create(@Valid @RequestBody ClothingSaveRequest request) {
        return ApiResult.ok(clothingService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<ClothingVO> update(@PathVariable long id, @Valid @RequestBody ClothingSaveRequest request) {
        return ApiResult.ok(clothingService.update(id, request));
    }

    /** 收藏 / 取消收藏 */
    @PatchMapping("/{id}/favorite")
    public ApiResult<ClothingVO> toggleFavorite(@PathVariable long id) {
        return ApiResult.ok(clothingService.toggleFavorite(id));
    }

    /** 记一次穿着，供 AI 分析衣橱利用率 */
    @PostMapping("/{id}/wear")
    public ApiResult<ClothingVO> markWorn(@PathVariable long id) {
        return ApiResult.ok(clothingService.markWorn(id));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> remove(@PathVariable long id) {
        clothingService.remove(id);
        return ApiResult.ok();
    }
}