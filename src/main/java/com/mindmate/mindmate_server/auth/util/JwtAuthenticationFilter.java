package com.mindmate.mindmate_server.auth.util;

import com.mindmate.mindmate_server.auth.service.TokenService;
import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.global.exception.AuthErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final TokenService tokenService;

    /**
     * 모든 요청에 대해 JWT 토큰 검증
     * - 토큰 유효성, 블랙리스트 확인
     * - SecurityContext에 인증 정보 설정 -> SecurityUtil로 사용자 정보 얻어내기
     */

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // OAuth2 관련 경로는 JWT 필터 적용 제외
        return path.startsWith("/oauth2/") ||
                path.startsWith("/login/oauth2/") ||
                path.startsWith("/auth/oauth2/") ||
                path.equals("/auth/oauth2/redirect") ||
                path.startsWith("/emoticonImages/") ||
                path.startsWith("/magazineImages/") ||
                path.startsWith("/profileImages/") ||
                path.startsWith("/ws/") ||
                path.startsWith("/auth/") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            // JWT가 없는 경우 그냥 통과
            if (!StringUtils.hasText(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 블랙리스트 확인
            if (tokenService.isTokenBlacklisted(jwt)) {
                handleAuthenticationException(response, "토큰이 블랙리스트에 등록됨");
                return;
            }

            // 토큰 검증 및 인증 설정
            if (tokenProvider.validateToken(jwt)) {
                setAuthentication(jwt, request);
            }

            filterChain.doFilter(request, response);

        } catch (CustomException e) {
            log.error("JWT 커스텀 예외 발생: {}", e.getMessage());
            handleAuthenticationException(response, e.getMessage());
        } catch (Exception e) {
            logger.error("JWT 인증 처리 중 예상치 못한 오류 발생", e);
            // OAuth2 처리 중에는 예외를 던지지 않고 그냥 통과
            filterChain.doFilter(request, response);
        }
    }

    private void setAuthentication(String jwt, HttpServletRequest request) {
        try {
            Long userId = tokenProvider.getUserIdFromToken(jwt);
            User user = userService.findUserById(userId);
            String email = user.getEmail();

            List<GrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority(user.getCurrentRole().getKey())
            );

            UserPrincipal userPrincipal = new UserPrincipal(userId, email, authorities);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userPrincipal, null, authorities);

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.error("인증 정보 설정 중 오류: {}", e.getMessage());
            throw new CustomException(AuthErrorCode.AUTHENTICATION_FAILED);
        }
    }

    private void handleAuthenticationException(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"error\": \"UNAUTHORIZED\", \"message\": \"%s\"}", message));
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
