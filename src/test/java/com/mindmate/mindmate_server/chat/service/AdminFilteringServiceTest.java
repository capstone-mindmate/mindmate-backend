package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.dto.UserFilteringHistoryDTO;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminFilteringServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private RedisKeyManager redisKeyManager;
    @Mock private UserService userService;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private ListOperations<String, Object> listOperations;

    @InjectMocks
    private AdminFilteringService adminFilteringService;

    @BeforeEach
    void setup() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    private User createMockUser(Long userId, String email, String nickname) {
        User user = mock(User.class);
        Profile profile = mock(Profile.class);
        when(user.getEmail()).thenReturn(email);
        when(user.getProfile()).thenReturn(profile);
        when(profile.getNickname()).thenReturn(nickname);
        return user;
    }

    @ParameterizedTest(name = "getUserFilteringHistory - {0}")
    @MethodSource("userFilteringHistoryScenarios")
    void getUserFilteringHistory_Param(
            String desc,
            Long userId,
            Long roomId,
            int filterCount,
            List<String> filteredContents,
            String email,
            String nickname,
            int expectedUniqueContentCount
    ) {
        // given
        String countKey = "filtering:count:" + userId + ":" + roomId;
        String contentKey = "filtering:content:" + userId + ":" + roomId;
        Set<String> keys = Set.of(countKey);

        when(redisKeyManager.getFilteringCountKey(userId, "*")).thenReturn("filtering:count:" + userId + ":*");
        when(redisTemplate.keys("filtering:count:" + userId + ":*")).thenReturn(keys);
        when(valueOperations.get(countKey)).thenReturn(filterCount);
        when(redisKeyManager.getFilteringContentKey(userId, roomId)).thenReturn(contentKey);
        when(listOperations.range(contentKey, 0, -1))
                .thenReturn(new ArrayList<>(filteredContents));

        User mockUser = createMockUser(userId, email, nickname);
        when(userService.findUserById(userId)).thenReturn(mockUser);

        // when
        UserFilteringHistoryDTO result = adminFilteringService.getUserFilteringHistory(userId);

        // then
        assertEquals(userId, result.getUserId());
        assertEquals(email, result.getEmail());
        assertEquals(nickname, result.getNickname());
        assertEquals(filterCount, result.getRoomFilterCounts().get(roomId));
        assertEquals(expectedUniqueContentCount, result.getRecentFilteredContents().size());
        assertEquals(filterCount, result.getTotalFilterCount());
    }

    static Stream<Arguments> userFilteringHistoryScenarios() {
        return Stream.of(
                Arguments.of("중복 없는 필터링 내용", 1L, 10L, 2, List.of("욕설1", "욕설2"), "test1@ex.com", "닉1", 2),
                Arguments.of("중복 포함 필터링 내용", 2L, 20L, 3, List.of("욕설1", "욕설2", "욕설1"), "test2@ex.com", "닉2", 2),
                Arguments.of("필터링 내용 없음", 3L, 30L, 0, List.of(), "test3@ex.com", "닉3", 0)
        );
    }

    @Test
    @DisplayName("전체 사용자 필터링 이력 조회 - 여러 유저, 정렬")
    void getUserFilteringHistories_MultiUser_Sorted() {
        // given
        String key1 = "filtering:count:1:10";
        String key2 = "filtering:count:2:20";
        Set<String> allKeys = Set.of(key1, key2);

        when(redisTemplate.keys("filtering:count:*")).thenReturn(allKeys);

        AdminFilteringService spyService = Mockito.spy(adminFilteringService);
        doReturn(UserFilteringHistoryDTO.builder().userId(1L).totalFilterCount(5).build())
                .when(spyService).getUserFilteringHistory(1L);
        doReturn(UserFilteringHistoryDTO.builder().userId(2L).totalFilterCount(10).build())
                .when(spyService).getUserFilteringHistory(2L);

        // when
        List<UserFilteringHistoryDTO> result = spyService.getUserFilteringHistories();

        // then
        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getUserId()); // totalFilterCount 내림차순
        assertEquals(1L, result.get(1).getUserId());
    }
}
