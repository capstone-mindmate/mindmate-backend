package com.mindmate.mindmate_server.auth.util;

import com.mindmate.mindmate_server.auth.domain.AuthProvider;
import com.mindmate.mindmate_server.auth.service.TokenService;
import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider tokenProvider;
    private final TokenService tokenService;
    private final UserService userService;

    @Value("${app.oauth2.redirect.uri}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 인증 성공: principal={}", authentication.getPrincipal().getClass().getName());

        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.warn("응답이 이미 커밋되었습니다. {}로 리다이렉트할 수 없습니다.", targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        try {
            Object principal = authentication.getPrincipal();
            User user;

            if (principal instanceof DefaultOidcUser) {
                DefaultOidcUser oidcUser = (DefaultOidcUser) principal;

                String email = oidcUser.getEmail();
                String sub = oidcUser.getAttribute("sub");

                Optional<User> userOptional = userService.findByEmailOptional(email);

                if (userOptional.isPresent()) {
                    user = userOptional.get();
                    log.info("기존 사용자 발견: {}", email);
                } else {
                    log.info("새 사용자 등록: {}", email);
                    user = User.builder()
                            .email(email)
                            .provider(AuthProvider.GOOGLE)
                            .providerId(sub)
                            .role(RoleType.ROLE_USER)
                            .build();
                    user = userService.save(user);
                    log.info("새 사용자 등록 완료: id={}", user.getId());
                }
            } else if (principal instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) principal;
                user = userService.findUserById(userPrincipal.getUserId());
            } else {
                throw new RuntimeException("지원되지 않는 인증 타입입니다.");
            }

            user.updateLastLoginAt();
            userService.save(user);

            String tokenFamily = UUID.randomUUID().toString();
            String accessToken = tokenProvider.generateToken(user);
            String refreshToken = tokenProvider.generateRefreshToken(user, tokenFamily);

            tokenService.saveRefreshToken(user.getId(), refreshToken, tokenFamily);

            String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                    .queryParam("token", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .queryParam("email", user.getEmail())
                    .queryParam("role", user.getCurrentRole().name())
                    .build().toUriString();
            return targetUrl;
        } catch (Exception e) {
            log.error("인증 성공 처리 중 오류 발생", e);
            throw e;
        }
    }
}
