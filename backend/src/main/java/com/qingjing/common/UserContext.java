package com.qingjing.common;

/**
 * 当前登录用户上下文。
 *
 * <p>阶段一以「演示用户模式」运行：请求头 X-User-Id 缺省时回落到 1 号演示账号。
 * 阶段二接入 JWT 过滤器后，只需把解析结果写入同一 ThreadLocal，
 * Service 层代码无需改动。</p>
 */
public final class UserContext {

    /** 演示账号 ID，与 seed.sql 中的 demo 用户一致。 */
    public static final long DEMO_USER_ID = 1L;

    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId) {
        CURRENT_USER.set(userId);
    }

    public static Long get() {
        Long userId = CURRENT_USER.get();
        return userId == null ? DEMO_USER_ID : userId;
    }

    /** 仅返回已显式设置的身份（JWT 解析结果），不做演示账号回落。 */
    public static Long peek() {
        return CURRENT_USER.get();
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
    }
}