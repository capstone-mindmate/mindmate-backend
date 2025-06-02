package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MagazineErrorCode;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineStatus;
import com.mindmate.mindmate_server.magazine.dto.MagazineResponse;
import com.mindmate.mindmate_server.magazine.repository.MagazineRepository;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MagazinePopularityServiceImplTest {
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private RedisKeyManager redisKeyManager;
    @Mock private MagazineRepository magazineRepository;

    @Mock private ValueOperations<String, String> valueOps;
    @Mock private ZSetOperations<String, String> zSetOps;

    @InjectMocks
    private MagazinePopularityServiceImpl magazinePopularityService;

    private Magazine mockMagazine;
    private User mockUser;
    private Profile mockProfile;
    private Long magazineId = 1L;
    private Long userId = 1L;

    @BeforeEach
    void setup() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);

        mockUser = mock(User.class);
        mockProfile = mock(Profile.class);
        when(mockUser.getId()).thenReturn(userId);
        when(mockUser.getProfile()).thenReturn(mockProfile);
        when(mockProfile.getNickname()).thenReturn("testUser");

        mockMagazine = mock(Magazine.class);
        when(mockMagazine.getId()).thenReturn(magazineId);
        when(mockMagazine.getTitle()).thenReturn("Test Magazine");
        when(mockMagazine.getAuthor()).thenReturn(mockUser);
        when(mockMagazine.getLikeCount()).thenReturn(10);
        when(mockMagazine.getMagazineStatus()).thenReturn(MagazineStatus.PUBLISHED);
        when(mockMagazine.getCategory()).thenReturn(MatchingCategory.ACADEMIC);
        when(mockMagazine.getCreatedAt()).thenReturn(LocalDateTime.now().minusDays(2));
        when(mockMagazine.getContents()).thenReturn(new ArrayList<>());

        when(magazineRepository.findById(magazineId)).thenReturn(Optional.of(mockMagazine));
    }

    @Nested
    @DisplayName("조회수 관리 테스트")
    class ViewCountTest {
        @Test
        @DisplayName("첫 조회 시 조회수 증가 성공")
        void incrementViewCount_FirstView_Success() {
            // given
            String viewKey = "user:1:viewed:magazine:1";
            String viewCountKey = "magazine:1:view_count";

            when(redisKeyManager.getMagazineViewedKey(userId, magazineId)).thenReturn(viewKey);
            when(redisKeyManager.getMagazineViewCountKey(magazineId)).thenReturn(viewCountKey);
            when(redisKeyManager.getMagazinePopularityKey()).thenReturn("magazine:popularity");
            when(redisKeyManager.getCategoryPopularityKey("ACADEMIC")).thenReturn("category:ACADEMIC:popularity");
            when(valueOps.setIfAbsent(eq(viewKey), eq("1"), eq(30L), eq(TimeUnit.MINUTES))).thenReturn(true);

            // when
            magazinePopularityService.incrementViewCount(mockMagazine, userId);

            // then
            verify(valueOps).setIfAbsent(eq(viewKey), eq("1"), eq(30L), eq(TimeUnit.MINUTES));
            verify(valueOps).increment(viewCountKey);
            ArgumentCaptor<Double> scoreCaptor = ArgumentCaptor.forClass(Double.class);
            verify(zSetOps).incrementScore(eq("magazine:popularity"), eq(magazineId.toString()), scoreCaptor.capture());
            verify(zSetOps).incrementScore(eq("category:ACADEMIC:popularity"), eq(magazineId.toString()), scoreCaptor.capture());
        }

        @Test
        @DisplayName("재조회 시 조회수 증가하지 않음")
        void incrementViewCount_RepeatedView_NoIncrement() {
            // given
            String viewKey = "user:1:viewed:magazine:1";

            when(redisKeyManager.getMagazineViewedKey(userId, magazineId)).thenReturn(viewKey);
            when(valueOps.setIfAbsent(eq(viewKey), eq("1"), eq(30L), eq(TimeUnit.MINUTES))).thenReturn(false);

            // when
            magazinePopularityService.incrementViewCount(mockMagazine, userId);

            // then
            verify(valueOps).setIfAbsent(eq(viewKey), eq("1"), eq(30L), eq(TimeUnit.MINUTES));
            verify(valueOps, never()).increment(anyString());
            verify(zSetOps, never()).incrementScore(anyString(), anyString(), anyDouble());
        }

        @Test
        @DisplayName("조회수 증가 중 예외 발생 시 로깅만 발생")
        void incrementViewCount_ExceptionHandling() {
            // given
            String viewKey = "user:1:viewed:magazine:1";
            when(redisKeyManager.getMagazineViewedKey(userId, magazineId)).thenReturn(viewKey);
            when(valueOps.setIfAbsent(eq(viewKey), eq("1"), eq(30L), eq(TimeUnit.MINUTES)))
                    .thenThrow(new RuntimeException("Redis connection failed"));

            // when & then
            assertDoesNotThrow(() -> magazinePopularityService.incrementViewCount(mockMagazine, userId));
        }
    }

    @Nested
    @DisplayName("좋아요 점수 테스트")
    class LikeScoreTest {
        @ParameterizedTest(name = "좋아요 {0} 시 점수 {1}")
        @CsvSource({
                "true, 1",   // 좋아요 추가
                "false, -1"  // 좋아요 취소
        })
        void updateLikeScore_Param(boolean isLiked, int sign) {
            // given
            when(redisKeyManager.getMagazinePopularityKey()).thenReturn("magazine:popularity");
            when(redisKeyManager.getCategoryPopularityKey("ACADEMIC")).thenReturn("category:ACADEMIC:popularity");

            // when
            magazinePopularityService.updateLikeScore(mockMagazine, isLiked);

            // then
            ArgumentCaptor<Double> scoreCaptor = ArgumentCaptor.forClass(Double.class);
            verify(zSetOps).incrementScore(eq("magazine:popularity"), eq(magazineId.toString()), scoreCaptor.capture());
            verify(zSetOps).incrementScore(eq("category:ACADEMIC:popularity"), eq(magazineId.toString()), scoreCaptor.capture());

            for (Double score : scoreCaptor.getAllValues()) {
                assertNotNull(score);
                if (sign > 0) {
                    assertTrue(score > 0);
                } else {
                    assertTrue(score < 0);
                }
            }
        }
    }

    @Nested
    @DisplayName("사용자 참여 처리 테스트")
    class EngagementProcessingTest {
        @Test
        @DisplayName("체류 시간과 스크롤 처리 성공")
        void processEngagement_Success() {
            // given
            String dwellTimeKey = "magazine:1:dwell_time";
            long dwellTime = 120000; // 2분
            double scrollPercentage = 85.0;

            when(redisKeyManager.getMagazineDwellTimeKey(magazineId)).thenReturn(dwellTimeKey);
            when(redisKeyManager.getMagazinePopularityKey()).thenReturn("magazine:popularity");
            when(redisKeyManager.getCategoryPopularityKey("ACADEMIC")).thenReturn("category:ACADEMIC:popularity");

            // when
            magazinePopularityService.processEngagement(magazineId, userId, dwellTime, scrollPercentage);

            // then
            verify(zSetOps).add(eq(dwellTimeKey), eq(userId.toString()), eq((double) dwellTime));
            verify(zSetOps).incrementScore(eq("magazine:popularity"), eq(magazineId.toString()), anyDouble());
            verify(zSetOps).incrementScore(eq("category:ACADEMIC:popularity"), eq(magazineId.toString()), anyDouble());
        }

        @Test
        @DisplayName("잘못된 매거진 ID로 참여 처리 실패")
        void processEngagement_InvalidMagazineId_ThrowsException() {
            // given
            Long invalidMagazineId = 999L;
            when(magazineRepository.findById(invalidMagazineId)).thenReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    magazinePopularityService.processEngagement(invalidMagazineId, userId, 120000L, 85.0));
            assertEquals(MagazineErrorCode.MAGAZINE_NOT_FOUND, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("인기 매거진 조회 테스트")
    class PopularMagazinesTest {
        @ParameterizedTest(name = "Redis 데이터 존재: {0}")
        @CsvSource({
                "true,2",   // Redis hit, 2개 반환
                "false,1"   // Redis miss, DB fallback 1개 반환
        })
        void getPopularMagazines_Param(boolean redisHit, int expectedSize) {
            String popularityKey = "magazine:popularity";
            int limit = 10;

            if (redisHit) {
                Set<String> topMagazineIds = new LinkedHashSet<>();
                topMagazineIds.add("1");
                topMagazineIds.add("2");
                when(redisKeyManager.getMagazinePopularityKey()).thenReturn(popularityKey);
                when(zSetOps.reverseRange(popularityKey, 0, limit - 1)).thenReturn(topMagazineIds);

                Magazine magazine1 = mockMagazine;
                Magazine magazine2 = mock(Magazine.class);
                when(magazine2.getId()).thenReturn(2L);
                when(magazine2.getMagazineStatus()).thenReturn(MagazineStatus.PUBLISHED);
                when(magazine2.getAuthor()).thenReturn(mockUser);
                List<Magazine> magazines = Arrays.asList(magazine1, magazine2);
                when(magazineRepository.findAllById(Arrays.asList(1L, 2L))).thenReturn(magazines);
            } else {
                when(redisKeyManager.getMagazinePopularityKey()).thenReturn(popularityKey);
                when(zSetOps.reverseRange(popularityKey, 0, limit - 1)).thenReturn(Collections.emptySet());
                when(magazineRepository.findTop10ByMagazineStatusOrderByLikeCountDesc(MagazineStatus.PUBLISHED))
                        .thenReturn(Collections.singletonList(mockMagazine));
            }

            // when
            List<MagazineResponse> result = magazinePopularityService.getPopularMagazines(limit);

            // then
            assertNotNull(result);
            assertEquals(expectedSize, result.size());
        }

        @Test
        @DisplayName("존재하지 않은 매거진 ID 필터링")
        void getMagazineResponsesFromIds_FilterNonExistentMagazines() {
            // given
            String popularityKey = "magazine:popularity";
            Set<String> magazineIds = new LinkedHashSet<>();
            magazineIds.add("1");
            magazineIds.add("999");

            when(redisKeyManager.getMagazinePopularityKey()).thenReturn(popularityKey);
            when(zSetOps.reverseRange(popularityKey, 0, 9)).thenReturn(magazineIds);
            when(magazineRepository.findAllById(Arrays.asList(1L, 999L)))
                    .thenReturn(Collections.singletonList(mockMagazine));

            // when
            List<MagazineResponse> result = magazinePopularityService.getPopularMagazines(10);

            // then
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("카테고리별 인기 매거진 조회 테스트")
    class PopularByCategoryTest {
        @ParameterizedTest(name = "카테고리: {0}, Redis hit: {1}, 예외: {2}, 기대 결과: {3}")
        @CsvSource({
                "ACADEMIC,true,false,1",     // Redis hit
                "ACADEMIC,false,false,1",    // Redis miss, DB fallback
                "INVALID,false,true,0"       // 잘못된 카테고리, 예외 발생, 빈 리스트
        })
        void getPopularMagazinesByCategory_Param(String categoryName, boolean redisHit, boolean isException, int expectedSize) {
            int limit = 5;
            String categoryKey = "category:" + categoryName + ":popularity";
            MatchingCategory category;

            if (!"INVALID".equals(categoryName)) {
                category = MatchingCategory.valueOf(categoryName);
            } else {
                category = mock(MatchingCategory.class);
                when(category.getTitle()).thenReturn("INVALID");
            }

            when(redisKeyManager.getCategoryPopularityKey(anyString())).thenReturn(categoryKey);

            if (redisHit) {
                Set<String> topMagazineIds = new LinkedHashSet<>();
                topMagazineIds.add("1");
                when(zSetOps.reverseRange(categoryKey, 0, limit - 1)).thenReturn(topMagazineIds);
                when(magazineRepository.findAllById(Collections.singletonList(1L))).thenReturn(Collections.singletonList(mockMagazine));
            } else {
                when(zSetOps.reverseRange(categoryKey, 0, limit - 1)).thenReturn(Collections.emptySet());
                if (isException) {
                    when(magazineRepository.findByMagazineStatusAndCategoryOrderByLikeCountDesc(
                            eq(MagazineStatus.PUBLISHED), any(MatchingCategory.class)))
                            .thenThrow(new IllegalArgumentException());
                } else {
                    when(magazineRepository.findByMagazineStatusAndCategoryOrderByLikeCountDesc(
                            eq(MagazineStatus.PUBLISHED), eq(category)))
                            .thenReturn(Collections.singletonList(mockMagazine));
                }
            }

            // when
            List<MagazineResponse> result = magazinePopularityService.getPopularMagazinesByCategory(category, limit);

            // then
            assertEquals(expectedSize, result.size());
        }
    }



    @Nested
    @DisplayName("인기도 점수 초기화 및 삭제 테스트")
    class PopularityScoreManagementTest {
        @Test
        @DisplayName("인기도 점수 초기화 성공")
        void initializePopularityScore_Success() {
            // given
            when(redisKeyManager.getMagazinePopularityKey()).thenReturn("magazine:popularity");
            when(redisKeyManager.getCategoryPopularityKey("ACADEMIC")).thenReturn("category:ACADEMIC:popularity");

            // when
            magazinePopularityService.initializePopularityScore(mockMagazine);

            // then
            verify(zSetOps).incrementScore(eq("magazine:popularity"), eq(magazineId.toString()), anyDouble());
            verify(zSetOps).incrementScore(eq("category:ACADEMIC:popularity"), eq(magazineId.toString()), anyDouble());
        }

        @Test
        @DisplayName("인기도 점수 삭제 성공")
        void removePopularityScores_Success() {
            // given
            when(redisKeyManager.getMagazinePopularityKey()).thenReturn("magazine:popularity");
            when(redisKeyManager.getCategoryPopularityKey("ACADEMIC")).thenReturn("category:ACADEMIC:popularity");

            // when
            magazinePopularityService.removePopularityScores(magazineId, MatchingCategory.ACADEMIC);

            // then
            verify(zSetOps).remove("magazine:popularity", magazineId.toString());
            verify(zSetOps).remove("category:ACADEMIC:popularity", magazineId.toString());
        }

        @Test
        @DisplayName("카테고리 없는 인기도 점수 삭제 성공")
        void removePopularityScores_NullCategory_Success() {
            // given
            when(redisKeyManager.getMagazinePopularityKey()).thenReturn("magazine:popularity");

            // when
            magazinePopularityService.removePopularityScores(magazineId, null);

            // then
            verify(zSetOps).remove("magazine:popularity", magazineId.toString());
            verify(zSetOps, never()).remove(anyString(), eq("category:null:popularity"));
        }

        @Test
        @DisplayName("카테고리 null인 경우 점수 업데이트")
        void removePopularityScores_NullCategory_onlyMainKey() {
            // given
            when(redisKeyManager.getMagazinePopularityKey()).thenReturn("magazine:popularity");

            // when
            magazinePopularityService.removePopularityScores(magazineId, null);

            // then
            verify(zSetOps).remove("magazine:popularity", magazineId.toString());
            verify(zSetOps, never()).remove(contains("category:"), anySet());
        }
    }


    @Nested
    @DisplayName("사용자 참여 처리 - 추가 분기 테스트")
    class EngagementProcessingAdditionalTest {

        @Test
        @DisplayName("체류 시간만 유효한 경우 (스크롤 null)")
        void processEngagement_OnlyDwellTimeValid_ScrollNull() {
            // given
            String dwellTimeKey = "magazine:1:dwell_time";
            long dwellTime = 120000L;
            Double scrollPercentage = null;

            when(redisKeyManager.getMagazineDwellTimeKey(magazineId)).thenReturn(dwellTimeKey);
            when(redisKeyManager.getMagazinePopularityKey()).thenReturn("magazine:popularity");
            when(redisKeyManager.getCategoryPopularityKey("ACADEMIC")).thenReturn("category:ACADEMIC:popularity");

            // when
            magazinePopularityService.processEngagement(magazineId, userId, dwellTime, scrollPercentage);

            // then
            verify(zSetOps).add(eq(dwellTimeKey), eq(userId.toString()), eq((double) dwellTime));
            verify(zSetOps).incrementScore(eq("magazine:popularity"), eq(magazineId.toString()), anyDouble());
        }

        @Test
        @DisplayName("체류 시간만 유효한 경우 (스크롤 0)")
        void processEngagement_OnlyDwellTimeValid_ScrollZero() {
            // given
            String dwellTimeKey = "magazine:1:dwell_time";
            long dwellTime = 120000L;
            double scrollPercentage = 0.0;

            when(redisKeyManager.getMagazineDwellTimeKey(magazineId)).thenReturn(dwellTimeKey);
            when(redisKeyManager.getMagazinePopularityKey()).thenReturn("magazine:popularity");
            when(redisKeyManager.getCategoryPopularityKey("ACADEMIC")).thenReturn("category:ACADEMIC:popularity");

            // when
            magazinePopularityService.processEngagement(magazineId, userId, dwellTime, scrollPercentage);

            // then
            verify(zSetOps).add(eq(dwellTimeKey), eq(userId.toString()), eq((double) dwellTime));
            verify(zSetOps).incrementScore(eq("magazine:popularity"), eq(magazineId.toString()), anyDouble());
        }

        @Test
        @DisplayName("스크롤만 유효한 경우 (체류 시간 null)")
        void processEngagement_OnlyScrollValid_DwellTimeNull() {
            // given
            Long dwellTime = null;
            double scrollPercentage = 85.0;

            when(redisKeyManager.getMagazinePopularityKey()).thenReturn("magazine:popularity");
            when(redisKeyManager.getCategoryPopularityKey("ACADEMIC")).thenReturn("category:ACADEMIC:popularity");

            // when
            magazinePopularityService.processEngagement(magazineId, userId, dwellTime, scrollPercentage);

            // then
            verify(zSetOps, never()).add(anyString(), anyString(), anyDouble());
            verify(zSetOps).incrementScore(eq("magazine:popularity"), eq(magazineId.toString()), anyDouble());
        }

        @Test
        @DisplayName("스크롤만 유효한 경우 (체류 시간 0)")
        void processEngagement_OnlyScrollValid_DwellTimeZero() {
            // given
            long dwellTime = 0L;
            double scrollPercentage = 85.0;

            when(redisKeyManager.getMagazinePopularityKey()).thenReturn("magazine:popularity");
            when(redisKeyManager.getCategoryPopularityKey("ACADEMIC")).thenReturn("category:ACADEMIC:popularity");

            // when
            magazinePopularityService.processEngagement(magazineId, userId, dwellTime, scrollPercentage);

            // then
            verify(zSetOps, never()).add(anyString(), anyString(), anyDouble());
            verify(zSetOps).incrementScore(eq("magazine:popularity"), eq(magazineId.toString()), anyDouble());
        }

        @Test
        @DisplayName("모든 값이 유효하지 않은 경우 (totalScore = 0)")
        void processEngagement_NoValidInputs_NoScoreUpdate() {
            // given
            Long dwellTime = null;
            Double scrollPercentage = null;

            // when
            magazinePopularityService.processEngagement(magazineId, userId, dwellTime, scrollPercentage);

            // then
            verify(zSetOps, never()).add(anyString(), anyString(), anyDouble());
            verify(zSetOps, never()).incrementScore(anyString(), anyString(), anyDouble());
        }

        @Test
        @DisplayName("체류 시간 음수인 경우")
        void processEngagement_NegativeDwellTime() {
            // given
            long dwellTime = -1000L;
            double scrollPercentage = 85.0;

            when(redisKeyManager.getMagazinePopularityKey()).thenReturn("magazine:popularity");
            when(redisKeyManager.getCategoryPopularityKey("ACADEMIC")).thenReturn("category:ACADEMIC:popularity");

            // when
            magazinePopularityService.processEngagement(magazineId, userId, dwellTime, scrollPercentage);

            // then
            verify(zSetOps, never()).add(anyString(), anyString(), anyDouble());
            verify(zSetOps).incrementScore(eq("magazine:popularity"), eq(magazineId.toString()), anyDouble());
        }
    }

}