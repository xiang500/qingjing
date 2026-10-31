package com.qingjing.common;

import java.util.Arrays;
import java.util.Optional;

/**
 * 衣物分类枚举。前后端共用的稳定编码，中文名只用于展示。
 */
public enum ClothingCategory {

    TOP("上装", "#7C9EC3"),
    BOTTOM("下装", "#8FA37E"),
    OUTER("外套", "#C69B7B"),
    DRESS("连衣裙", "#C98A9B"),
    SHOES("鞋履", "#8A8FA3"),
    BAG("包袋", "#C2A56B"),
    ACCESSORY("配饰", "#B79FCB");

    private final String label;
    private final String accentColor;

    ClothingCategory(String label, String accentColor) {
        this.label = label;
        this.accentColor = accentColor;
    }

    public String getLabel() {
        return label;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public static Optional<ClothingCategory> of(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(category -> category.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static String labelOf(String code) {
        return of(code).map(ClothingCategory::getLabel).orElse("其他");
    }
}