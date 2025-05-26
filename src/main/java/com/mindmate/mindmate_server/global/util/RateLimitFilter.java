package com.mindmate.mindmate_server.global.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final int TOO_MANY_REQUESTS = 429;

    private final LoadingCache<String, RateLimiter> rateLimiters = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<>() {
                @Override
                public RateLimiter load(String key) {
                    return RateLimiter.create(40.0);
                }
            });

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // ip 기반 또는 사용자별로 구분하여 api 호출 제한
        String clientId = request.getRemoteAddr();

        try {
            RateLimiter rateLimiter = rateLimiters.get(clientId);
            if (!rateLimiter.tryAcquire()) {
                response.setStatus(TOO_MANY_REQUESTS);
                response.getWriter().write("Rate limit exceeded. Try again later.");
                return;
            }
            filterChain.doFilter(request, response);
        } catch (ExecutionException e) {
            // 캐시에서 RateLimiter를 가져오는 데 실패한 경우
            logger.error("Failed to get rate limiter", e);
            filterChain.doFilter(request, response);
        }
    }
}

