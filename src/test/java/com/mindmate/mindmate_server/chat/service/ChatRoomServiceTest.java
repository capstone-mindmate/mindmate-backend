package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.chat.dto.*;
import com.mindmate.mindmate_server.chat.repository.ChatRoomRepository;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.ProfileService;
import com.mindmate.mindmate_server.user.service.UserService;
import org.assertj.core.util.Streams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatRoomServiceTest {
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMessageService chatMessageService;
    @Mock private UserService userService;
    @Mock private NotificationService notificationService;
    @Mock private ProfileService profileService;
    @Mock private KafkaTemplate<String, ChatRoomCloseEvent> kafkaTemplate;

    @InjectMocks
    private ChatRoomServiceImpl chatRoomService;

    private final Long userId = 1L;
    private final Long roomId = 100L;
    private ChatRoom mockChatRoom;
    private User mockUser;
    private User mockListener;
    private User mockSpeaker;
    private Matching mockMatching;
    private Profile mockProfile;

    @BeforeEach
    void setup() {
        // Mock 객체 생성
        mockUser = mock(User.class);
        mockListener = mock(User.class);
        mockSpeaker = mock(User.class);
        mockChatRoom = mock(ChatRoom.class);
        mockMatching = mock(Matching.class);
        mockProfile = mock(Profile.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockChatRoom.getId()).thenReturn(roomId);
        when(mockSpeaker.getId()).thenReturn(2L);
        when(mockChatRoom.getId()).thenReturn(roomId);
        when(mockMatching.getId()).thenReturn(1L);

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mockChatRoom));
        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(mockChatRoom.getMatching()).thenReturn(mockMatching);
        when(mockChatRoom.getListener()).thenReturn(mockListener);
        when(mockChatRoom.getSpeaker()).thenReturn(mockSpeaker);

        Profile mockListenerProfile = mock(Profile.class);
        Profile mockSpeakerProfile = mock(Profile.class);
        when(mockListenerProfile.getNickname()).thenReturn("Listener");
        when(mockSpeakerProfile.getNickname()).thenReturn("Speaker");
        when(mockListener.getProfile()).thenReturn(mockListenerProfile);
        when(mockSpeaker.getProfile()).thenReturn(mockSpeakerProfile);

        when(chatRoomRepository.findAllByUserId(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(new ChatRoomResponse())));
    }

    @Nested
    @DisplayName("채팅방 조회")
    class FindChatRoomTest {
        @Test
        @DisplayName("채팅방 ID로 조회 성공")
        void findChatRoomById_Success() {
            // when
            ChatRoom result = chatRoomService.findChatRoomById(roomId);

            // then
            assertNotNull(result);
            assertEquals(roomId, result.getId());
            verify(chatRoomRepository).findById(roomId);
        }

        @Test
        @DisplayName("채팅방 ID로 조회 실패 - 존재하지 않은 채팅방")
        void findChatRoomById_NotFound() {
            // given
            Long nonExistRoomId = 999L;
            when(chatRoomRepository.findById(nonExistRoomId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(CustomException.class, () -> chatRoomService.findChatRoomById(nonExistRoomId));
        }
    }

    @Nested
    @DisplayName("사용자로 채팅방 목록 조회")
    class FindChatRoomByUserTest {
        @Test
        @DisplayName("사용자의 채팅방 목록 조회 - pagination")
        void getChatRoomsForUser_Success() {
            // given
            PageRequest pageRequest = PageRequest.of(0, 10);
            Page<ChatRoomResponse> mockPage = new PageImpl<>(List.of(new ChatRoomResponse()));
            when(chatRoomRepository.findAllByUserId(userId, pageRequest)).thenReturn(mockPage);

            // when
            Page<ChatRoomResponse> result = chatRoomService.getChatRoomsForUser(userId, pageRequest);

            // then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("역할별 채팅방 목록 조회")
        void getChatRoomsByUserRole_Success() {
            // given
            PageRequest pageRequest = PageRequest.of(0, 10);
            Page<ChatRoomResponse> mockPage = new PageImpl<>(List.of(new ChatRoomResponse()));
            when(chatRoomRepository.findAllByUserIdAndRole(eq(userId), anyString(), eq(pageRequest)))
                    .thenReturn(mockPage);

            // when
            Page<ChatRoomResponse> result = chatRoomService.getChatRoomsByUserRole(userId, pageRequest, "ROLE_LISTENER");

            // then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("상태별 채팅방 목록 조회")
        void getChatROomByUserAndStatus_Success() {
            // given
            PageRequest pageRequest = PageRequest.of(0, 10);
            Page<ChatRoomResponse> mockPage = new PageImpl<>(List.of(new ChatRoomResponse()));
            when(chatRoomRepository.findAllByUserIdAndStatus(eq(userId), eq(ChatRoomStatus.ACTIVE), eq(pageRequest)))
                    .thenReturn(mockPage);

            // when
            Page<ChatRoomResponse> result = chatRoomService.getChatRoomsByUserAndStatus(userId, pageRequest, ChatRoomStatus.ACTIVE);

            // then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }
    }

    @Test
    @DisplayName("채팅방 종료 요청 성공")
    void closeChatRoom_Success() {
        // given
        when(mockChatRoom.getChatRoomStatus()).thenReturn(ChatRoomStatus.ACTIVE);
        when(mockChatRoom.isListener(mockUser)).thenReturn(true);
        when(mockChatRoom.isSpeaker(mockUser)).thenReturn(false);

        // when
        chatRoomService.closeChatRoom(userId, roomId);

        // then
        verify(mockChatRoom).requestClosure(mockUser);
        verify(chatRoomRepository).save(mockChatRoom);
    }

    @Nested
    @DisplayName("채팅방 종료 요청 처리")
    class HandleCloseChatRoomTest {
        @Test
        @DisplayName("채팅방 종료 요청 수락")
        void acceptCloseChatRoom_Success() {
            // given
            when(mockChatRoom.getChatRoomStatus()).thenReturn(ChatRoomStatus.CLOSE_REQUEST);
            when(mockChatRoom.isClosureRequester(mockUser)).thenReturn(false);

            // when
            chatRoomService.acceptCloseChatRoom(userId, roomId);

            // then
            verify(mockChatRoom).acceptClosure();
            verify(profileService).incrementCounselingCount(mockListener.getId());
            verify(chatRoomRepository).save(mockChatRoom);
            verify(kafkaTemplate).send(eq("chat-room-close-topic"), anyString(), any(ChatRoomCloseEvent.class));
        }

        @Test
        @DisplayName("채팅방 종료 요청 거절")
        void rejectCloseChatRoom_Success() {
            // given
            when(mockChatRoom.getChatRoomStatus()).thenReturn(ChatRoomStatus.CLOSE_REQUEST);
            when(mockChatRoom.isClosureRequester(mockUser)).thenReturn(false);

            // when
            chatRoomService.rejectCloseChatRoom(userId, roomId);

            // then
            verify(mockChatRoom).rejectClosure();
            verify(chatRoomRepository).save(mockChatRoom);
            verify(notificationService).processNotification(any(ChatRoomNotificationEvent.class));
        }

        @Test
        @DisplayName("종료 요청 상태가 아닌 채팅방 처리 시도")
        void handleCloseChatRoom_NotRequested() {
            // given
            when(mockChatRoom.getChatRoomStatus()).thenReturn(ChatRoomStatus.ACTIVE);

            // when & then
            assertThrows(CustomException.class, () -> chatRoomService.acceptCloseChatRoom(userId, roomId));
            assertThrows(CustomException.class, () -> chatRoomService.rejectCloseChatRoom(userId, roomId));
        }

        @Test
        @DisplayName("자신의 종료 요청을 처리하려는 시도")
        void handleCloseChatRoom_OwnRequest() {
            // given
            when(mockChatRoom.getChatRoomStatus()).thenReturn(ChatRoomStatus.CLOSE_REQUEST);
            when(mockChatRoom.isClosureRequester(mockUser)).thenReturn(true);

            // when & then
            assertThrows(CustomException.class, () -> chatRoomService.acceptCloseChatRoom(userId, roomId));
            assertThrows(CustomException.class, () -> chatRoomService.rejectCloseChatRoom(userId, roomId));
        }
    }

    @Nested
    @DisplayName("초기 메시지 로드")
    class GetInitialMessagesTest {
        @ParameterizedTest
        @DisplayName("메시지 로드 시나리오")
        @MethodSource("messageLoadScenarios")
        void getInitialMessages_Scenarios(
                boolean isListener,
                long lastReadMessageId,
                long totalMessages,
                boolean hasLatestMessage,
                int expectedMessageCount) {
            // given
            when(mockChatRoom.isListener(mockUser)).thenReturn(isListener);

            if (isListener) {
                when(mockChatRoom.getListenerLastReadMessageId()).thenReturn(lastReadMessageId);
            } else {
                when(mockChatRoom.getSpeakerLastReadMessageId()).thenReturn(lastReadMessageId);
            }
            when(chatMessageService.countMessagesByChatRoomId(roomId)).thenReturn(totalMessages);

            if (hasLatestMessage) {
                ChatMessage latestMessage = createMockChatMessage(totalMessages);
                when(chatMessageService.findLatestMessageByChatRoomId(roomId)).thenReturn(Optional.of(latestMessage));
            }

            setupMessageMocking(lastReadMessageId, totalMessages);

            // when
            ChatRoomDetailResponse result = chatRoomService.getInitialMessages(userId, roomId, 10);

            // then
            assertNotNull(result);
            assertEquals(expectedMessageCount, result.getMessages().size());

            if (totalMessages > 0) {
                verify(mockChatRoom).markAsRead(mockUser, totalMessages);
                verify(chatRoomRepository).save(mockChatRoom);
            }
        }

        static Stream<Arguments> messageLoadScenarios() {
            return Stream.of(
                    // isListener, lastReadMessageId, totalMessages, hasLatestMessage, expectedMessageCount
                    Arguments.of(true, 0L, 0L, false, 0),      // 메시지가 없는 경우
                    Arguments.of(true, 0L, 5L, false, 5),      // 첫 입장 - 모든 메시지 로드
                    Arguments.of(true, 5L, 10L, true, 10),     // 재접속 - 읽지 않은 메시지 존재
                    Arguments.of(false, 10L, 10L, true, 10)    // 재접속 - 모든 메시지 읽음
            );
        }

        private void setupMessageMocking(long lastReadMessageId, long totalMessages) {
            if (totalMessages == 0) {
                return;
            }

            if (lastReadMessageId == 0) {
                // 첫 접속
                List<ChatMessage> allMessages = IntStream.range(1, (int) totalMessages + 1)
                        .mapToObj(i -> createMockChatMessage((long) i))
                        .collect(Collectors.toList());

                when(chatMessageService.findAllByChatRoomIdOrderByIdAsc(roomId)).thenReturn(allMessages);
            } else if (lastReadMessageId < totalMessages) {
                // 읽지 않은 메시지가 있는 케이스
                List<ChatMessage> previousMessages = IntStream.range(1, (int)lastReadMessageId + 1)
                        .mapToObj(i -> createMockChatMessage((long)i))
                        .collect(Collectors.toList());
                when(chatMessageService.findMessagesBeforeId(eq(roomId), eq(lastReadMessageId), eq(10)))
                        .thenReturn(previousMessages);

                List<ChatMessage> newMessages = IntStream.rangeClosed((int)lastReadMessageId + 1, (int)totalMessages)
                        .mapToObj(i -> createMockChatMessage((long)i))
                        .collect(Collectors.toList());
                when(chatMessageService.findMessagesAfterOrEqualId(roomId, lastReadMessageId))
                        .thenReturn(newMessages);
            } else {
                // 모든 메시지를 읽은 케이스
                List<ChatMessage> recentMessages = IntStream.range(1, (int)totalMessages + 1)
                        .mapToObj(i -> createMockChatMessage((long)i))
                        .collect(Collectors.toList());
                when(chatMessageService.findRecentMessages(roomId, 10)).thenReturn(recentMessages);
            }
        }
    }

    @Test
    @DisplayName("이전 메시지 조회")
    void getPreviousMessage() {
        // given
        Long messageId  = 10L;
        int size = 5;
        List<ChatMessage> messages = IntStream.range(5, 10)
                .mapToObj(i -> createMockChatMessage((long) i))
                .collect(Collectors.toList());

        when(chatMessageService.findPreviousMessages(roomId, messageId, size)).thenReturn(messages);

        // when
        List<ChatMessageResponse> result = chatRoomService.getPreviousMessages(roomId, messageId, userId, size);

        // then
        assertNotNull(result);
        assertEquals(5, result.size());
    }

    @Nested
    @DisplayName("채팅방 활동 검증")
    class ValidateChatActivityTest {
        @ParameterizedTest
        @DisplayName("채팅방 활동 검증 시나리오")
        @MethodSource("chatActivityScenarios")
        void validateChatActivity_Scenarios(
                ChatRoomStatus status,
                boolean isParticipant,
                boolean shouldThrowException) {
            // given
            when(mockChatRoom.getChatRoomStatus()).thenReturn(status);
            when(mockChatRoom.isListener(mockUser)).thenReturn(isParticipant);
            when(mockChatRoom.isSpeaker(mockUser)).thenReturn(isParticipant);

            // when & then
            if (shouldThrowException) {
                assertThrows(CustomException.class, () -> chatRoomService.validateChatActivity(userId, roomId));
            } else {
                assertDoesNotThrow(() -> chatRoomService.validateChatActivity(userId, roomId));
            }
        }

        static Stream<Arguments> chatActivityScenarios() {
            return Stream.of(
                    // status, isParticipant, shouldThrowException
                    Arguments.of(ChatRoomStatus.ACTIVE, true, false), // 정상 케이스
                    Arguments.of(ChatRoomStatus.CLOSED, true, true),  // 비활성화된 채팅방
                    Arguments.of(ChatRoomStatus.ACTIVE, false, true)  // 권한 없는 사용자
            );
        }
    }

    @Test
    @DisplayName("채팅방 읽기 검증")
    void validateChatRead_Success() {
        // given
        when(mockChatRoom.isListener(mockUser)).thenReturn(true);

        // when & then
        assertDoesNotThrow(() -> chatRoomService.validateChatRead(userId, roomId));
    }

    @Test
    @DisplayName("채팅방 읽기 검증 실패 - 권한 없음")
    void validateChatRead_Unauthorized() {
        // given
        when(mockChatRoom.isListener(mockUser)).thenReturn(false);
        when(mockChatRoom.isSpeaker(mockUser)).thenReturn(false);

        // when & then
        assertThrows(CustomException.class, () -> chatRoomService.validateChatRead(userId, roomId));
    }


    @Test
    @DisplayName("채팅방 저장")
    void save_Success() {
        // given
        when(chatRoomRepository.save(mockChatRoom)).thenReturn(mockChatRoom);

        // when
        ChatRoom result = chatRoomService.save(mockChatRoom);

        // then
        assertNotNull(result);
        verify(chatRoomRepository).save(mockChatRoom);
    }

    @Test
    @DisplayName("채팅방 생성")
    void createChatRoom_Success() {
        // given
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(mockChatRoom);

        // when
        ChatRoom result = chatRoomService.createChatRoom(mockMatching);

        // then
        assertNotNull(result);
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    private ChatMessage createMockChatMessage(Long id) {
        ChatMessage msg = mock(ChatMessage.class);
        when(msg.getId()).thenReturn(id);
        when(msg.getChatRoom()).thenReturn(mockChatRoom);
        when(msg.getSender()).thenReturn(mockUser);
        return msg;
    }
}
