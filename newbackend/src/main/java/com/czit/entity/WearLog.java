package com.czit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 穿着记录（wear_log）。
 *
 * <p>每点一次「今天穿了」就追加一条，所以同一天可能有多条记录；
 * 看板热力图按 worn_date 聚合计数，不做去重。</p>
 */
@TableName("wear_log")
public class WearLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long outfitId;
    private LocalDate wornDate;
    private String weather;
    private String occasion;
    private Integer rating;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOutfitId() {
        return outfitId;
    }

    public void setOutfitId(Long outfitId) {
        this.outfitId = outfitId;
    }

    public LocalDate getWornDate() {
        return wornDate;
    }

    public void setWornDate(LocalDate wornDate) {
        this.wornDate = wornDate;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public String getOccasion() {
        return occasion;
    }

    public void setOccasion(String occasion) {
        this.occasion = occasion;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}