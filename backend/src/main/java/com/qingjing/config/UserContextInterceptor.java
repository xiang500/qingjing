package com.qingjing.config;

import com.qingjing.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 解析用户身份。阶段一只认 X-User-Id；阶段二替换为 JWT 解析，
 * 保持 UserContext 这一出口不变。
 */
public class UserContextInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(UserContextInterceptor.class);
    private static final String HEADER = "X-User-Id";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String raw = request.getHeader(HEADER);
        // JWT 过滤器已经解析出身份时不再覆盖
        if (UserContext.peek() != null) {
            return true;
        }
        if (raw != null && !raw.isBlank()) {
            try {
                UserContext.set(Long.parseLong(raw.trim()));
                return true;
            } catch (NumberFormatException ex) {
                log.warn("非法的 {} 请求头: {}", HEADER, raw);
            }
        }
        UserContext.set(UserContext.DEMO_USER_ID);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        UserContext.clear();
    }
}