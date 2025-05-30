package com.mindmate.mindmate_server.emoticon.service;

import com.mindmate.mindmate_server.emoticon.domain.Emoticon;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonResponse;
import com.mindmate.mindmate_server.emoticon.repository.EmoticonRepository;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmoticonPopularityServiceTest {
    @Mock private EmoticonRepository emoticonRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private RedisKeyManager redisKeyManager;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private ZSetOperations<String, String> zSetOps;

    @InjectMocks
    private EmoticonPopularityService emoticonPopularityService;

    private static final String POPULARITY_KEY = "emoticon:popularity";
    private static final String DAILY_VIEW_KEY = "emoticon:daily:view:1";
    private static final String USAGE_KEY = "emoticon:usage:1";
    private static final String PURCHASE_KEY = "emoticon:purchase";

    private Emoticon mockEmoticon;
    private User mockUser;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);

        setupMockEmoticon();
        setupRedisKeys();
    }

    private void setupMockEmoticon() {
        mockUser = mock(User.class);
        Profile mockProfile = mock(Profile.class);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getProfile()).thenReturn(mockProfile);
        when(mockProfile.getNickname()).thenReturn("Creator");

        mockEmoticon = mock(Emoticon.class);
        when(mockEmoticon.getId()).thenReturn(1L);
        when(mockEmoticon.getName()).thenReturn("Test Emoticon");
        when(mockEmoticon.getImageUrl()).thenReturn("test.png");
        when(mockEmoticon.getPrice()).thenReturn(100);
        when(mockEmoticon.isDefault()).thenReturn(false);
        when(mockEmoticon.getCreator()).thenReturn(mockUser);
        when(mockEmoticon.getCreatedAt()).thenReturn(LocalDateTime.now().minusDays(1));
    }

    private void setupRedisKeys() {
        when(redisKeyManager.getEmoticonPopularityKey()).thenReturn(POPULARITY_KEY);
        when(redisKeyManager.getEmoticonDailyViewKey(1L)).thenReturn(DAILY_VIEW_KEY);
        when(redisKeyManager.getEmoticonUsageKey(1L)).thenReturn(USAGE_KEY);
        when(redisKeyManager.getEmoticonPurchaseKey()).thenReturn(PURCHASE_KEY);
    }

    @Nested
    @DisplayName("인기 이모티콘 조회")
    class PopularEmoticonsTest {
        @Test
        @DisplayName("가장 많이 구매된 이모티콘 조회")
        void getMostPurchasedEmoticons_Success() {
            // given
            int limit = 5;
            Set<ZSetOperations.TypedTuple<String>> tuples = createMockTuples();
            when(zSetOps.reverseRangeWithScores(POPULARITY_KEY, 0, limit - 1)).thenReturn(tuples);
            when(emoticonRepository.findAllById(List.of(1L))).thenReturn(List.of(mockEmoticon));

            // when
            List<EmoticonResponse> result = emoticonPopularityService.getMostPurchasedEmoticons(limit);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            verify(zSetOps).reverseRangeWithScores(POPULARITY_KEY, 0, limit - 1);
        }

        @Test
        @DisplayName("가장 많이 조회된 이모티콘 조회")
        void getMostViewedEmoticons_Success() {
            // given
            int limit = 5;
            when(emoticonRepository.findAll()).thenReturn(List.of(mockEmoticon));
            when(valueOps.get(DAILY_VIEW_KEY)).thenReturn("10");

            // when
            List<EmoticonResponse> result = emoticonPopularityService.getMostViewedEmoticons(limit);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            verify(emoticonRepository).findAll();
        }

        @Test
        @DisplayName("가장 많이 사용된 이모티콘 조회 - redis 값이 null")
        void getMostUsedEmoticons_NullRedisValue() {
            // given
            int limit = 5;
            when(emoticonRepository.findAll()).thenReturn(List.of(mockEmoticon));
            when(valueOps.get(PURCHASE_KEY)).thenReturn(null);

            // when
            List<EmoticonResponse> result = emoticonPopularityService.getMostUsedEmoticons(limit);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("전체 인기 이모티콘 조회 - Redis가 null 반환")
        void getOverallPopularEmoticons_RedisReturnsNull() {
            // given
            int limit = 5;
            when(zSetOps.reverseRangeWithScores(POPULARITY_KEY, 0, limit - 1)).thenReturn(null);

            // when
            List<EmoticonResponse> result = emoticonPopularityService.getOverallPopularEmoticons(limit);

            // then
            assertThat(result).isEmpty();
            verify(zSetOps).reverseRangeWithScores(POPULARITY_KEY, 0, limit - 1);
        }

        private Set<ZSetOperations.TypedTuple<String>> createMockTuples() {
            ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
            when(tuple.getValue()).thenReturn("1");
            when(tuple.getScore()).thenReturn(10.0);
            return Set.of(tuple);
        }
    }

    @Nested
    @DisplayName("인기도 점수 계산")
    class PopularityCalculationTest {
        @Test
        @DisplayName("인기도 점수 계산 스케줄러")
        void calculatePopularity_Success() {
            // given
            when(emoticonRepository.findAll()).thenReturn(List.of(mockEmoticon));
            when(valueOps.get(DAILY_VIEW_KEY)).thenReturn("10");
            when(valueOps.get(USAGE_KEY)).thenReturn("5");
            when(zSetOps.score(PURCHASE_KEY, "1")).thenReturn(3.0);

            // when
            emoticonPopularityService.calculatePopularity();

            // then
            verify(emoticonRepository).findAll();
            verify(zSetOps).add(eq(POPULARITY_KEY), eq("1"), anyDouble());
        }

        @ParameterizedTest
        @DisplayName("최신성 점수 계산")
        @CsvSource({
                "0, 1.0",    // 오늘 생성
                "1, 0.98",   // 1일 전
                "10, 0.8",   // 10일 전
                "50, 0.0"    // 50일 전
        })
        void calculateRecency_DifferentDays(int daysOld, double expectedMin) {
            // given
            LocalDateTime createdAt = LocalDateTime.now().minusDays(daysOld);
            when(mockEmoticon.getCreatedAt()).thenReturn(createdAt);
            when(emoticonRepository.findAll()).thenReturn(List.of(mockEmoticon));
            when(valueOps.get(anyString())).thenReturn("0");
            when(zSetOps.score(anyString(), anySet())).thenReturn(0.0);

            // when
            emoticonPopularityService.calculatePopularity();

            // then
            ArgumentCaptor<Double> scoreCaptor = ArgumentCaptor.forClass(Double.class);
            verify(zSetOps).add(eq(POPULARITY_KEY), eq("1"), scoreCaptor.capture());

            Double capturedScore = scoreCaptor.getValue();
            if (daysOld >= 50) {
                assertThat(capturedScore).isGreaterThanOrEqualTo(0.0);
            } else {
                assertThat(capturedScore).isGreaterThan(0.0);
            }
        }
    }
}