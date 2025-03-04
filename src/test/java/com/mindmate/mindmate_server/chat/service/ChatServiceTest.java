package com.mindmate.mindmate_server.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.MessageType;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.chat.dto.ChatMessageRequest;
import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.user.domain.ListenerProfile;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.SpeakerProfile;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // 불필요한 스터빙 경고 방지
class ChatServiceTest {
    @Mock private ChatRoomService chatRoomService;
    @Mock private UserService userService;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatPresenceService chatPresenceService;
    @Mock private KafkaTemplate<String, ChatMessageEvent> kafkaTemplate;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private RedisKeyManager redisKeyManager;
    @Mock private ObjectMapper objectMapper;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ChatServiceImpl chatService;

    private Long userId1;
    private Long userId2;
    private Long userId3; // 권한 없는 사용자
    private Long roomId;
    private User mockUser1;
    private User mockUser2;
    private User mockUser3;
    private ChatRoom mockChatRoom;
    private ListenerProfile listenerProfile;
    private SpeakerProfile speakerProfile;

    @BeforeEach
    void setup() {
        userId1 = 1L;
        userId2 = 2L;
        userId3 = 3L;
        roomId = 100L;

        mockUser1 = mock(User.class);
        mockUser2 = mock(User.class);
        mockUser3 = mock(User.class);
        listenerProfile = mock(ListenerProfile.class);
        speakerProfile = mock(SpeakerProfile.class);
        mockChatRoom = mock(ChatRoom.class);

        when(mockUser1.getId()).thenReturn(userId1);
        when(mockUser2.getId()).thenReturn(userId2);
        when(mockUser3.getId()).thenReturn(userId3);

        // 프로필과 사용자 연결
        when(listenerProfile.getUser()).thenReturn(mockUser2);
        when(speakerProfile.getUser()).thenReturn(mockUser1);

        when(mockChatRoom.getListener()).thenReturn(listenerProfile);
        when(mockChatRoom.getSpeaker()).thenReturn(speakerProfile);
        when(mockChatRoom.getId()).thenReturn(roomId);

        when(userService.findUserById(userId1)).thenReturn(mockUser1);
        when(userService.findUserById(userId2)).thenReturn(mockUser2);
        when(userService.findUserById(userId3)).thenReturn(mockUser3);
        when(chatRoomService.findChatRoomById(roomId)).thenReturn(mockChatRoom);

        // Redis 관련 모킹
        when(redisKeyManager.getChatRoomChannel(anyLong())).thenReturn("chat:room:" + roomId);
        when(redisKeyManager.getReadStatusKey(anyLong(), anyLong())).thenReturn("read:status:key");

        // ValueOperations 모킹
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString());
        doReturn(true).when(redisTemplate).expire(anyString(), anyLong(), any(TimeUnit.class));

        // 메시지 조회 모킹
        when(chatMessageRepository.findTopByChatRoomIdOrderByIdDesc(roomId))
                .thenReturn(Optional.empty());
    }


    @Nested
    @DisplayName("메시지 전송 테스트")
    class SendMessageTest {
        @Test
        @DisplayName("메시지 전송 성공")
        void sendMessage_Success() throws JsonProcessingException {
            // given
            ChatMessageRequest request = new ChatMessageRequest(roomId, "Hello!", MessageType.TEXT);
            ChatMessage mockMessage = mock(ChatMessage.class);
            when(mockMessage.getId()).thenReturn(1L);
            when(mockMessage.getCreatedAt()).thenReturn(LocalDateTime.now());

            when(mockUser1.getCurrentRole()).thenReturn(RoleType.ROLE_SPEAKER);
            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(mockMessage);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"message\":\"test\"}");

            // when
            ChatMessageResponse response = chatService.sendMessage(userId1, request);

            // then
            assertNotNull(response);
            assertEquals("Hello!", response.getContent());
            verify(chatMessageRepository).save(any(ChatMessage.class));
            verify(redisTemplate).convertAndSend(anyString(), anyString());
            verify(kafkaTemplate).send(anyString(), anyString(), any(ChatMessageEvent.class));
        }
    }

    @Nested
    @DisplayName("읽음 상태 업데이트 테스트")
    class MarkAsReadTest {
        @Test
        @DisplayName("스피커 사용자의 읽음 상태 업데이트 성공")
        void markAsRead_Success_Speaker() {
            // given
            // 스피커 사용자(userId1)로 테스트

            // when
            int result = chatService.markAsRead(userId1, roomId);

            // then
            assertEquals(0, result);
            verify(chatPresenceService).resetUnreadCount(roomId, userId1);
            verify(mockChatRoom).markAsReadForSpeaker(anyLong());
            verify(redisTemplate.opsForValue()).set(anyString(), anyString());
            verify(redisTemplate).expire(anyString(), anyLong(), any(TimeUnit.class));
        }

        @Test
        @DisplayName("리스너 사용자의 읽음 상태 업데이트 성공")
        void markAsRead_Success_Listener() {
            // given
            // 리스너 사용자(userId2)로 테스트

            // when
            int result = chatService.markAsRead(userId2, roomId);

            // then
            assertEquals(0, result);
            verify(chatPresenceService).resetUnreadCount(roomId, userId2);
            verify(mockChatRoom).markAsReadForListener(anyLong());
            verify(redisTemplate.opsForValue()).set(anyString(), anyString());
            verify(redisTemplate).expire(anyString(), anyLong(), any(TimeUnit.class));
        }

        @Test
        @DisplayName("권한 없는 사용자의 읽음 상태 업데이트 실패")
        void markAsRead_Failure_NoAccess() {
            // given
            // 권한 없는 사용자(userId3)로 테스트

            // when & then
            assertThrows(CustomException.class, () -> chatService.markAsRead(userId3, roomId));
            verify(chatPresenceService, never()).resetUnreadCount(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("Redis 관련 예외 처리 테스트")
    class RedisExceptionTest {
        @Test
        @DisplayName("Redis 예외 발생 시 정상 처리")
        void handleRedisException() throws JsonProcessingException {
            // given
            when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Test exception") {});

            // when
            int result = chatService.markAsRead(userId1, roomId);

            // then
            assertEquals(0, result);
            verify(chatPresenceService).resetUnreadCount(roomId, userId1);
        }
    }
}
