package com.qingjing.auth;

import com.qingjing.common.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 从 Authorization: Bearer 中解析用户，写入 UserContext。
 *
 * <p>没有携带 token 时不拦截，交由后续的 X-User-Id 兼容逻辑处理，
 * 保证阶段一、二的演示调用方式仍然可用。</p>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Long userId = jwtService.parseUserId(header.substring(7).trim());
            if (userId != null) {
                UserContext.set(userId);
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 事务/异步线程之外统一清理，避免线程复用导致的身份串号
            UserContext.clear();
        }
    }
}