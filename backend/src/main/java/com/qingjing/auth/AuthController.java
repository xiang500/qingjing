package com.qingjing.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingjing.common.ApiResult;
import com.qingjing.common.BizException;
import com.qingjing.common.ErrorCode;
import com.qingjing.common.JsonUtils;
import com.qingjing.common.UserContext;
import com.qingjing.entity.User;
import com.qingjing.mapper.UserMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 账号与鉴权：登录、注册、当前用户资料。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(UserMapper userMapper, JwtService jwtService, BCryptPasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public record LoginRequest(
            @NotBlank(message = "不能为空") String username,
            @NotBlank(message = "不能为空") String password) {
    }

    public record RegisterRequest(
            @NotBlank(message = "不能为空")
            @Size(min = 3, max = 20, message = "长度需在 3-20 之间") String username,
            @NotBlank(message = "不能为空")
            @Size(min = 6, max = 32, message = "长度需在 6-32 之间") String password,
            @Size(max = 32, message = "长度不能超过 32") String nickname) {
    }

    public record Profile(Long id, String username, String nickname, String avatarUrl, Integer gender,
                          BigDecimal heightCm, BigDecimal weightKg, String bodyType,
                          List<String> stylePreference) {
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username().trim()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号或密码不正确");
        }
        return ApiResult.ok(session(user));
    }

    @PostMapping("/register")
    public ApiResult<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        String username = request.username().trim();
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exists != null && exists > 0) {
            throw new BizException("这个用户名已经被注册了");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname() == null || request.nickname().isBlank()
                ? username : request.nickname().trim());
        user.setGender(0);
        user.setDeleted(0);
        userMapper.insert(user);
        return ApiResult.ok(session(user));
    }

    @GetMapping("/me")
    public ApiResult<Profile> me() {
        User user = userMapper.selectById(UserContext.requireUserId());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return ApiResult.ok(profile(user));
    }

    private Map<String, Object> session(User user) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", jwtService.issue(user.getId(), user.getUsername()));
        body.put("expiresIn", jwtService.ttlSeconds());
        body.put("user", profile(user));
        return body;
    }

    private Profile profile(User user) {
        return new Profile(user.getId(), user.getUsername(), user.getNickname(), user.getAvatarUrl(),
                user.getGender(), user.getHeightCm(), user.getWeightKg(), user.getBodyType(),
                JsonUtils.parseStringList(user.getStylePreference()));
    }
}