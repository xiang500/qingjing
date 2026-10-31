package com.qingjing.controller;

import com.qingjing.common.ApiResult;
import com.qingjing.service.OutfitService;
import com.qingjing.vo.OutfitVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 穿搭套装接口（阶段一只读）。
 */
@RestController
@RequestMapping("/api/outfit")
public class OutfitController {

    private final OutfitService outfitService;

    public OutfitController(OutfitService outfitService) {
        this.outfitService = outfitService;
    }

    @GetMapping
    public ApiResult<List<OutfitVO>> list() {
        return ApiResult.ok(outfitService.list());
    }
}