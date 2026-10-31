package com.qingjing.dto;

/**
 * 衣橱列表查询条件。由 Spring 从 Query String 绑定。
 */
public class ClothingQuery {

    /** 分类编码：TOP/BOTTOM/... 为空表示全部 */
    private String category;
    /** 名称 / 品牌 / 材质 模糊搜索 */
    private String keyword;
    /** 仅看收藏 */
    private Boolean favorite;
    /** 季节过滤：SPRING/SUMMER/AUTUMN/WINTER */
    private String season;
    /** 排序：latest(默认) / wearCount / priceAsc / priceDesc */
    private String sort = "latest";

    private long page = 1;
    private long size = 12;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(Boolean favorite) {
        this.favorite = favorite;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}