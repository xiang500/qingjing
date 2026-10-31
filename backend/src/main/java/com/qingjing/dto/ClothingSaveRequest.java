package com.qingjing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 编辑衣橱单品的请求体。
 */
public record ClothingSaveRequest(

        @NotBlank(message = "不能为空")
        @Size(max = 128, message = "长度不能超过 128")
        String name,

        @NotBlank(message = "不能为空")
        @Pattern(regexp = "TOP|BOTTOM|OUTER|DRESS|SHOES|BAG|ACCESSORY", message = "取值不在允许范围内")
        String category,

        @Size(max = 32, message = "长度不能超过 32")
        String subCategory,

        @Size(max = 32, message = "长度不能超过 32")
        String color,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "需为 #RRGGBB 格式")
        String colorHex,

        List<String> season,

        List<String> styleTags,

        @Size(max = 64, message = "长度不能超过 64")
        String material,

        @Size(max = 64, message = "长度不能超过 64")
        String brand,

        @Size(max = 512, message = "长度不能超过 512")
        String imageUrl,

        @Size(max = 512, message = "长度不能超过 512")
        String modelUrl,

        @DecimalMin(value = "0.0", message = "不能为负数")
        BigDecimal price,

        LocalDate purchaseDate,

        Boolean favorite,

        @Size(max = 255, message = "长度不能超过 255")
        String note
) {
}