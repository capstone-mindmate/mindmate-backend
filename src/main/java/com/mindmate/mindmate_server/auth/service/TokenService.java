package com.mindmate.mindmate_server.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmate.mindmate_server.auth.util.JwtTokenProvider;
import com.mindmate.mindmate_server.auth.dto.TokenData;
import com.mindmate.mindmate_server.global.exception.AuthErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String BLACKLIST_PREFIX = "BL:";
    private static final String TOKEN_FAMILY_PREFIX = "TF:";

    public void saveRefreshToken(Long userId, String refreshToken, String tokenFamily) {
        try {
            TokenData tokenData = TokenData.of(refreshToken, tokenFamily);
            String tokenDataJson = objectMapper.writeValueAsString(tokenData);
            redisTemplate.opsForValue()
                    .set(REFRESH_TOKEN_PREFIX + userId, tokenDataJson, 7, TimeUnit.DAYS);
        } catch (JsonProcessingException e) {
            throw new CustomException(AuthErrorCode.TOKEN_SAVE_FAILED);
        }
    }

    public TokenData getRefreshToken(Long userId) {
        try {
            String tokenDataJson = redisTemplate.opsForValue()
                    .get(REFRESH_TOKEN_PREFIX + userId);
            if (tokenDataJson == null) {
                return null;
            }
            return objectMapper.readValue(tokenDataJson, TokenData.class);
        } catch (JsonProcessingException e) {
            throw new CustomException(AuthErrorCode.TOKEN_GET_FAILED);
        }
    }

//    public void invalidateTokenFamily(String tokenFamily) {
//        redisTemplate.delete(TOKEN_FAMILY_PREFIX + tokenFamily);
//    }

    public void invalidateRefreshToken(Long userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }

    public void addToBlackList(String token) {
        log.info("Adding token to blacklist: {}", maskToken(token));

        redisTemplate.opsForValue()
                .set(BLACKLIST_PREFIX + token, "blacklisted",
                        jwtTokenProvider.getExpirationFromToken(token), TimeUnit.MILLISECONDS);
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("BL:" + token));
    }

    private String maskToken(String token) {
        if (token.length() <= 10) return "*".repeat(token.length());

        return token.substring(0, 5)
                + "*".repeat(token.length() - 10)
                + token.substring(token.length() - 5);
    }
}
