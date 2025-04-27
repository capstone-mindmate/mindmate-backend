package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.MessageType;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.chat.dto.ChatMessageRequest;
import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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
        when(mockChatRoom.getListener()).thenReturn(mockUser2);
        when(mockChatRoom.getSpeaker()).thenReturn(mockUser1);

        when(userService.findUserById(userId1)).thenReturn(mockUser1);
        when(userService.findUserById(userId2)).thenReturn(mockUser2);
        when(userService.findUserById(userId3)).thenReturn(mockUser3);
        when(chatRoomService.findChatRoomById(roomId)).thenReturn(mockChatRoom);

        when(redisKeyManager.getReadStatusKey(anyLong(), anyLong())).thenReturn("read:status:key");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        doNothing().when(chatRoomService).validateChatActivity(userId1, roomId);
        doNothing().when(chatRoomService).validateChatActivity(userId2, roomId);
        doThrow(new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
                .when(chatRoomService).validateChatActivity(userId3, roomId);

        doNothing().when(chatRoomService).validateChatRead(userId1, roomId);
        doNothing().when(chatRoomService).validateChatRead(userId2, roomId);
        doThrow(new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
                .when(chatRoomService).validateChatRead(userId3, roomId);

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
            Profile mockProfile = mock(Profile.class);

            when(mockUser1.getProfile()).thenReturn(mockProfile);
            when(mockProfile.getNickname()).thenReturn("사용자1");

            when(mockMessage.getId()).thenReturn(1L);
            when(mockMessage.getCreatedAt()).thenReturn(LocalDateTime.now());
            when(mockMessage.getChatRoom()).thenReturn(mockChatRoom);
            when(mockMessage.getSender()).thenReturn(mockUser1);
            when(mockMessage.getContent()).thenReturn("Hello!");
            when(mockMessage.getType()).thenReturn(MessageType.TEXT);

            when(chatMessageService.save(any(ChatMessage.class))).thenReturn(mockMessage);
            when(chatPresenceService.isUserActiveInRoom(userId2, roomId)).thenReturn(true);

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
            Profile mockProfile = mock(Profile.class);

            when(mockUser1.getProfile()).thenReturn(mockProfile);
            when(mockProfile.getNickname()).thenReturn("사용자1");
            when(contentFilterService.isFiltered("부적절한 내용")).thenReturn(true);

            // when
            ChatMessageResponse response = chatService.sendMessage(userId1, request);

            // then
            assertNotNull(response);
            assertTrue(response.getContent().contains("부적절한 내용이 감지되었습니다"));
            assertEquals(mockUser1.getId(), response.getSenderId());
            verify(chatMessageService, never()).save(any(ChatMessage.class));
            verify(kafkaTemplate).send(eq("chat-message-topic"), anyString(), any(ChatMessageEvent.class));
        }

        @Test
        @DisplayName("권한 없는 사용자의 메시지 전송 실패")
        void sendMessage_Unauthorized() {
            // given
            ChatMessageRequest request = new ChatMessageRequest(roomId, "Hello!", MessageType.TEXT);

            // when
            ChatMessageResponse response = chatService.sendMessage(userId3, request);

            // then
            assertNotNull(response);
            assertTrue(response.isError());
            assertNotNull(response.getErrorMessage());
            verify(chatMessageService, never()).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("예외 발생 시 에러 응답 반환")
        void sendMessage_Error() {
            // given
            ChatMessageRequest request = new ChatMessageRequest(roomId, "Hello!", MessageType.TEXT);
            when(chatMessageService.save(any(ChatMessage.class))).thenThrow(new RuntimeException("Test exception"));

            // when
            ChatMessageResponse response = chatService.sendMessage(userId1, request);

            // then
            assertNotNull(response);
            assertTrue(response.isError());
            assertEquals("메시지 전송에 실패했습니다. 잠시 후 다시 시도해주세요.", response.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("읽음 상태 업데이트 테스트")
    class MarkAsReadTest {
        @ParameterizedTest
        @MethodSource("markAsReadScenarios")
        void markAsRead_Scenarios(
                String desc,
                Long userId,
                Optional<Long> latestMessageId,
                boolean isAuthorized,
                boolean shouldThrow) {
            // given
            if (latestMessageId.isPresent()) {
                ChatMessage mockLatestMessage = mock(ChatMessage.class);
                when(mockLatestMessage.getId()).thenReturn(latestMessageId.get());
                when(chatMessageService.findLatestMessageByChatRoomId(roomId)).thenReturn(Optional.of(mockLatestMessage));
            } else {
                when(chatMessageService.findLatestMessageByChatRoomId(roomId)).thenReturn(Optional.empty());
            }

            if (!isAuthorized) {
                doThrow(new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
                        .when(chatRoomService).validateChatRead(userId, roomId);
            }

            // when & then
            if (shouldThrow) {
                assertThrows(CustomException.class, () -> chatService.markAsRead(userId, roomId));
                verify(chatPresenceService, never()).resetUnreadCount(anyLong(), anyLong());
                verify(chatRoomService, never()).save(any(ChatRoom.class));
            } else {
                int result = chatService.markAsRead(userId, roomId);
                assertEquals(0, result);
                verify(chatPresenceService).resetUnreadCount(roomId, userId);
                verify(chatRoomService).save(mockChatRoom);
                verify(redisTemplate.opsForValue()).set(anyString(), anyString());
                verify(redisTemplate).expire(anyString(), anyLong(), any(TimeUnit.class));
            }
        }

        static Stream<Arguments> markAsReadScenarios() {
            return Stream.of(
                    Arguments.of("최신 메시지가 있을 때", 1L, Optional.of(10L), true, false),
                    Arguments.of("최신 메시지가 없을 때", 2L, Optional.empty(), true, false),
                    Arguments.of("권한 없는 사용자", 3L, Optional.empty(), false, true)
            );
        }
    }

    @Nested
    @DisplayName("메시지 이벤트 발행 테스트")
    class PublishMessageEventTest {
        @Test
        @DisplayName("메시지 이벤트 발행 성공")
        void publishMessageEvent_Success() {
            // given
            ChatMessage mockMessage = mock(ChatMessage.class);
            when(mockMessage.getId()).thenReturn(1L);
            when(mockMessage.getChatRoom()).thenReturn(mockChatRoom);
            when(mockMessage.getSender()).thenReturn(mockUser1);
            when(mockMessage.getContent()).thenReturn("Hello!");
            when(mockMessage.getType()).thenReturn(MessageType.TEXT);
            when(mockMessage.getCreatedAt()).thenReturn(LocalDateTime.now());

            // when
            chatService.publishMessageEvent(mockMessage, userId2, true, "Hello!");

            // then
            ArgumentCaptor<ChatMessageEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageEvent.class);
            verify(kafkaTemplate).send(eq("chat-message-topic"), anyString(), eventCaptor.capture());

            ChatMessageEvent capturedEvent = eventCaptor.getValue();
            assertEquals(1L, capturedEvent.getMessageId());
            assertEquals(roomId, capturedEvent.getRoomId());
            assertEquals(userId1, capturedEvent.getSenderId());
            assertEquals("Hello!", capturedEvent.getContent());
            assertEquals(MessageType.TEXT, capturedEvent.getType());
            assertEquals(userId2, capturedEvent.getRecipientId());
            assertTrue(capturedEvent.isRecipientActive());
            assertFalse(capturedEvent.isFiltered());
            assertEquals("Hello!", capturedEvent.getPlainContent());
        }

        @Test
        @DisplayName("트랜잭션 동기화 활성화 시 afterCommit에서 메시지 발행")
        void publishMessageEvent_TransactionSynchronizationActive() {
            // given
            ChatMessage mockMessage = mock(ChatMessage.class);
            when(mockMessage.getId()).thenReturn(1L);
            when(mockMessage.getChatRoom()).thenReturn(mockChatRoom);
            when(mockMessage.getSender()).thenReturn(mockUser1);
            when(mockMessage.getContent()).thenReturn("Hello!");
            when(mockMessage.getType()).thenReturn(MessageType.TEXT);
            when(mockMessage.getCreatedAt()).thenReturn(LocalDateTime.now());

            TransactionSynchronizationManager.initSynchronization();

            try {
                chatService.publishMessageEvent(mockMessage, userId2, true, "Hello!");;

                TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
                verify(kafkaTemplate).send(eq("chat-message-topic"), anyString(), any(ChatMessageEvent.class));
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

    }
}
