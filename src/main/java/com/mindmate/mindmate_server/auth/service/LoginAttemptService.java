package com.mindmate.mindmate_server.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoginAttemptService {
    private final RedisTemplate<String, String> redisTemplate;

    public static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION = 30;

    private static final String ATTEMPTS_PREFIX = "login-attempts:";
    private static final String LOCKED_PREFIX = "login-locked:";

    public void loginFailed(String email) {
        String attemptKey = ATTEMPTS_PREFIX + email;
        String lockedKey = LOCKED_PREFIX + email;

        String attempts = redisTemplate.opsForValue().get(attemptKey);
        int currentAttempts = (attempts != null) ? Integer.parseInt(attempts) : 0;
        currentAttempts++;

        if (currentAttempts >= MAX_ATTEMPTS) {
            redisTemplate.opsForValue().set(
                    lockedKey,
                    "LOCKED",
                    BLOCK_DURATION,
                    TimeUnit.MINUTES
            );
            log.warn("Account locked for email: {}", email);
        }

        redisTemplate.opsForValue().set(
                attemptKey,
                String.valueOf(currentAttempts),
                BLOCK_DURATION,
                TimeUnit.MINUTES
        );
        log.info("Failed login attempt {} for email: {}", currentAttempts, email);
    }

    public void loginSucceeded(String email) {
        String attemptsKey = ATTEMPTS_PREFIX + email;
        String lockedKey = LOCKED_PREFIX + email;
        redisTemplate.delete(attemptsKey);
        redisTemplate.delete(lockedKey);

        log.info("Login succeeded for email: {}", email);
    }

    public boolean isBlocked(String email) {
        String lockedKey = LOCKED_PREFIX + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockedKey));
    }

    public int getCurrentAttempts(String email) {
        String attemptsKey = ATTEMPTS_PREFIX + email;
        String attempts = redisTemplate.opsForValue().get(attemptsKey);
        return Integer.parseInt(attempts);
    }

    public Long getRemainingLockTime(String email) {
        String lockedKey = LOCKED_PREFIX + email;
        return redisTemplate.getExpire(lockedKey, TimeUnit.MINUTES);
    }
}
