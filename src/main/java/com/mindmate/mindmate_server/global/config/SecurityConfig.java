package com.mindmate.mindmate_server.global.config;

import com.mindmate.mindmate_server.auth.service.OAuth2UserService;
import com.mindmate.mindmate_server.auth.service.TokenService;
import com.mindmate.mindmate_server.auth.util.JwtAuthenticationFilter;
import com.mindmate.mindmate_server.auth.util.JwtTokenProvider;
import com.mindmate.mindmate_server.auth.util.OAuth2AuthenticationSuccessHandler;
import com.mindmate.mindmate_server.global.util.RateLimitFilter;
import com.mindmate.mindmate_server.global.util.RequestLoggingFilter;
import com.mindmate.mindmate_server.global.util.XssFilter;
import com.mindmate.mindmate_server.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final UserService userService;
    private final OAuth2UserService oAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // csrf 보호 비활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // cors 설정
                .headers(headers -> headers // 보안 헤더 설정
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny) // 클릭재킹 방지
                        .xssProtection(Customizer.withDefaults()) // XSS 보호
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://www.gstatic.com; " + // Firebase SDK 로딩 허용
                                                "style-src 'self' 'unsafe-inline'; " +
                                                "img-src 'self' data: blob: http: https:; " +
                                                "font-src 'self' data:; " +
                                                "connect-src 'self' ws: wss: https://fcm.googleapis.com https://fcmregistrations.googleapis.com https://firebaseinstallations.googleapis.com; " + // Firebase API 허용
                                                "worker-src 'self' blob:;" // 서비스 워커 허용
                                )
                        )
                        .httpStrictTransportSecurity(hsts -> // HTTPS 강제
                                hsts.includeSubDomains(true)
                                        .maxAgeInSeconds(31536000)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 관리 STATELESS하게 (JWT 사용)
                .exceptionHandling(exception -> exception // 예외 처리
                        .authenticationEntryPoint(customAuthenticationEntryPoint()) // 인증 실패시
                        .accessDeniedHandler(customAccessDeniedHandler())) // 권한 부족시
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/emoticonImages/**",
                                "/magazineImages/**",
                                "/profileImages/**",
                                "/ws/**",
                                "/auth/*",
                                "/auth/oauth2/redirect",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/v3/api-docs/**",
                                "/profiles/image/default/register",
                                "/oauth2/**"
                        ).permitAll() // 향후 수정 (api 접근, role 별 접근 등)
                        .requestMatchers("/**").hasAnyAuthority("ROLE_USER", "ROLE_PROFILE", "ROLE_ADMIN")
                        .requestMatchers("/profile/**").hasAnyAuthority("ROLE_USER", "ROLE_PROFILE", "ROLE_ADMIN")
                        .requestMatchers("/chat/**", "/ws/**").hasAnyAuthority("ROLE_PROFILE", "ROLE_ADMIN")
//        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN") 나중에 admin 접근 제한 시 사용
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint.baseUri("/oauth2/authorize"))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/oauth2/callback/google"))
                        .userInfoEndpoint(endpoint -> endpoint.userService(oAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler))
                .addFilterBefore(new RateLimitFilter(), UsernamePasswordAuthenticationFilter.class) // 초당 10개 요청 제한
                .addFilterBefore(new XssFilter(), UsernamePasswordAuthenticationFilter.class) // XSS 공격 방지
                .addFilterBefore(new RequestLoggingFilter(), UsernamePasswordAuthenticationFilter.class) // 모든 요청 로깅
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class); // JWT 토큰 검증
        return http.build();
    }

    /**
     * cors 허용 설정
     */
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:8080",
                "http://localhost:63342",
                "ws://localhost:8080",
                "wss://localhost:8080",
                "https://mindmate.shop/"
        ));
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "Sec-WebSocket-Protocol",
                "Sec-WebSocket-Version",
                "Sec-WebSocket-Key"
        ));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json:charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"인증 실패\", \"message\": \"" + authException.getMessage() + "\"}");
        };
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"접근 거부\", \"message\": \""
                    + accessDeniedException.getMessage() + "\"}");
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userService, tokenService);
    }

}
