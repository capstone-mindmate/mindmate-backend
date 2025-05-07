package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.matching.domain.MatchingStatus;
import com.mindmate.mindmate_server.matching.repository.MatchingRepository;
import com.mindmate.mindmate_server.user.domain.Profile;
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
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;
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
    @Mock
    private MatchingRepository matchingRepository;

    @InjectMocks
    private RedisMatchingService redisMatchingService;

    @Mock
    private User creator;
    @Mock
    private User applicant;
    @Mock
    private Profile creatorProfile;
    @Mock
    private Profile applicantProfile;
    @Mock
    private Matching matching;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        // 사용자 및 프로필
        when(creator.getId()).thenReturn(1L);
        when(applicant.getId()).thenReturn(2L);

        when(creatorProfile.getDepartment()).thenReturn("Computer Science");
        when(creatorProfile.getEntranceTime()).thenReturn(2020);
        when(creator.getProfile()).thenReturn(creatorProfile);

        when(applicantProfile.getDepartment()).thenReturn("Psychology");
        when(applicantProfile.getEntranceTime()).thenReturn(2021);
        when(applicant.getProfile()).thenReturn(applicantProfile);

        // 매칭
        when(matching.getId()).thenReturn(1L);
        when(matching.getCreator()).thenReturn(creator);
        when(matching.getCreatorRole()).thenReturn(InitiatorType.SPEAKER);
        when(matching.getCategory()).thenReturn(MatchingCategory.CAREER);
        when(matching.getStatus()).thenReturn(MatchingStatus.OPEN);
        when(matching.isOpen()).thenReturn(true);
        when(matching.isAllowRandom()).thenReturn(true);
    }

    @Test
    @DisplayName("매칭 가능 세트에 추가")
    void addMatchingToAvailableSet() {
        // when
        redisMatchingService.addMatchingToAvailableSet(matching);

        // then
        verify(redisTemplate.opsForSet()).add("matching:available:SPEAKER", "1");
        verify(redisTemplate).expire("matching:available:SPEAKER", 24, TimeUnit.HOURS);
    }

    @Test
    @DisplayName("매칭 가능 세트에서 제거")
    void removeMatchingFromAvailableSet() {
        // when
        redisMatchingService.removeMatchingFromAvailableSet(1L, InitiatorType.SPEAKER);

        // then
        verify(redisTemplate.opsForSet()).remove("matching:available:SPEAKER", "1");
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
    @DisplayName("활성 매칭 수 감소 - 0 이하일 때 키 삭제")
    void decrementUserActiveMatchingCountToZero() {
        // given
        given(redisTemplate.opsForValue().decrement(anyString())).willReturn(0L);

        // when
        redisMatchingService.decrementUserActiveMatchingCount(1L);

        // then
        verify(redisTemplate.opsForValue()).decrement("user:1:activeMatchings");
        verify(redisTemplate).delete("user:1:activeMatchings");
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
        verify(redisTemplate.opsForSet(), never()).remove(anyString(), any());
    }

    @Test
    @DisplayName("매칭 키 정리 - 매칭 완료 상태")
    void cleanupMatchingKeys_matched() {
        // given
        when(matching.isOpen()).thenReturn(false);
        when(matching.getStatus()).thenReturn(MatchingStatus.MATCHED);

        // when
        redisMatchingService.cleanupMatchingKeys(matching);

        // then
        verify(redisTemplate.opsForSet()).remove(eq("matching:available:SPEAKER"), eq("1"));
    }

    @Test
    @DisplayName("매칭 키 정리 - 취소 상태")
    void cleanupMatchingKeys_canceled() {
        // given
        when(matching.isOpen()).thenReturn(false);
        when(matching.getStatus()).thenReturn(MatchingStatus.CANCELED);

        // when
        redisMatchingService.cleanupMatchingKeys(matching);

        // then
        verify(redisTemplate.opsForSet()).remove(eq("matching:available:SPEAKER"), eq("1"));
    }

    @Test
    @DisplayName("랜덤 매칭 가져오기 - 매칭 가능한 경우")
    void getRandomMatching_available() {
        // given
        Set<String> matchingIds = new HashSet<>();
        matchingIds.add("1");

        given(redisTemplate.opsForSet().members("matching:available:SPEAKER")).willReturn(matchingIds);
        given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));

        // when
        Long result = redisMatchingService.getRandomMatching(applicant, InitiatorType.LISTENER);

        // then
        assertThat(result).isEqualTo(1L);
        verify(redisTemplate.opsForSet()).members("matching:available:SPEAKER");
        verify(matchingRepository).findById(1L);
    }

    @Test
    @DisplayName("랜덤 매칭 가져오기 - 매칭 가능한 것이 없는 경우")
    void getRandomMatching_notAvailable() {
        // given
        Set<String> emptySet = new HashSet<>();
        given(redisTemplate.opsForSet().members("matching:available:SPEAKER")).willReturn(emptySet);

        // when
        Long result = redisMatchingService.getRandomMatching(applicant, InitiatorType.LISTENER);

        // then
        assertThat(result).isNull();
        verify(redisTemplate.opsForSet()).members("matching:available:SPEAKER");
        verify(matchingRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("랜덤 매칭 가져오기 - 매칭이 닫혀있는 경우 제외")
    void getRandomMatching_closedMatching() {
        // given
        Set<String> matchingIds = new HashSet<>();
        matchingIds.add("1");

        Matching closedMatching = mock(Matching.class);
        when(closedMatching.getId()).thenReturn(1L);
        when(closedMatching.isOpen()).thenReturn(false);
        when(closedMatching.isAllowRandom()).thenReturn(true);

        given(redisTemplate.opsForSet().members("matching:available:SPEAKER")).willReturn(matchingIds);
        given(matchingRepository.findById(1L)).willReturn(Optional.of(closedMatching));

        // when
        Long result = redisMatchingService.getRandomMatching(applicant, InitiatorType.LISTENER);

        // then
        assertThat(result).isNull();
        verify(redisTemplate.opsForSet()).members("matching:available:SPEAKER");
        verify(matchingRepository).findById(1L);
        verify(redisTemplate.opsForSet()).remove(eq("matching:available:SPEAKER"), eq("1"));
    }

    @Test
    @DisplayName("랜덤 매칭 가져오기 - 랜덤 허용하지 않는 매칭 제외")
    void getRandomMatching_notRandomAllowed() {
        // given
        Set<String> matchingIds = new HashSet<>();
        matchingIds.add("1");

        Matching nonRandomMatching = mock(Matching.class);
        when(nonRandomMatching.getId()).thenReturn(1L);
        when(nonRandomMatching.isOpen()).thenReturn(true);
        when(nonRandomMatching.isAllowRandom()).thenReturn(false);

        given(redisTemplate.opsForSet().members("matching:available:SPEAKER")).willReturn(matchingIds);
        given(matchingRepository.findById(1L)).willReturn(Optional.of(nonRandomMatching));

        // when
        Long result = redisMatchingService.getRandomMatching(applicant, InitiatorType.LISTENER);

        // then
        assertThat(result).isNull();
        verify(redisTemplate.opsForSet()).members("matching:available:SPEAKER");
        verify(matchingRepository).findById(1L);
        verify(redisTemplate.opsForSet()).remove(eq("matching:available:SPEAKER"), eq("1"));
    }

    @Test
    @DisplayName("랜덤 매칭 - 점수 계산 테스트")
    void calculateMatchingScore() {
        // given
        Set<String> matchingIds = new HashSet<>();
        matchingIds.add("1");
        matchingIds.add("2");

        Matching sameDeptMatching = mock(Matching.class);
        when(sameDeptMatching.getId()).thenReturn(1L);
        when(sameDeptMatching.isOpen()).thenReturn(true);
        when(sameDeptMatching.isAllowRandom()).thenReturn(true);
        when(sameDeptMatching.getCreator()).thenReturn(creator);
        when(sameDeptMatching.getCreatorRole()).thenReturn(InitiatorType.SPEAKER);

        User creator2 = mock(User.class);
        Profile creatorProfile2 = mock(Profile.class);
        when(creatorProfile2.getDepartment()).thenReturn("Software Engineering");
        when(creatorProfile2.getEntranceTime()).thenReturn(2021);
        when(creator2.getProfile()).thenReturn(creatorProfile2);

        Matching sameCollegeMatching = mock(Matching.class);
        when(sameCollegeMatching.getId()).thenReturn(2L);
        when(sameCollegeMatching.isOpen()).thenReturn(true);
        when(sameCollegeMatching.isAllowRandom()).thenReturn(true);
        when(sameCollegeMatching.getCreator()).thenReturn(creator2);
        when(sameCollegeMatching.getCreatorRole()).thenReturn(InitiatorType.SPEAKER);

        given(redisTemplate.opsForSet().members("matching:available:SPEAKER")).willReturn(matchingIds);
        given(matchingRepository.findById(1L)).willReturn(Optional.of(sameDeptMatching));
        given(matchingRepository.findById(2L)).willReturn(Optional.of(sameCollegeMatching));

        when(applicantProfile.getDepartment()).thenReturn("Computer Science");

        // when
        Long result = redisMatchingService.getRandomMatching(applicant, InitiatorType.LISTENER);

        // then
        assertThat(result).isNotNull();
        verify(redisTemplate.opsForSet()).members("matching:available:SPEAKER");
        verify(matchingRepository).findById(1L);
        verify(matchingRepository).findById(2L);
    }

    @Test
    @DisplayName("동일 단과대학 확인 테스트")
    void testIsSameCollege() {
        // given
        when(creatorProfile.getDepartment()).thenReturn("컴퓨터공학과");
        when(applicantProfile.getDepartment()).thenReturn("응용화학공학과");

        Set<String> matchingIds = new HashSet<>();
        matchingIds.add("1");

        given(redisTemplate.opsForSet().members("matching:available:SPEAKER")).willReturn(matchingIds);
        given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));

        // when
        Long result = redisMatchingService.getRandomMatching(applicant, InitiatorType.LISTENER);

        // then
        assertThat(result).isEqualTo(1L); // 같은 단과대학이므로 매칭 점수가 높아야 함
        verify(redisTemplate.opsForSet()).members("matching:available:SPEAKER");
        verify(matchingRepository).findById(1L);
    }

    @Test
    @DisplayName("연도 차이에 따른 점수 계산 테스트")
    void testYearDifferenceScore() {
        // given
        Set<String> matchingIds = new HashSet<>();
        matchingIds.add("1");
        matchingIds.add("2");

        when(creatorProfile.getEntranceTime()).thenReturn(2021);
        when(applicantProfile.getEntranceTime()).thenReturn(2021);

        User creator2 = mock(User.class);
        Profile creatorProfile2 = mock(Profile.class);
        when(creatorProfile2.getDepartment()).thenReturn("Psychology");
        when(creatorProfile2.getEntranceTime()).thenReturn(2018);
        when(creator2.getProfile()).thenReturn(creatorProfile2);

        Matching differentYearMatching = mock(Matching.class);
        when(differentYearMatching.getId()).thenReturn(2L);
        when(differentYearMatching.isOpen()).thenReturn(true);
        when(differentYearMatching.isAllowRandom()).thenReturn(true);
        when(differentYearMatching.getCreator()).thenReturn(creator2);
        when(differentYearMatching.getCreatorRole()).thenReturn(InitiatorType.SPEAKER);

        given(redisTemplate.opsForSet().members("matching:available:SPEAKER")).willReturn(matchingIds);
        given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
        given(matchingRepository.findById(2L)).willReturn(Optional.of(differentYearMatching));

        // when
        Long result = redisMatchingService.getRandomMatching(applicant, InitiatorType.LISTENER);

        // then
        assertThat(result).isNotNull();
        verify(redisTemplate.opsForSet()).members("matching:available:SPEAKER");
        verify(matchingRepository).findById(1L);
        verify(matchingRepository).findById(2L);
    }

    @Test
    @DisplayName("매칭이 100개 초과 시 무작위 선택 테스트")
    void testRandomSelectionWhenManyMatches() {
        // given
        Set<String> largeMatchingSet = new HashSet<>();
        for (int i = 1; i <= 150; i++) {
            largeMatchingSet.add(String.valueOf(i));
        }

        given(redisTemplate.opsForSet().members("matching:available:SPEAKER")).willReturn(largeMatchingSet);

        for (int i = 1; i <= 150; i++) {
            Matching m = mock(Matching.class);
            when(m.getId()).thenReturn((long) i);
            when(m.isOpen()).thenReturn(true);
            when(m.isAllowRandom()).thenReturn(true);

            User u = mock(User.class);
            Profile p = mock(Profile.class);
            when(p.getDepartment()).thenReturn("Department" + i);
            when(p.getEntranceTime()).thenReturn(2020);
            when(u.getProfile()).thenReturn(p);
            when(m.getCreator()).thenReturn(u);
            when(m.getCreatorRole()).thenReturn(InitiatorType.SPEAKER);

            given(matchingRepository.findById((long) i)).willReturn(Optional.of(m));
        }

        // when
        Long result = redisMatchingService.getRandomMatching(applicant, InitiatorType.LISTENER);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isBetween(1L, 150L);

        verify(redisTemplate.opsForSet()).members("matching:available:SPEAKER");
        // 매칭 후보가 많을 경우 100개로 제한해서 처리하는지 확인
        verify(matchingRepository, atMost(100)).findById(anyLong());
    }

    @Test
    @DisplayName("학과 매핑을 통한 단과대학 정보 추출")
    void extractCollegeFromDepartment() {
        // given
        Set<String> matchingIds = new HashSet<>();
        matchingIds.add("1");

        when(creatorProfile.getDepartment()).thenReturn("경영학과");
        when(applicantProfile.getDepartment()).thenReturn("경제학과");

        given(redisTemplate.opsForSet().members("matching:available:SPEAKER")).willReturn(matchingIds);
        given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));

        // when
        Long result = redisMatchingService.getRandomMatching(applicant, InitiatorType.LISTENER);

        // then
        assertThat(result).isEqualTo(1L); // 같은 단과대(경영대학)이므로 높은 점수 받아야 함
        verify(matchingRepository).findById(1L);
    }
}