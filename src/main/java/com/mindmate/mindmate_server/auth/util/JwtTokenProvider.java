package com.mindmate.mindmate_server.auth.util;

import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    private final RedisTemplate<String, String> redisTemplate;
//    private final TokenService tokenService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * Refresh token 생성
     * (tokenFamily, tokenType)
     */
    public String generateRefreshToken(User user, String tokenFamily) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("tokenFamily", tokenFamily)
                .claim("tokenType", "REFRESH")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 액세스 토큰 생성
     * (id, role, username, tokenType)
     * 이후 요청마다 헤어(Authorization: Bearer <token>에 JWT 포함하여 전달
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        String username = switch (user.getCurrentRole()) {
            case ROLE_UNVERIFIED, ROLE_USER -> user.getEmail();
            case ROLE_PROFILE, ROLE_SUSPENDED -> user.getProfile().getNickname();
            case ROLE_ADMIN -> "admin:" + user.getEmail();
        };

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getCurrentRole().name())
                .claim("username", username)
                .claim("tokenType", "ACCESS")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    private boolean isProfileComplete(User user) {
        return user.getCurrentRole() != RoleType.ROLE_UNVERIFIED && user.getCurrentRole() != RoleType.ROLE_USER;
    }

    /**
     * 변조 방지
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
//            if (tokenService.isTokenBlacklisted(token)) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey("BL:" + token))) {
                return false;
            }
            Claims claims = getClaims(token);
            String tokenType = claims.get("tokenType", String.class);

            if (tokenType == null) {
                return false;
            }
            return true;
        } catch (SecurityException | MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationFromToken(String token) {
        try {
            Claims claims = getClaims(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();
            return expiration.getTime() - now.getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    public String getTokenFamilyFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("tokenFamily", String.class);
    }

    public String getTokenTypeFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("tokenType", String.class);
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("username", String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
