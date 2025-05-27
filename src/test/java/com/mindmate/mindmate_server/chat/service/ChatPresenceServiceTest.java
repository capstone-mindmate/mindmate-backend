package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatEventType;
import com.mindmate.mindmate_server.chat.repository.ChatRoomRepository;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatPresenceServiceTest {
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private RedisKeyManager redisKeyManager;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private ChatEventPublisher eventPublisher;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private UserService userService;

    @InjectMocks
    private ChatPresenceService chatPresenceService;

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 100L;
    private static final Long ROOM_ID_2 = 200L;
    private static final String USER_STATUS_KEY = "user:status:1";
    private static final String UNREAD_COUNT_KEY = "unread:count:100:1";
    private static final String TOTAL_UNREAD_KEY = "user:total:unread:1";

    private ChatRoom mockChatRoom;
    private ChatRoom mockChatRoom2;
    private User mockUser;

    @BeforeEach
    void setup() {
        mockChatRoom = mock(ChatRoom.class);
        mockChatRoom2 = mock(ChatRoom.class);
        mockUser = mock(User.class);

        setupRedisKeyManager();
        setupRedisOperations();
        setupMockObjects();
    }

    private void setupRedisKeyManager() {
        when(redisKeyManager.getUserStatusKey(USER_ID)).thenReturn(USER_STATUS_KEY);
        when(redisKeyManager.getUnreadCountKey(ROOM_ID, USER_ID)).thenReturn(UNREAD_COUNT_KEY);
        when(redisKeyManager.getUnreadCountKey(ROOM_ID_2, USER_ID)).thenReturn("unread:count:200:1");
        when(redisKeyManager.getUserTotalUnreadCountKey(USER_ID)).thenReturn(TOTAL_UNREAD_KEY);
    }

    private void setupRedisOperations() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(redisTemplate.delete(anyString())).thenReturn(true);
    }

    private void setupMockObjects() {
        when(mockUser.getId()).thenReturn(USER_ID);
        when(mockChatRoom.getId()).thenReturn(ROOM_ID);
        when(mockChatRoom2.getId()).thenReturn(ROOM_ID_2);
        when(mockChatRoom.isSpeaker(mockUser)).thenReturn(true);
        when(mockChatRoom.getSpeakerUnreadCount()).thenReturn(5L);
        when(mockChatRoom.getListenerUnreadCount()).thenReturn(3L);

        when(userService.findUserById(USER_ID)).thenReturn(mockUser);
    }

    @Nested
    @DisplayName("사용자 상태 업데이트 테스트")
    class UserStatusTest {
        @ParameterizedTest
        @DisplayName("사용자 상태 업데이트 시나리오")
        @MethodSource("userStatusScenarios")
        void updateUserStatus_Scenarios(
                String description,
                boolean isOnline,
                Long expectedExpireTime) {
            // when
            chatPresenceService.updateUserStatus(USER_ID, isOnline, ROOM_ID);

            // then
            ArgumentCaptor<Map<String, Object>> statusCaptor = ArgumentCaptor.forClass(Map.class);
            verify(hashOperations).putAll(eq(USER_STATUS_KEY), statusCaptor.capture());

            Map<String, Object> capturedStatus = statusCaptor.getValue();
            assertThat(capturedStatus.get("online")).isEqualTo(isOnline);
            assertThat(capturedStatus.get("activeRoomId")).isEqualTo(ROOM_ID);
            assertThat(capturedStatus.get("status")).isEqualTo(isOnline ? "ONLINE" : "OFFLINE");
            assertThat(capturedStatus.get("lastActive")).isNotNull();

            verify(redisTemplate).expire(eq(USER_STATUS_KEY), eq(expectedExpireTime), eq(TimeUnit.MINUTES));
            verify(eventPublisher).publishUserEvent(eq(USER_ID), eq(ChatEventType.USER_STATUS), any(Map.class));
        }

        static Stream<Arguments> userStatusScenarios() {
            return Stream.of(
                    Arguments.of("온라인 상태", true, 5L),
                    Arguments.of("오프라인 상태", false, 30L)
            );
        }
    }


    @Test
    @DisplayName("활성 채팅방 조회")
    void getActiveRoom_Success() {
        // given
        when(hashOperations.get(USER_STATUS_KEY, "activeRoomId")).thenReturn(ROOM_ID);

        // when
        Long activeRoom = chatPresenceService.getActiveRoom(USER_ID);

        // then
        assertThat(activeRoom).isEqualTo(ROOM_ID);
        verify(hashOperations).get(USER_STATUS_KEY, "activeRoomId");
    }

    @Nested
    @DisplayName("사용자 채팅방 활성 상태 확인")
    class IsUserActiveRoomTest {
        @ParameterizedTest
        @DisplayName("사용자 활성 상태 확인 시나리오")
        @MethodSource("userActiveScenarios")
        void isUserActiveInRoom_Scenarios(
                String description,
                Boolean isOnline,
                Object activeRoomObj,
                boolean expectedResult) {
            // given
            when(hashOperations.get(USER_STATUS_KEY, "online")).thenReturn(isOnline);
            when(hashOperations.get(USER_STATUS_KEY, "activeRoomId")).thenReturn(activeRoomObj);

            // when
            boolean isActive = chatPresenceService.isUserActiveInRoom(USER_ID, ROOM_ID);

            // then
            assertThat(isActive).isEqualTo(expectedResult);
        }


        static Stream<Arguments> userActiveScenarios() {
            return Stream.of(
                    Arguments.of("온라인이고 같은 방에 있음", true, ROOM_ID, true),
                    Arguments.of("온라인이지만 다른 방에 있음", true, 999L, false),
                    Arguments.of("오프라인 상태", false, ROOM_ID, false),
                    Arguments.of("활성 방이 null", true, null, false),
                    Arguments.of("온라인 상태가 null", null, ROOM_ID, false)
            );
        }

        @ParameterizedTest
        @DisplayName("activeRoomId 타입 변환 테스트")
        @MethodSource("roomIdTypeScenarios")
        void isUserActiveInRoom_TypeConversion(
                String description,
                Object activeRoomObj,
                boolean expectedResult) {
            // given
            when(hashOperations.get(USER_STATUS_KEY, "online")).thenReturn(true);
            when(hashOperations.get(USER_STATUS_KEY, "activeRoomId")).thenReturn(activeRoomObj);

            // when
            boolean isActive = chatPresenceService.isUserActiveInRoom(USER_ID, ROOM_ID);

            // then
            assertThat(isActive).isEqualTo(expectedResult);
        }

        static Stream<Arguments> roomIdTypeScenarios() {
            return Stream.of(
                    Arguments.of("Long 타입", ROOM_ID, true),
                    Arguments.of("Integer 타입", 100, true),
                    Arguments.of("String 타입 (유효한 숫자)", "100", true),
                    Arguments.of("String 타입 (다른 숫자)", "999", false),
                    Arguments.of("String 타입 (잘못된 형식)", "invalid", false),
                    Arguments.of("지원되지 않는 타입", new Object(), false)
            );
        }
    }

    @Nested
    @DisplayName("읽지 않은 메시지 수 관리")
    class UnreadCountTest {
        @Test
        @DisplayName("Redis에서 읽지 않은 메시지 수 증가")
        void incrementUnreadCountInRedis_Success() {
            // given
            when(valueOperations.increment(UNREAD_COUNT_KEY)).thenReturn(3L);

            // when
            Long count = chatPresenceService.incrementUnreadCountInRedis(ROOM_ID, USER_ID);

            // then
            assertThat(count).isEqualTo(3L);
            verify(valueOperations).increment(UNREAD_COUNT_KEY);

            ArgumentCaptor<Map<String, Object>> unreadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(messagingTemplate).convertAndSendToUser(
                    eq(USER_ID.toString()),
                    eq("/queue/unread"),
                    unreadCaptor.capture()
            );

            Map<String, Object> unreadData = unreadCaptor.getValue();
            assertThat(unreadData.get("roomId")).isEqualTo(ROOM_ID);
            assertThat(unreadData.get("unreadCount")).isEqualTo(3L);
        }

        @Test
        @DisplayName("읽지 않은 메시지 수 초기화")
        void resetUnreadCount_Success() {
            // when
            chatPresenceService.resetUnreadCount(ROOM_ID, USER_ID);

            // then
            verify(redisTemplate).delete(UNREAD_COUNT_KEY);

            ArgumentCaptor<Map<String, Object>> unreadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(messagingTemplate).convertAndSendToUser(
                    eq(USER_ID.toString()),
                    eq("/queue/unread"),
                    unreadCaptor.capture()
            );

            Map<String, Object> unreadData = unreadCaptor.getValue();
            assertThat(unreadData.get("roomId")).isEqualTo(ROOM_ID);
            assertThat(unreadData.get("unreadCount")).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("전체 읽지 않은 메시지 수 계싼")
    class TotalUnreadCountTest {
        @Test
        @DisplayName("사용자 채팅방이 없는 경우")
        void getTotalUnreadCount_NoRooms() {
            // given
            when(chatRoomRepository.findActiveChatRoomByUserId(USER_ID)).thenReturn(Collections.emptyList());

            // when
            Long totalCount = chatPresenceService.getTotalUnreadCount(USER_ID);

            // then
            assertThat(totalCount).isEqualTo(0L);
        }

        @Test
        @DisplayName("Redis 모든 데이터가 잇는 경우")
        void getTotalUnreadCount_AllFromRedis() {
            // given
            List<ChatRoom> chatRooms = Arrays.asList(mockChatRoom, mockChatRoom2);
            when(chatRoomRepository.findActiveChatRoomByUserId(USER_ID)).thenReturn(chatRooms);
            when(valueOperations.get("unread:count:100:1")).thenReturn(5L);
            when(valueOperations.get("unread:count:200:1")).thenReturn(3L);

            // when
            Long totalCount = chatPresenceService.getTotalUnreadCount(USER_ID);

            // then
            assertThat(totalCount).isEqualTo(8L);
            verify(valueOperations, never()).set(anyString(), any());
        }

        @ParameterizedTest
        @DisplayName("Reidis 데이터 타입 변환 테스트")
        @MethodSource("redisDataTypeScenarios")
        void getTotalUnreadCount_DataTypeConversion(
                String description,
                Object redisValue,
                Long expectedContribution) {
            // given
            List<ChatRoom> chatRooms = Arrays.asList(mockChatRoom);
            when(chatRoomRepository.findActiveChatRoomByUserId(USER_ID)).thenReturn(chatRooms);
            when(valueOperations.get(UNREAD_COUNT_KEY)).thenReturn(redisValue);

            // when
            Long totalCount = chatPresenceService.getTotalUnreadCount(USER_ID);

            // then
            assertThat(totalCount).isEqualTo(expectedContribution);
        }

        static Stream<Arguments> redisDataTypeScenarios() {
            return Stream.of(
                    Arguments.of("Long 타입", 5L, 5L),
                    Arguments.of("Integer 타입", 3, 3L),
                    Arguments.of("String 타입 (유효한 숫자)", "7", 7L),
                    Arguments.of("String 타입 (잘못된 형식)", "invalid", 0L)
            );
        }

        @Test
        @DisplayName("Redis에 데이터가 없어서 DB에서 가져오는 경우")
        void getTotalUnreadCount_FallbackToDatabase() {
            // given
            List<ChatRoom> chatRooms = Arrays.asList(mockChatRoom, mockChatRoom2);
            when(chatRoomRepository.findActiveChatRoomByUserId(USER_ID)).thenReturn(chatRooms);

            when(valueOperations.get("unread:count:100:1")).thenReturn(null);
            when(valueOperations.get("unread:count:200:1")).thenReturn(null);

            when(mockChatRoom.isSpeaker(mockUser)).thenReturn(true);
            when(mockChatRoom.getSpeakerUnreadCount()).thenReturn(5L);
            when(mockChatRoom2.isSpeaker(mockUser)).thenReturn(false);
            when(mockChatRoom2.getListenerUnreadCount()).thenReturn(3L);

            // when
            Long totalCount = chatPresenceService.getTotalUnreadCount(USER_ID);

            // then
            assertThat(totalCount).isEqualTo(8L);
            verify(valueOperations).set("unread:count:100:1", 5L);
            verify(valueOperations).set("unread:count:200:1", 3L);
        }

    }
}
