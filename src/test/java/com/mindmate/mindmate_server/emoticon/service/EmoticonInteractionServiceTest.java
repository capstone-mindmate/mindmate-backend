package com.mindmate.mindmate_server.emoticon.service;

import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmoticonInteractionServiceTest {
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private RedisKeyManager redisKeyManager;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private ZSetOperations<String, String> zSetOps;

    @InjectMocks
    private EmoticonInteractionService emoticonInteractionService;

    private static final Long EMOTICON_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final String VIEW_KEY = "emoticon:view:1:100";
    private static final String DAILY_KEY = "emoticon:daily:view:1";
    private static final String USAGE_KEY = "emoticon:usage:1";
    private static final String PURCHASE_KEY = "emoticon:purchase";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
    }

    @Nested
    @DisplayName("조회 수 증가 테스트")
    class IncrementViewCountTest {
        @Test
        @DisplayName("첫 조회 시 조회수 증가")
        void incrementViewCount_FirstView_Success() {
            // given
            when(redisKeyManager.getEmoticonViewCountKey(EMOTICON_ID, USER_ID)).thenReturn(VIEW_KEY);
            when(redisKeyManager.getEmoticonDailyViewKey(EMOTICON_ID)).thenReturn(DAILY_KEY);
            when(valueOps.setIfAbsent(VIEW_KEY, "1", 10, TimeUnit.MINUTES)).thenReturn(true);
            when(valueOps.increment(DAILY_KEY)).thenReturn(1L);

            // when
            emoticonInteractionService.incrementViewCount(EMOTICON_ID, USER_ID);

            // then
            verify(valueOps).setIfAbsent(VIEW_KEY, "1", 10, TimeUnit.MINUTES);
            verify(valueOps).increment(DAILY_KEY);
        }

        @Test
        @DisplayName("중복 조회 시 조회수 증가하지 않음")
        void incrementViewCount_DuplicateView_NoIncrement() {
            // given
            when(redisKeyManager.getEmoticonViewCountKey(EMOTICON_ID, USER_ID)).thenReturn(VIEW_KEY);
            when(valueOps.setIfAbsent(VIEW_KEY, "1", 10, TimeUnit.MINUTES)).thenReturn(false);

            // when
            emoticonInteractionService.incrementViewCount(EMOTICON_ID, USER_ID);

            // then
            verify(valueOps).setIfAbsent(VIEW_KEY, "1", 10, TimeUnit.MINUTES);
            verify(valueOps, never()).increment(anyString());
        }
    }

    @Nested
    @DisplayName("사용량 증가 테스트")
    class IncrementUsageTest {
        @Test
        @DisplayName("이모티콘 사용량 증가")
        void incrementUsage_Success() {
            // given
            when(redisKeyManager.getEmoticonUsageKey(EMOTICON_ID)).thenReturn(USAGE_KEY);
            when(valueOps.increment(USAGE_KEY)).thenReturn(5L);

            // when
            emoticonInteractionService.incrementUsage(EMOTICON_ID);

            // then
            verify(redisKeyManager).getEmoticonUsageKey(EMOTICON_ID);
            verify(valueOps).increment(USAGE_KEY);
        }
    }

    @Nested
    @DisplayName("구매 처리 테스트")
    class HandlePurchaseTest {
        @Test
        @DisplayName("이모티콘 구매 처리")
        void handlePurchase_Success() {
            // given
            when(redisKeyManager.getEmoticonPurchaseKey()).thenReturn(PURCHASE_KEY);
            when(zSetOps.incrementScore(PURCHASE_KEY, EMOTICON_ID.toString(), 1)).thenReturn(3.0);

            // when
            emoticonInteractionService.handlePurchase(EMOTICON_ID);

            // then
            verify(redisKeyManager).getEmoticonPurchaseKey();
            verify(zSetOps).incrementScore(PURCHASE_KEY, EMOTICON_ID.toString(), 1);
        }
    }

}