package com.czit.controller;

import com.czit.common.Result;
import com.czit.common.PageResult;
import com.czit.dto.ClothingQuery;
import com.czit.dto.ClothingSaveRequest;
import com.czit.service.ClothingService;
import com.czit.vo.ClothingVO;
import com.czit.vo.WardrobeStatsVO;
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
    public Result page(@ModelAttribute ClothingQuery query) {
        return Result.ok(clothingService.page(query));
    }

    /** 衣橱数据看板 */
    @GetMapping("/stats")
    public Result stats() {
        return Result.ok(clothingService.stats());
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable long id) {
        return Result.ok(clothingService.detail(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result create(@Valid @RequestBody ClothingSaveRequest request) {
        return Result.ok(clothingService.create(request));
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable long id, @Valid @RequestBody ClothingSaveRequest request) {
        return Result.ok(clothingService.update(id, request));
    }

    /** 收藏 / 取消收藏 */
    @PatchMapping("/{id}/favorite")
    public Result toggleFavorite(@PathVariable long id) {
        return Result.ok(clothingService.toggleFavorite(id));
    }

    /** 记一次穿着，供 AI 分析衣橱利用率 */
    @PostMapping("/{id}/wear")
    public Result markWorn(@PathVariable long id) {
        return Result.ok(clothingService.markWorn(id));
    }

    @DeleteMapping("/{id}")
    public Result remove(@PathVariable long id) {
        clothingService.remove(id);
        return Result.ok();
    }
}