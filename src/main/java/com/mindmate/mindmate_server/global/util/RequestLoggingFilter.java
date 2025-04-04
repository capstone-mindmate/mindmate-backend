package com.mindmate.mindmate_server.global.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import org.slf4j.Logger;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    // 자세한 로깅 -> 요청 헤더, 클라이언트 IP, 사용자 정보, req-res 시간
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String clientIP = request.getRemoteAddr();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String userAgent = request.getHeader("User-Agent");
        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "anonymous";

        logger.info("Request: {} {} from IP: {}, User: {}, User-Agent: {}",
                method, uri, clientIP, username, userAgent);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Response: {} {} completed in {}ms with status {}",
                    method, uri, duration, response.getStatus());
        }
    }
}
