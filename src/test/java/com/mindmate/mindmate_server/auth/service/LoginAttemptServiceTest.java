package com.mindmate.mindmate_server.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setup() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("로그인 실패 처리 테스트")
    class LoginFailedTest {
        @Test
        @DisplayName("첫 로그인 실패")
        void loginFailed_FirstAttempt() {
            // given
            String email = "test@example.com";
            when(valueOperations.get("login-attempts:" + email)).thenReturn(null);

            // when
            loginAttemptService.loginFailed(email);

            // then
            verify(valueOperations).set(
                    eq("login-attempts:" + email),
                    eq("1"),
                    eq(30L),
                    eq(TimeUnit.MINUTES)
            );
        }

//        @Test
//        @DisplayName("반복되는 로그인 실패")

        @Test
        @DisplayName("최대 시도 횟수 초과")
        void loginFailed_ExceedMaxAttempts() {
            // given
            String email = "test@example.com";
            when(valueOperations.get("login-attempts:" + email)).thenReturn("4");

            // when
            loginAttemptService.loginFailed(email);

            // then
            verify(valueOperations).set(
                    eq("login-locked:" + email),
                    eq("LOCKED"),
                    eq(30L),
                    eq(TimeUnit.MINUTES)
            );
        }
    }

    @Test
    @DisplayName("로그인 성공 시 시도 횟수 초기화")
    void loginSucceeded() {
        // given
        String email = "test@example.com";

        // when
        loginAttemptService.loginSucceeded(email);

        // then
        verify(redisTemplate).delete("login-attempts:" + email);
        verify(redisTemplate).delete("login-locked:" + email);
    }

    @Test
    @DisplayName("계정 잠금 상태 확인")
    void isBlocked() {
        // given
        String email = "test@exmaple.com";
        when(redisTemplate.hasKey("login-locked:" + email)).thenReturn(true);

        // when
        boolean result = loginAttemptService.isBlocked(email);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("현재 로그인 시도 횟수")
    void getCurrentAttempts() {
        // given
        String email = "test@example.com";
        when(valueOperations.get("login-attempts:" + email)).thenReturn("3");

        // when
        int attempts = loginAttemptService.getCurrentAttempts(email);

        // then
        assertEquals(3, attempts);
    }

    @Test
    @DisplayName("계정 잠금 남은 시간 확인")
    void getRemainingLockTime() {
        // given
        String email = "test@example.com";
        when(redisTemplate.getExpire("login-locked:" + email, TimeUnit.MINUTES)).thenReturn(15L);

        // when
        Long remainingTime = loginAttemptService.getRemainingLockTime(email);

        // then
        assertEquals(15L, remainingTime);
    }



}