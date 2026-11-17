package com.czit.service;

import com.czit.common.PageResult;
import com.czit.vo.CommentVO;
import com.czit.vo.PostVO;

import java.util.List;
import java.util.Map;

/**
 * 社区业务接口：信息流、发布、点赞、评论、删除。
 *
 * <p>帖子可关联一套穿搭；发布时会从套装里解析单品图片，保证前端拿到封面与单品名，
 * 不用前端再补三次请求。</p>
 *
 * <p>实现见 {@link com.czit.impl.CommunityServiceImpl}。</p>
 */
public interface CommunityService {

    record PublishRequest(String title, String content, Long outfitId, List<String> tags, List<String> images) {
    }

    record CommentRequest(String content) {
    }

    PageResult<PostVO> page(long page, long size);

    PostVO publish(PublishRequest request);

    Map<String, Object> toggleLike(long postId);

    void delete(long postId);

    /** 某条帖子下的评论（按时间正序）。 */
    List<CommentVO> comments(long postId);

    /** 发一条评论，并同步帖子的评论数。 */
    CommentVO addComment(long postId, CommentRequest request);

    /** 删除自己的评论，并同步帖子的评论数。 */
    void deleteComment(long commentId);
}
