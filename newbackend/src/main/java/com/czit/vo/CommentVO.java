package com.czit.vo;

import java.time.LocalDateTime;

/**
 * 评论展示对象：带作者昵称/头像与「是否本人」标记，前端直接渲染。
 */
public record CommentVO(
        Long id,
        Long postId,
        Long userId,
        String content,
        String authorName,
        String authorAvatar,
        boolean mine,
        LocalDateTime createdAt
) {
}
