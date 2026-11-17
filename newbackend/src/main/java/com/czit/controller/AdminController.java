package com.czit.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.czit.common.JsonUtils;
import com.czit.common.Result;
import com.czit.common.UserContext;
import com.czit.entity.User;
import com.czit.mapper.AiRecommendationMapper;
import com.czit.mapper.ChatSessionMapper;
import com.czit.mapper.ClothingMapper;
import com.czit.mapper.KnowledgeChunkMapper;
import com.czit.mapper.OutfitMapper;
import com.czit.mapper.PostMapper;
import com.czit.mapper.UserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端只读接口：全局概览与用户列表。所有 /api/admin/** 接口仅 ADMIN 可访问。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    public record AdminOverview(
            long users,
            long clothing,
            long outfits,
            long posts,
            long chatSessions,
            long recommendations,
            long knowledgeChunks) {
    }

    public record AdminUser(
            Long id,
            String username,
            String role,
            String nickname,
            Integer gender,
            BigDecimal heightCm,
            BigDecimal weightKg,
            String bodyType,
            List<String> stylePreference,
            LocalDateTime createdAt) {
    }

    private final UserMapper userMapper;
    private final ClothingMapper clothingMapper;
    private final OutfitMapper outfitMapper;
    private final PostMapper postMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final AiRecommendationMapper recommendationMapper;
    private final KnowledgeChunkMapper knowledgeMapper;

    public AdminController(UserMapper userMapper,
                           ClothingMapper clothingMapper,
                           OutfitMapper outfitMapper,
                           PostMapper postMapper,
                           ChatSessionMapper chatSessionMapper,
                           AiRecommendationMapper recommendationMapper,
                           KnowledgeChunkMapper knowledgeMapper) {
        this.userMapper = userMapper;
        this.clothingMapper = clothingMapper;
        this.outfitMapper = outfitMapper;
        this.postMapper = postMapper;
        this.chatSessionMapper = chatSessionMapper;
        this.recommendationMapper = recommendationMapper;
        this.knowledgeMapper = knowledgeMapper;
    }

    @GetMapping("/overview")
    public Result overview() {
        UserContext.requireAdmin();
        return Result.ok(new AdminOverview(
                userMapper.selectCount(null),
                clothingMapper.selectCount(null),
                outfitMapper.selectCount(null),
                postMapper.selectCount(null),
                chatSessionMapper.selectCount(null),
                recommendationMapper.selectCount(null),
                knowledgeMapper.selectCount(null)));
    }

    @GetMapping("/users")
    public Result users() {
        UserContext.requireAdmin();
        List<AdminUser> list = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .orderByDesc(User::getId)
                        .last("limit 50"))
                .stream()
                .map(user -> new AdminUser(
                        user.getId(),
                        user.getUsername(),
                        roleOf(user),
                        user.getNickname(),
                        user.getGender(),
                        user.getHeightCm(),
                        user.getWeightKg(),
                        user.getBodyType(),
                        JsonUtils.parseStringList(user.getStylePreference()),
                        user.getCreatedAt()))
                .toList();
        return Result.ok(list);
    }

    /** 角色兜底：历史数据或空值时按普通用户处理。 */
    private String roleOf(User user) {
        String role = user.getRole();
        return role == null || role.isBlank() ? UserContext.ROLE_USER : role;
    }
}
