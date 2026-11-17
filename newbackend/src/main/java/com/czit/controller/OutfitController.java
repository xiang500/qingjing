package com.czit.controller;

import com.czit.common.Result;
import com.czit.service.OutfitService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 穿搭套装接口：查询、保存、删除。
 */
@RestController
@RequestMapping("/api/outfit")
public class OutfitController {

    private final OutfitService outfitService;

    public OutfitController(OutfitService outfitService) {
        this.outfitService = outfitService;
    }

    @GetMapping
    public Result list() {
        return Result.ok(outfitService.list());
    }

    @PostMapping
    public Result create(@RequestBody OutfitService.SaveOutfitRequest request) {
        return Result.ok(outfitService.create(request));
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable long id) {
        outfitService.delete(id);
        return Result.ok();
    }
}
