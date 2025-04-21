package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.FilteringWordCategory;
import com.mindmate.mindmate_server.chat.domain.MessageType;
import com.mindmate.mindmate_server.chat.dto.*;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.user.domain.Profile;
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
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // 불필요한 스터빙 경고 방지
class ChatServiceTest {
    @Mock private ChatRoomService chatRoomService;
    @Mock private UserService userService;
    @Mock private ChatPresenceService chatPresenceService;
    @Mock private ContentFilterService contentFilterService;
    @Mock private ChatMessageService chatMessageService;

    @Mock private KafkaTemplate<String, ChatMessageEvent> kafkaTemplate;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private RedisKeyManager redisKeyManager;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private ChatEventPublisher eventPublisher;

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

    @BeforeEach
    void setup() {
        userId1 = 1L;
        userId2 = 2L;
        userId3 = 3L;
        roomId = 100L;

        mockUser1 = mock(User.class);
        mockUser2 = mock(User.class);
        mockUser3 = mock(User.class);
        mockChatRoom = mock(ChatRoom.class);

        when(mockUser1.getId()).thenReturn(userId1);
        when(mockUser2.getId()).thenReturn(userId2);
        when(mockUser3.getId()).thenReturn(userId3);

        when(mockChatRoom.getId()).thenReturn(roomId);
        // user1 = 스피커 / user2 = 리스너
        when(mockChatRoom.isSpeaker(mockUser1)).thenReturn(true);
        when(mockChatRoom.isListener(mockUser2)).thenReturn(true);

        when(userService.findUserById(userId1)).thenReturn(mockUser1);
        when(userService.findUserById(userId2)).thenReturn(mockUser2);
        when(userService.findUserById(userId3)).thenReturn(mockUser3);
        when(chatRoomService.findChatRoomById(roomId)).thenReturn(mockChatRoom);

        when(redisKeyManager.getReadStatusKey(anyLong(), anyLong())).thenReturn("read:status:key");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }


    @Nested
    @DisplayName("메시지 전송 테스트")
    class SendMessageTest {
        @Test
        @DisplayName("메시지 전송 성공")
        void sendMessage_Success() {
            // given
            ChatMessageRequest request = new ChatMessageRequest(roomId, "Hello!", MessageType.TEXT);
            ChatMessage mockMessage = mock(ChatMessage.class);

            when(mockMessage.getId()).thenReturn(1L);
            when(mockMessage.getCreatedAt()).thenReturn(LocalDateTime.now());
            when(mockMessage.getChatRoom()).thenReturn(mockChatRoom);
            when(mockMessage.getSender()).thenReturn(mockUser1);
            when(mockMessage.getContent()).thenReturn("Hello!");
            when(mockMessage.getType()).thenReturn(MessageType.TEXT);
            when(chatMessageService.save(any(ChatMessage.class))).thenReturn(mockMessage);

            // when
            ChatMessageResponse response = chatService.sendMessage(userId1, request);

            // then
            assertNotNull(response);
            assertEquals("Hello!", response.getContent());
            verify(chatMessageService).save(any(ChatMessage.class));
            verify(kafkaTemplate).send(eq("chat-message-topic"), anyString(), any(ChatMessageEvent.class));
        }

        @Test
        @DisplayName("필털이된 메시지 처리")
        void sendMessage_Filtered() {
            // given
            ChatMessageRequest request = new ChatMessageRequest(roomId, "부적절한 내용", MessageType.TEXT);
            FilteringWordCategory mockCategory = mock(FilteringWordCategory.class);
            Profile mockProfile = mock(Profile.class);

            when(mockCategory.getDescription()).thenReturn("욕설");
            when(mockUser1.getProfile()).thenReturn(mockProfile);
            when(mockProfile.getNickname()).thenReturn("사용자1");
            when(contentFilterService.findFilteringWordCategory("부적절한 내용"))
                    .thenReturn(Optional.of(mockCategory));

            // when
            ChatMessageResponse response = chatService.sendMessage(userId1, request);

            // then
            assertNotNull(response);
            assertTrue(response.getContent().contains("욕설 관련 부적절한 내용이 감지되었습니다"));
            assertEquals(mockUser1.getId(), response.getSenderId());
            verify(chatMessageService, never()).save(any(ChatMessage.class));
            verify(eventPublisher).publishChatRoomEvent(
                    eq(roomId),
                    eq(ChatEventType.CONTENT_FILTERED),
                    any(ChatMessageResponse.class)
            );
        }
    }

    @Nested
    @DisplayName("읽음 상태 업데이트 테스트")
    class MarkAsReadTest {
        @Test
        @DisplayName("스피커 사용자의 읽음 상태 업데이트 성공")
        void markAsRead_Success_Speaker() {
            // given
            when(chatMessageService.findLatestMessageByChatRoomId(roomId)).thenReturn(Optional.empty());

            // when
            int result = chatService.markAsRead(userId1, roomId);

            // then
            assertEquals(0, result);
            verify(chatPresenceService).resetUnreadCount(roomId, userId1);
            verify(mockChatRoom).markAsRead(mockUser1, 0L);
            verify(chatRoomService).save(mockChatRoom);
            verify(redisTemplate.opsForValue()).set(anyString(), anyString());
            verify(redisTemplate).expire(anyString(), anyLong(), any(TimeUnit.class));
        }

        @Test
        @DisplayName("권한 없는 사용자의 읽음 상태 업데이트 실패")
        void markAsRead_Failure_NoAccess() {
            // given
            doThrow(new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
                    .when(chatRoomService).validateChatActivity(userId3, roomId);

            // when & then
            assertThrows(CustomException.class, () -> chatService.markAsRead(userId3, roomId));
            verify(chatPresenceService, never()).resetUnreadCount(anyLong(), anyLong());
            verify(chatRoomService, never()).save(any(ChatRoom.class));
        }
    }
}
