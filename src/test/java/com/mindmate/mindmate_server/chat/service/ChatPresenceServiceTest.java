package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
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
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatPresenceServiceTest {
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private RedisKeyManager redisKeyManager;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private ValueOperations<String, Object> valueOperations;

    @Mock private UserService userService;
    @Mock private ChatRoomService chatRoomService;

    @InjectMocks
    private ChatPresenceService chatPresenceService;

    private Long userId;
    private Long roomId;
    private ChatRoom mockChatRoom;
    private User mockUser;

    @BeforeEach
    void setup() {
        userId = 1L;
        roomId = 100L;
        mockChatRoom = mock(ChatRoom.class);
        mockUser = mock(User.class);

        // Redis 관련 모킹
        when(redisKeyManager.getUserStatusKey(userId)).thenReturn("user:status:" + userId);
        when(redisKeyManager.getUserStatusChannel(userId)).thenReturn("user:status:channel:" + userId);
        when(redisKeyManager.getUnreadCountKey(roomId, userId)).thenReturn("unread:count:" + roomId + ":" + userId);

        // HashOperations 및 ValueOperations 모킹
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        doReturn(true).when(redisTemplate).expire(anyString(), anyLong(), any(TimeUnit.class));
        doReturn(true).when(redisTemplate).delete(anyString());

        when(valueOperations.increment(anyString())).thenReturn(1L);
        doReturn(null).when(redisTemplate).convertAndSend(anyString(), any());

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(chatRoomService.findChatRoomById(roomId)).thenReturn(mockChatRoom);
        when(chatRoomService.save(any(ChatRoom.class))).thenReturn(mockChatRoom);
    }

    @Nested
    @DisplayName("사용자 상태 업데이트 테스트")
    class UserStatusTest {
        @Test
        @DisplayName("사용자 온라인 상태")
        void updateUserStatus_Online() {
            // given
            boolean isOnline = true;
            Long activeRoomId = roomId;

            // when
            chatPresenceService.updateUserStatus(userId, isOnline, activeRoomId);

            // then
            verify(hashOperations).putAll(eq("user:status:" + userId), any(Map.class));
            verify(redisTemplate).expire(eq("user:status:" + userId), eq(5L), eq(TimeUnit.MINUTES));
            verify(redisTemplate).convertAndSend(eq("user:status:channel:" + userId), any(Map.class));
        }

        @Test
        @DisplayName("사용자 오프라인")
        void updateUserStatus_OFFLINE() {
            // given
            boolean isOnline = false;
            Long activeRoomId = roomId;

            // when
            chatPresenceService.updateUserStatus(userId, isOnline, activeRoomId);

            // then
            verify(hashOperations).putAll(eq("user:status:" + userId), any(Map.class));
            verify(redisTemplate).expire(eq("user:status:" + userId), eq(30L), eq(TimeUnit.MINUTES));
            verify(redisTemplate).convertAndSend(eq("user:status:channel:" + userId), any(Map.class));
        }
    }

    @Test
    @DisplayName("활성 채팅방 조회 테스트")
    void getActiveRoom_Success() {
        // given
        when(hashOperations.get("user:status:" + userId, "activeRoomId")).thenReturn(roomId);

        // when
        Long activeRoom = chatPresenceService.getActiveRoom(userId);

        // then
        assertEquals(roomId, activeRoom);
        verify(hashOperations).get("user:status:" + userId, "activeRoomId");
    }

    @Nested
    @DisplayName("사용자 채팅방 활성 상태 확인")
    class IsUserActiveInRoomTest {
        @Test
        @DisplayName("사용자 채팅방 활성 상태 확인 - 활성")
        void isUserActiveInRoom_Active() {
            // given
            when(hashOperations.get("user:status:" + userId, "online")).thenReturn(true);
            when(hashOperations.get("user:status:" + userId, "activeRoomId")).thenReturn(roomId);

            // when
            boolean isActive = chatPresenceService.isUserActiveInRoom(userId, roomId);

            // then
            assertTrue(isActive);
        }

        @Test
        @DisplayName("사용자 채팅방 활성 상태 확인 - 비활성")
        void isUserActiveInRoom_Inactive() {
            // given
            when(hashOperations.get("user:status:" + userId, "online")).thenReturn(true);
            when(hashOperations.get("user:status:" + userId, "activeRoomId")).thenReturn(999L);

            // when
            boolean isActive = chatPresenceService.isUserActiveInRoom(userId, roomId);

            // then
            assertFalse(isActive);
        }

        @Test
        @DisplayName("사용자 채팅방 활성 상태 확인 - 오프라인")
        void isUserActiveInRoom_Offline() {
            // given
            when(hashOperations.get("user:status:" + userId, "online")).thenReturn(false);
            when(hashOperations.get("user:status:" + userId, "activeRoomId")).thenReturn(roomId);

            // when
            boolean isActive = chatPresenceService.isUserActiveInRoom(userId, roomId);

            // then
            assertFalse(isActive);
        }

        @Test
        @DisplayName("사용자 채팅방 활성 상태 확인 - 활성방 없음")
        void isUserActiveInRoom_NoActiveRoom() {
            // given
            when(hashOperations.get("user:status:" + userId, "online")).thenReturn(true);
            when(hashOperations.get("user:status:" + userId, "activeRoomId")).thenReturn(null);

            // when
            boolean isActive = chatPresenceService.isUserActiveInRoom(userId, roomId);

            // then
            assertFalse(isActive);
        }

        @Test
        @DisplayName("사용자 채팅방 활성 상태 확인 - 문자열 형태의 roomId")
        void isUserActiveInRoom_StringRoomId() {
            // given
            when(hashOperations.get("user:status:" + userId, "online")).thenReturn(true);
            when(hashOperations.get("user:status:" + userId, "activeRoomId")).thenReturn("100");

            // when
            boolean isActive = chatPresenceService.isUserActiveInRoom(userId, roomId);

            // then
            assertTrue(isActive);
        }
    }

    @Nested
    @DisplayName("읽지 않은 메시지 수 증가 테스트")
    class IncrementUnreadCountTest {
        @Test
        @DisplayName("Redis에서만 읽지 않은 메시지 수 증가")
        void incrementUnreadCountInRedis_Success() {
            // when
            Long count = chatPresenceService.incrementUnreadCountInRedis(roomId, userId);

            // then
            assertEquals(1L, count);
            verify(valueOperations).increment("unread:count:" + roomId + ":" + userId);
            verify(messagingTemplate).convertAndSendToUser(eq(userId.toString()), eq("/queue/unread"), any(Map.class));
        }
    }

    @Test
    @DisplayName("읽지 않은 메시지 수 초기화 테스트")
    void resetUnreadCount_Success() {
        // when
        chatPresenceService.resetUnreadCount(roomId, userId);

        // then
        verify(redisTemplate).delete("unread:count:" + roomId + ":" + userId);
        verify(messagingTemplate).convertAndSendToUser(eq(userId.toString()), eq("/queue/unread"), any(Map.class));
    }

}
