package com.czit.common;

/**
 * 当前登录用户上下文。
 *
 * <p>阶段一以「演示用户模式」运行：请求头 X-User-Id 缺省时回落到 1 号演示账号。
 * 阶段二接入 JWT 过滤器后，只需把解析结果写入同一 ThreadLocal，
 * Service 层代码无需改动。</p>
 *
 * <p>角色（role）只来自 JWT 解析结果，用于管理端接口鉴权；
 * 通过 X-User-Id 兜底的身份没有角色，视为普通用户。</p>
 */
public final class UserContext {

    /** 演示账号 ID，与 seed.sql 中的 demo 用户一致。 */
    public static final long DEMO_USER_ID = 1L;

    /** 管理员角色标识，与 user.role 字段一致。 */
    public static final String ROLE_ADMIN = "ADMIN";

    /** 普通用户角色标识，user.role 为空时兜底。 */
    public static final String ROLE_USER = "USER";

    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_ROLE = new ThreadLocal<>();

    private UserContext() {
    }

    /** 只写入用户 ID（角色未知），用于异步线程恢复身份等场景。 */
    public static void set(Long userId) {
        set(userId, null);
    }

    /** 写入用户 ID 与角色，JWT 过滤器解析成功后调用。 */
    public static void set(Long userId, String role) {
        CURRENT_USER.set(userId);
        CURRENT_ROLE.set(role);
    }

    public static Long get() {
        Long userId = CURRENT_USER.get();
        return userId == null ? DEMO_USER_ID : userId;
    }

    /** 仅返回已显式设置的身份（JWT 解析结果），不做演示账号回落。 */
    public static Long peek() {
        return CURRENT_USER.get();
    }

    /** 当前请求的角色；未从 JWT 解析到身份时返回 null。 */
    public static String getRole() {
        return CURRENT_ROLE.get();
    }

    public static boolean isAdmin() {
        return ROLE_ADMIN.equals(CURRENT_ROLE.get());
    }

    /** 管理端接口入口校验：非管理员直接抛业务异常，由全局异常处理器转成 40300。 */
    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅管理员可访问");
        }
    }

    public static long requireUserId() {
        Long userId = get();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }
        return userId;
    }

    public static void clear() {
        CURRENT_USER.remove();
        CURRENT_ROLE.remove();
    }
}
