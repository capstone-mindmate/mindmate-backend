package com.mindmate.mindmate_server.global.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FCMTokenService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String FCM_TOKEN_PREFIX = "FCM:";
    private static final long FCM_TOKEN_EXPIRY = 60 * 24 * 7;

    public void saveUserFCMToken(Long userId, String fcmToken) {
        String key = FCM_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, fcmToken, FCM_TOKEN_EXPIRY, TimeUnit.MINUTES);
        log.info("FCM token saved for user: {}", userId);
    }

    public String getUserFCMToken(Long userId) {
        String key = FCM_TOKEN_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    public void removeUserFCMToken(long userId) {
        String key = FCM_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("FCM token removed for user: {}", userId);
    }
}
