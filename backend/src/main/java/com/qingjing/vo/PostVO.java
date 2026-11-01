package com.qingjing.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区帖子展示对象：聚合作者、穿搭单品与当前用户的点赞状态。
 */
public record PostVO(
        Long id,
        String title,
        String content,
        List<String> images,
        List<String> tags,
        int likeCount,
        int commentCount,
        boolean liked,
        boolean mine,
        String authorName,
        String authorAvatar,
        String outfitName,
        List<Long> itemIds,
        List<String> itemNames,
        List<String> itemImages,
        LocalDateTime createdAt
) {
}