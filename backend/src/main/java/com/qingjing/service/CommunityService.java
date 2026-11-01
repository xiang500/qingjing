package com.qingjing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qingjing.common.BizException;
import com.qingjing.common.ErrorCode;
import com.qingjing.common.JsonUtils;
import com.qingjing.common.PageResult;
import com.qingjing.common.UserContext;
import com.qingjing.entity.Clothing;
import com.qingjing.entity.Outfit;
import com.qingjing.entity.Post;
import com.qingjing.entity.PostLike;
import com.qingjing.entity.User;
import com.qingjing.mapper.ClothingMapper;
import com.qingjing.mapper.OutfitMapper;
import com.qingjing.mapper.PostLikeMapper;
import com.qingjing.mapper.PostMapper;
import com.qingjing.mapper.UserMapper;
import com.qingjing.vo.PostVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 社区：信息流、发布、点赞、删除。
 *
 * <p>帖子可关联一套穿搭；发布时会从套装里解析单品图片，保证前端拿到封面与单品名，
 * 不用前端再补三次请求。</p>
 */
@Service
public class CommunityService {

    private final PostMapper postMapper;
    private final PostLikeMapper postLikeMapper;
    private final UserMapper userMapper;
    private final OutfitMapper outfitMapper;
    private final ClothingMapper clothingMapper;

    public CommunityService(PostMapper postMapper,
                            PostLikeMapper postLikeMapper,
                            UserMapper userMapper,
                            OutfitMapper outfitMapper,
                            ClothingMapper clothingMapper) {
        this.postMapper = postMapper;
        this.postLikeMapper = postLikeMapper;
        this.userMapper = userMapper;
        this.outfitMapper = outfitMapper;
        this.clothingMapper = clothingMapper;
    }

    public record PublishRequest(String title, String content, Long outfitId, List<String> tags, List<String> images) {
    }

    public PageResult<PostVO> page(long page, long size) {
        long userId = UserContext.requireUserId();
        Page<Post> result = postMapper.selectPage(
                new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 50)),
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, 1)
                        .orderByDesc(Post::getCreatedAt)
                        .orderByDesc(Post::getId));

        List<Post> posts = result.getRecords();
        if (posts.isEmpty()) {
            return new PageResult<>(List.of(), result.getTotal(), result.getCurrent(), result.getSize());
        }

        Set<Long> authorIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
        Map<Long, User> authors = new HashMap<>();
        userMapper.selectBatchIds(authorIds).forEach(user -> authors.put(user.getId(), user));

        Set<Long> outfitIds = posts.stream().map(Post::getOutfitId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Outfit> outfits = new HashMap<>();
        if (!outfitIds.isEmpty()) {
            outfitMapper.selectBatchIds(outfitIds).forEach(outfit -> outfits.put(outfit.getId(), outfit));
        }

        Set<Long> clothingIds = outfits.values().stream()
                .flatMap(outfit -> JsonUtils.parseLongList(outfit.getItemIds()).stream())
                .collect(Collectors.toSet());
        Map<Long, Clothing> clothing = new HashMap<>();
        if (!clothingIds.isEmpty()) {
            clothingMapper.selectBatchIds(clothingIds).forEach(item -> clothing.put(item.getId(), item));
        }

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Set<Long> likedIds = postLikeMapper.selectList(new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getUserId, userId)
                        .in(PostLike::getPostId, postIds)).stream()
                .map(PostLike::getPostId)
                .collect(Collectors.toSet());

        List<PostVO> records = posts.stream().map(post -> {
            User author = authors.get(post.getUserId());
            Outfit outfit = post.getOutfitId() == null ? null : outfits.get(post.getOutfitId());
            List<String> itemNames = new ArrayList<>();
            List<String> itemImages = new ArrayList<>();
            List<Long> itemIds = new ArrayList<>();
            if (outfit != null) {
                for (Long id : JsonUtils.parseLongList(outfit.getItemIds())) {
                    Clothing item = clothing.get(id);
                    if (item != null) {
                        itemIds.add(id);
                        itemNames.add(item.getName());
                        if (item.getImageUrl() != null) {
                            itemImages.add(item.getImageUrl());
                        }
                    }
                }
            }
            List<String> images = JsonUtils.parseStringList(post.getImages());
            if (images.isEmpty()) {
                images = itemImages;
            }
            return new PostVO(
                    post.getId(),
                    post.getTitle(),
                    post.getContent(),
                    images,
                    JsonUtils.parseStringList(post.getTags()),
                    post.getLikeCount() == null ? 0 : post.getLikeCount(),
                    post.getCommentCount() == null ? 0 : post.getCommentCount(),
                    likedIds.contains(post.getId()),
                    post.getUserId() == userId,
                    author == null ? "轻境用户" : (author.getNickname() == null || author.getNickname().isBlank()
                            ? author.getUsername() : author.getNickname()),
                    author == null ? null : author.getAvatarUrl(),
                    outfit == null ? null : outfit.getName(),
                    itemIds,
                    itemNames,
                    itemImages,
                    post.getCreatedAt());
        }).toList();

        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public PostVO publish(PublishRequest request) {
        long userId = UserContext.requireUserId();
        if (request.title() == null || request.title().isBlank()) {
            throw new BizException("给这套搭配起个标题吧");
        }
        if (request.content() != null && request.content().length() > 500) {
            throw new BizException("正文最多 500 字");
        }

        List<String> images = request.images() == null ? List.of() : request.images();
        if (request.outfitId() != null) {
            Outfit outfit = outfitMapper.selectById(request.outfitId());
            if (outfit == null || outfit.getUserId() != userId) {
                throw new BizException(ErrorCode.NOT_FOUND, "套装不存在");
            }
            if (images.isEmpty()) {
                List<Long> ids = JsonUtils.parseLongList(outfit.getItemIds());
                if (!ids.isEmpty()) {
                    // selectBatchIds 不保证返回顺序，这里按整身的单品顺序重排
                    Map<Long, Clothing> byId = clothingMapper.selectBatchIds(ids).stream()
                            .collect(Collectors.toMap(Clothing::getId, item -> item, (a, b) -> a));
                    List<String> ordered = new ArrayList<>();
                    if (outfit.getCoverUrl() != null && !outfit.getCoverUrl().isBlank()) {
                        ordered.add(outfit.getCoverUrl());
                    }
                    for (Long id : ids) {
                        Clothing item = byId.get(id);
                        if (item != null && item.getImageUrl() != null && !ordered.contains(item.getImageUrl())) {
                            ordered.add(item.getImageUrl());
                        }
                    }
                    images = ordered;
                }
            }
        }

        Post post = new Post();
        post.setUserId(userId);
        post.setOutfitId(request.outfitId());
        post.setTitle(request.title().trim());
        post.setContent(request.content() == null ? "" : request.content().trim());
        post.setImages(JsonUtils.write(images));
        post.setTags(JsonUtils.write(request.tags() == null ? List.of() : request.tags()));
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setStatus(1);
        post.setDeleted(0);
        postMapper.insert(post);

        return page(1, 1).getRecords().stream()
                .filter(item -> item.id().equals(post.getId()))
                .findFirst()
                .orElseThrow(() -> new BizException("发布成功但读取失败，请刷新"));
    }

    @Transactional
    public Map<String, Object> toggleLike(long postId) {
        long userId = UserContext.requireUserId();
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        PostLike existing = postLikeMapper.selectOne(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId));
        boolean liked;
        if (existing == null) {
            PostLike like = new PostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            postLikeMapper.insert(like);
            liked = true;
        } else {
            postLikeMapper.deleteById(existing.getId());
            liked = false;
        }
        int count = Math.max(0, (post.getLikeCount() == null ? 0 : post.getLikeCount()) + (liked ? 1 : -1));
        post.setLikeCount(count);
        postMapper.updateById(post);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("liked", liked);
        body.put("likeCount", count);
        return body;
    }

    @Transactional
    public void delete(long postId) {
        long userId = UserContext.requireUserId();
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        if (post.getUserId() != userId) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只能删除自己发布的帖子");
        }
        postMapper.deleteById(postId);
    }
}