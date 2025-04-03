package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.matching.domain.MatchingStatus;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisMatchingServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private RedisMatchingService redisMatchingService;
    @Mock
    private User creator;
    @Mock
    private Matching matching;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        when(setOperations.randomMember(anyString())).thenReturn("1");

        // 사용자
        when(creator.getId()).thenReturn(1L);

        // 매칭
        when(matching.getId()).thenReturn(1L);
        when(matching.getCreator()).thenReturn(creator);
        when(matching.getCreatorRole()).thenReturn(InitiatorType.SPEAKER);
        when(matching.getCategory()).thenReturn(MatchingCategory.CAREER);
        when(matching.getStatus()).thenReturn(MatchingStatus.OPEN);
        when(matching.isOpen()).thenReturn(true);
    }

    @Test
    @DisplayName("사용자 활성 매칭 수 증가")
    void incrementUserActiveMatchingCount() {
        // when
        redisMatchingService.incrementUserActiveMatchingCount(1L);

        // then
        verify(redisTemplate.opsForValue()).increment("user:1:activeMatchings");
        verify(redisTemplate).expire("user:1:activeMatchings", 24, TimeUnit.HOURS);
    }

    @Test
    @DisplayName("사용자 활성 매칭 수 감소")
    void decrementUserActiveMatchingCount() {
        // when
        redisMatchingService.decrementUserActiveMatchingCount(1L);

        // then
        verify(redisTemplate.opsForValue()).decrement("user:1:activeMatchings");
    }

    @Test
    @DisplayName("사용자 활성 매칭 수 가져오기")
    void getUserActiveMatchingCount() {
        // given
        given(redisTemplate.opsForValue().get(anyString())).willReturn("2");

        // when
        int count = redisMatchingService.getUserActiveMatchingCount(1L);

        // then
        assertThat(count).isEqualTo(2);
        verify(redisTemplate.opsForValue()).get("user:1:activeMatchings");
    }

    @Test
    @DisplayName("사용자 활성 매칭 수 가져오기 - null 값")
    void getUserActiveMatchingCount_nullValue() {
        // given
        given(redisTemplate.opsForValue().get(anyString())).willReturn(null);

        // when
        int count = redisMatchingService.getUserActiveMatchingCount(1L);

        // then
        assertThat(count).isEqualTo(0);
        verify(redisTemplate.opsForValue()).get("user:1:activeMatchings");
    }

    @Test
    @DisplayName("매칭 키 정리 - 열린 상태")
    void cleanupMatchingKeys_open() {
        // when
        redisMatchingService.cleanupMatchingKeys(matching);

        // then
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("매칭 키 정리 - 닫힌 상태")
    void cleanupMatchingKeys_closed() {
        // given
        when(matching.isOpen()).thenReturn(false);
        when(matching.getStatus()).thenReturn(MatchingStatus.CLOSED);

        // when
        redisMatchingService.cleanupMatchingKeys(matching);

        // then
        verify(setOperations).remove("matching:available:SPEAKER", matching.getId());
    }
}