package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.MessageType;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.chat.dto.ChatMessageRequest;
import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.service.ResilientEventPublisher;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.matching.domain.Matching;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // 불필요한 스터빙 경고 방지
class ChatServiceTest {
    @Mock private ChatRoomService chatRoomService;
    @Mock private UserService userService;
    @Mock private ChatPresenceService chatPresenceService;
    @Mock private ContentFilterService contentFilterService;
    @Mock private ChatMessageService chatMessageService;
    @Mock private ResilientEventPublisher eventPublisher;

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private RedisKeyManager redisKeyManager;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ChatServiceImpl chatService;

    private static final Long USER_ID_1 = 1L;
    private static final Long USER_ID_2 = 2L;
    private static final Long USER_ID_3 = 3L;
    private static final Long ROOM_ID = 100L;
    private static final String TOPIC_NAME = "chat-message-topic";
    private static final String NORMAL_CONTENT = "Hello!";
    private static final String FILTERED_CONTENT = "부적절한 내용";
    private static final String FILTERED_RESPONSE = "[부적절한 내용이 감지되었습니다]";
    private static final String NICKNAME = "사용자1";
    private static final String ANONYMOUS = "익명";

    private User mockUser1;
    private User mockUser2;
    private User mockUser3;
    private ChatRoom mockChatRoom;
    private Matching mockMatching;
    private Profile mockProfile;

    @BeforeEach
    void setup() {
        setupMockObjects();
        setupMockBehaviors();
        setupRedisOperations();
        setupValidations();
    }

    private void setupMockObjects() {
        mockUser1 = mock(User.class);
        mockUser2 = mock(User.class);
        mockUser3 = mock(User.class);
        mockChatRoom = mock(ChatRoom.class);
        mockMatching = mock(Matching.class);
        mockProfile = mock(Profile.class);
    }

    private void setupMockBehaviors() {
        // User Mock 설정
        when(mockUser1.getId()).thenReturn(USER_ID_1);
        when(mockUser2.getId()).thenReturn(USER_ID_2);
        when(mockUser3.getId()).thenReturn(USER_ID_3);
        when(mockUser1.getProfile()).thenReturn(mockProfile);
        when(mockUser2.getProfile()).thenReturn(mockProfile);
        when(mockProfile.getNickname()).thenReturn(NICKNAME);

        // ChatRoom Mock 설정
        when(mockChatRoom.getId()).thenReturn(ROOM_ID);
        when(mockChatRoom.getMatching()).thenReturn(mockMatching);
        when(mockChatRoom.isSpeaker(mockUser1)).thenReturn(true);
        when(mockChatRoom.isListener(mockUser2)).thenReturn(true);
        when(mockChatRoom.getListener()).thenReturn(mockUser2);
        when(mockChatRoom.getSpeaker()).thenReturn(mockUser1);

        // Service Mock 설정
        when(userService.findUserById(USER_ID_1)).thenReturn(mockUser1);
        when(userService.findUserById(USER_ID_2)).thenReturn(mockUser2);
        when(userService.findUserById(USER_ID_3)).thenReturn(mockUser3);
        when(chatRoomService.findChatRoomById(ROOM_ID)).thenReturn(mockChatRoom);

        // 기본값 설정
        when(mockMatching.isAnonymous()).thenReturn(false);
        when(contentFilterService.isFiltered(anyString())).thenReturn(false);
    }

    private void setupRedisOperations() {
        when(redisKeyManager.getReadStatusKey(anyLong(), anyLong())).thenReturn("read:status:key");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private void setupValidations() {
        doNothing().when(chatRoomService).validateChatActivity(USER_ID_1, ROOM_ID);
        doNothing().when(chatRoomService).validateChatActivity(USER_ID_2, ROOM_ID);
        doThrow(new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
                .when(chatRoomService).validateChatActivity(USER_ID_3, ROOM_ID);

        doNothing().when(chatRoomService).validateChatRead(USER_ID_1, ROOM_ID);
        doNothing().when(chatRoomService).validateChatRead(USER_ID_2, ROOM_ID);
        doThrow(new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
                .when(chatRoomService).validateChatRead(USER_ID_3, ROOM_ID);
    }

    private ChatMessage createMockMessage(Long messageId, User sender, String content) {
        ChatMessage mockMessage = mock(ChatMessage.class);
        when(mockMessage.getId()).thenReturn(messageId);
        when(mockMessage.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(mockMessage.getChatRoom()).thenReturn(mockChatRoom);
        when(mockMessage.getSender()).thenReturn(sender);
        when(mockMessage.getContent()).thenReturn(content);
        when(mockMessage.getType()).thenReturn(MessageType.TEXT);
        return mockMessage;
    }

    private ChatMessageRequest createMessageRequest(String content) {
        return new ChatMessageRequest(ROOM_ID, content, MessageType.TEXT);
    }


    @Nested
    @DisplayName("메시지 전송 테스트")
    class SendMessageTest {
        @ParameterizedTest
        @DisplayName("정상 메시지 전송 시나리오")
        @MethodSource("normalMessageScenarios")
        void sendMessage_NormalMessage_Success(
                String description,
                Long senderId,
                Long recipientId,
                boolean recipientActive) {
            // given
            ChatMessageRequest request = createMessageRequest(NORMAL_CONTENT);
            User sender = senderId.equals(USER_ID_1) ? mockUser1 : mockUser2;
            User recipient = recipientId.equals(USER_ID_1) ? mockUser1 : mockUser2;

            ChatMessage mockMessage = createMockMessage(1L, sender, NORMAL_CONTENT);

            when(chatMessageService.save(any(ChatMessage.class))).thenReturn(mockMessage);
            when(chatPresenceService.isUserActiveInRoom(recipientId, ROOM_ID)).thenReturn(recipientActive);

            // when
            ChatMessageResponse response = chatService.sendMessage(senderId, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo(NORMAL_CONTENT);
            assertThat(response.getSenderId()).isEqualTo(senderId);
            assertThat(response.isError()).isFalse();

            verify(chatMessageService).save(any(ChatMessage.class));
            verify(mockChatRoom).updateLastMessageTime();
            verify(chatPresenceService).isUserActiveInRoom(recipientId, ROOM_ID);

            // 이벤트 발행 검증
            ArgumentCaptor<ChatMessageEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageEvent.class);
            verify(eventPublisher).publishEvent(eq(TOPIC_NAME), anyString(), eventCaptor.capture());

            ChatMessageEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getMessageId()).isEqualTo(1L);
            assertThat(capturedEvent.getRecipientId()).isEqualTo(recipientId);
            assertThat(capturedEvent.isRecipientActive()).isEqualTo(recipientActive);
            assertThat(capturedEvent.isFiltered()).isFalse();
        }

        static Stream<Arguments> normalMessageScenarios() {
            return Stream.of(
                    Arguments.of("스피커가 발신자, 리스너 활성", USER_ID_1, USER_ID_2, true),
                    Arguments.of("스피커가 발신자, 리스너 비활성", USER_ID_1, USER_ID_2, false),
                    Arguments.of("리스너가 발신자, 스피커 활성", USER_ID_2, USER_ID_1, true),
                    Arguments.of("리스너가 발신자, 스피커 비활성", USER_ID_2, USER_ID_1, false)
            );
        }

        @ParameterizedTest
        @DisplayName("필터링된 메시지 처리 시나리오")
        @MethodSource("filteredMessageScenarios")
        void sendMessage_FilteredMessage(
                String description,
                boolean isAnonymous,
                String expectedSenderName) {
            // given
            ChatMessageRequest request = createMessageRequest(FILTERED_CONTENT);
            when(contentFilterService.isFiltered(FILTERED_CONTENT)).thenReturn(true);
            when(mockMatching.isAnonymous()).thenReturn(isAnonymous);

            // when
            ChatMessageResponse response = chatService.sendMessage(USER_ID_1, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).contains(FILTERED_RESPONSE);
            assertThat(response.getSenderName()).isEqualTo(expectedSenderName);

            verify(chatMessageService, never()).save(any(ChatMessage.class));

            ArgumentCaptor<ChatMessageEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageEvent.class);
            verify(eventPublisher).publishEvent(eq(TOPIC_NAME), anyString(), eventCaptor.capture());

            ChatMessageEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getMessageId()).isNull();
            assertThat(capturedEvent.getContent()).isEqualTo(FILTERED_CONTENT);
            assertThat(capturedEvent.isFiltered()).isTrue();
        }

        static Stream<Arguments> filteredMessageScenarios() {
            return Stream.of(
                    Arguments.of("익명 채팅방", true, ANONYMOUS),
                    Arguments.of("일반 채팅방", false, NICKNAME)
            );
        }

        @Test
        @DisplayName("권한 없는 사용자의 메시지 전송 실패")
        void sendMessage_Unauthorized() {
            // given
            ChatMessageRequest request = createMessageRequest(NORMAL_CONTENT);

            // when
            ChatMessageResponse response = chatService.sendMessage(USER_ID_3, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isError()).isTrue();
            assertThat(response.getErrorMessage()).isNotNull();
            verify(chatMessageService, never()).save(any(ChatMessage.class));
            verify(eventPublisher, never()).publishEvent(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("예외 발생 시 에러 응답 반환")
        void sendMessage_ExceptionHandling() {
            // given
            ChatMessageRequest request = createMessageRequest(NORMAL_CONTENT);
            when(chatMessageService.save(any(ChatMessage.class))).thenThrow(new RuntimeException("Test exception"));

            // when
            ChatMessageResponse response = chatService.sendMessage(USER_ID_1, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isError()).isTrue();
            assertThat(response.getErrorMessage()).isEqualTo("메시지 전송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    @Nested
    @DisplayName("읽음 상태 업데이트 테스트")
    class MarkAsReadTest {
        @ParameterizedTest
        @DisplayName("읽음 상태 업데이트 시나리오")
        @MethodSource("markAsReadScenarios")
        void markAsRead_Scenarios(
                String description,
                Long userId,
                Optional<Long> lastMessageId,
                boolean isAuthorized,
                boolean shouldThrow) {
            // given
            if (lastMessageId.isPresent()) {
                ChatMessage mockLatestMessage = mock(ChatMessage.class);
                when(mockLatestMessage.getId()).thenReturn(lastMessageId.get());
                when(chatMessageService.findLatestMessageByChatRoomId(ROOM_ID)).thenReturn(Optional.of(mockLatestMessage));
            } else {
                when(chatMessageService.findLatestMessageByChatRoomId(ROOM_ID)).thenReturn(Optional.empty());
            }

            // when & then
            if (shouldThrow) {
                assertThrows(CustomException.class, () -> chatService.markAsRead(userId, ROOM_ID));
                verify(chatPresenceService, never()).resetUnreadCount(anyLong(), anyLong());
                verify(chatRoomService, never()).save(any(ChatRoom.class));
            } else {
                int result = chatService.markAsRead(userId, ROOM_ID);

                assertThat(result).isEqualTo(0);
                verify(chatPresenceService).resetUnreadCount(ROOM_ID, userId);
                verify(chatRoomService).save(mockChatRoom);
                verify(valueOperations).set(anyString(), anyString());
                verify(redisTemplate).expire(anyString(), eq(1L), eq(TimeUnit.DAYS));
            }
        }

        static Stream<Arguments> markAsReadScenarios() {
            return Stream.of(
                    Arguments.of("최신 메시지가 있을 때", USER_ID_1, Optional.of(10L), true, false),
                    Arguments.of("최신 메시지가 없을 때", USER_ID_2, Optional.empty(), true, false),
                    Arguments.of("권한 없는 사용자", USER_ID_3, Optional.empty(), false, true)
            );
        }
    }

    @Nested
    @DisplayName("메시지 이벤트 발행 테스트")
    class PublishMessageEventTest {
        @Test
        @DisplayName("트랜잭션 동기화 활성화 시 afterCommit에서 메시지 발행")
        void publishMessageEvent_TransactionSynchronizationActive() {
            // given
            ChatMessage mockMessage = createMockMessage(1L, mockUser1, NORMAL_CONTENT);
            TransactionSynchronizationManager.initSynchronization();

            try {
                // when
                chatService.publishMessageEvent(mockMessage, USER_ID_2, true, NORMAL_CONTENT);

                verify(eventPublisher, never()).publishEvent(anyString(), anyString(), any());

                TransactionSynchronizationManager.getSynchronizations()
                        .forEach(TransactionSynchronization::afterCommit);

                // then
                ArgumentCaptor<ChatMessageEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageEvent.class);
                verify(eventPublisher).publishEvent(eq(TOPIC_NAME), anyString(), eventCaptor.capture());

                ChatMessageEvent capturedEvent = eventCaptor.getValue();
                assertThat(capturedEvent.getMessageId()).isEqualTo(1L);
                assertThat(capturedEvent.getContent()).isEqualTo(NORMAL_CONTENT);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }
    }
}
