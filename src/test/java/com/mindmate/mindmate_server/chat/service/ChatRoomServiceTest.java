package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.chat.domain.MessageType;
import com.mindmate.mindmate_server.chat.dto.*;
import com.mindmate.mindmate_server.chat.repository.ChatRoomRepository;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.service.ResilientEventPublisher;
import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.service.RedisMatchingService;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import com.mindmate.mindmate_server.review.repository.ReviewRepository;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.ProfileImage;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatRoomServiceTest {
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMessageService chatMessageService;
    @Mock private UserService userService;
    @Mock private NotificationService notificationService;
    @Mock private RedisMatchingService redisMatchingService;
    @Mock private ResilientEventPublisher eventPublisher;
    @Mock private ReviewRepository reviewRepository;

    @InjectMocks
    private ChatRoomServiceImpl chatRoomService;

    private static final Long USER_ID = 1L;
    private static final Long SPEAKER_ID = 2L;
    private static final Long LISTENER_ID = 3L;
    private static final Long ROOM_ID = 100L;
    private static final Long MATCHING_ID = 1L;

    private ChatRoom mockChatRoom;
    private User mockUser;
    private User mockSpeaker;
    private User mockListener;
    private Matching mockMatching;
    private Profile mockProfile;
    private Profile mockSpeakerProfile;
    private Profile mockListenerProfile;
    private ProfileImage mockProfileImage;

    @BeforeEach
    void setup() {
        setupMockObjects();
        setupMockBehaviors();
    }

    private void setupMockObjects() {
        mockUser = mock(User.class);
        mockSpeaker = mock(User.class);
        mockListener = mock(User.class);
        mockChatRoom = mock(ChatRoom.class);
        mockMatching = mock(Matching.class);
        mockProfile = mock(Profile.class);
        mockSpeakerProfile = mock(Profile.class);
        mockListenerProfile = mock(Profile.class);
        mockProfileImage = mock(ProfileImage.class);
    }

    private void setupMockBehaviors() {
        // User Mock 설정
        when(mockUser.getId()).thenReturn(USER_ID);
        when(mockSpeaker.getId()).thenReturn(SPEAKER_ID);
        when(mockListener.getId()).thenReturn(LISTENER_ID);

        // Profile Mock 설정 - 더 상세하게
        when(mockUser.getProfile()).thenReturn(mockProfile);
        when(mockSpeaker.getProfile()).thenReturn(mockSpeakerProfile);
        when(mockListener.getProfile()).thenReturn(mockListenerProfile);

        when(mockProfile.getNickname()).thenReturn("TestUser");
        when(mockSpeakerProfile.getNickname()).thenReturn("Speaker");
        when(mockListenerProfile.getNickname()).thenReturn("Listener");

        // ProfileImage Mock 설정 추가
        when(mockProfile.getProfileImage()).thenReturn(mockProfileImage);
        when(mockSpeakerProfile.getProfileImage()).thenReturn(mockProfileImage);
        when(mockListenerProfile.getProfileImage()).thenReturn(mockProfileImage);
        when(mockProfileImage.getImageUrl()).thenReturn("https://example.com/image.jpg");

        // ChatRoom Mock 설정
        when(mockChatRoom.getId()).thenReturn(ROOM_ID);
        when(mockChatRoom.getMatching()).thenReturn(mockMatching);
        when(mockChatRoom.getListener()).thenReturn(mockListener);
        when(mockChatRoom.getSpeaker()).thenReturn(mockSpeaker);
        when(mockChatRoom.getChatRoomStatus()).thenReturn(ChatRoomStatus.ACTIVE);

        // 삭제 상태 기본값 설정
        when(mockChatRoom.isDeletedByListener()).thenReturn(false);
        when(mockChatRoom.isDeletedBySpeaker()).thenReturn(false);

        // Matching Mock 설정
        when(mockMatching.getId()).thenReturn(MATCHING_ID);
        when(mockMatching.isAnonymous()).thenReturn(false);

        // Repository Mock 설정
        when(chatRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(mockChatRoom));
        when(userService.findUserById(USER_ID)).thenReturn(mockUser);
        when(userService.findUserById(SPEAKER_ID)).thenReturn(mockSpeaker);
        when(userService.findUserById(LISTENER_ID)).thenReturn(mockListener);
        when(reviewRepository.existsByChatRoomAndReviewer(any(), any())).thenReturn(false);
    }

    private ChatMessage createMockChatMessage(Long id) {
        ChatMessage msg = mock(ChatMessage.class);
        when(msg.getId()).thenReturn(id);
        when(msg.getChatRoom()).thenReturn(mockChatRoom);
        when(msg.getSender()).thenReturn(mockUser);
        when(msg.getContent()).thenReturn("Test message " + id);
        when(msg.getType()).thenReturn(MessageType.TEXT);
        when(msg.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(msg.getMessageReactions()).thenReturn(Collections.emptyList());
        when(msg.isCustomForm()).thenReturn(false);
        when(msg.getCustomForm()).thenReturn(null);
        when(msg.getEmoticon()).thenReturn(null);
        return msg;
    }

    @Nested
    @DisplayName("채팅방 조회")
    class FindChatRoomTest {

        @Test
        @DisplayName("채팅방 ID로 조회 성공")
        void findChatRoomById_Success() {
            // when
            ChatRoom result = chatRoomService.findChatRoomById(ROOM_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(ROOM_ID);
            verify(chatRoomRepository).findById(ROOM_ID);
        }

        @Test
        @DisplayName("채팅방 ID로 조회 실패 - 존재하지 않는 채팅방")
        void findChatRoomById_NotFound() {
            // given
            Long nonExistRoomId = 999L;
            when(chatRoomRepository.findById(nonExistRoomId)).thenReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> chatRoomService.findChatRoomById(nonExistRoomId));
            assertThat(exception.getErrorCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("채팅방 목록 조회")
    class GetChatRoomsTest {

        @Test
        @DisplayName("사용자의 채팅방 목록 조회")
        void getChatRoomsForUser_Success() {
            // given
            PageRequest pageRequest = PageRequest.of(0, 10);
            Page<ChatRoomResponse> mockPage = new PageImpl<>(List.of(new ChatRoomResponse()));
            when(chatRoomRepository.findAllByUserId(USER_ID, pageRequest)).thenReturn(mockPage);

            // when
            Page<ChatRoomResponse> result = chatRoomService.getChatRoomsForUser(USER_ID, pageRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(chatRoomRepository).findAllByUserId(USER_ID, pageRequest);
        }

        @Test
        @DisplayName("역할별 채팅방 목록 조회")
        void getChatRoomsByUserRole_Success() {
            // given
            PageRequest pageRequest = PageRequest.of(0, 10);
            String role = "ROLE_LISTENER";
            Page<ChatRoomResponse> mockPage = new PageImpl<>(List.of(new ChatRoomResponse()));
            when(chatRoomRepository.findAllByUserIdAndRole(USER_ID, role, pageRequest)).thenReturn(mockPage);

            // when
            Page<ChatRoomResponse> result = chatRoomService.getChatRoomsByUserRole(USER_ID, pageRequest, role);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(chatRoomRepository).findAllByUserIdAndRole(USER_ID, role, pageRequest);
        }

        @Test
        @DisplayName("상태별 채팅방 목록 조회")
        void getChatRoomsByUserAndStatus_Success() {
            // given
            PageRequest pageRequest = PageRequest.of(0, 10);
            ChatRoomStatus status = ChatRoomStatus.ACTIVE;
            Page<ChatRoomResponse> mockPage = new PageImpl<>(List.of(new ChatRoomResponse()));
            when(chatRoomRepository.findAllByUserIdAndStatus(USER_ID, status, pageRequest)).thenReturn(mockPage);

            // when
            Page<ChatRoomResponse> result = chatRoomService.getChatRoomsByUserAndStatus(USER_ID, pageRequest, status);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(chatRoomRepository).findAllByUserIdAndStatus(USER_ID, status, pageRequest);
        }
    }

    @Nested
    @DisplayName("초기 메시지 로드")
    class GetInitialMessagesTest {

        @ParameterizedTest
        @DisplayName("사용자 역할별 삭제 상태 확인")
        @MethodSource("deletedRoomScenarios")
        void getInitialMessages_DeletedRoom_ThrowsException(
                String description,
                boolean isListener,
                boolean isDeletedByListener,
                boolean isDeletedBySpeaker) {
            // given
            when(mockChatRoom.isListener(mockUser)).thenReturn(isListener);
            when(mockChatRoom.isDeletedByListener()).thenReturn(isDeletedByListener);
            when(mockChatRoom.isDeletedBySpeaker()).thenReturn(isDeletedBySpeaker);

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> chatRoomService.getInitialMessages(USER_ID, ROOM_ID, 10));
            assertThat(exception.getErrorCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_DELETED);
        }

        static Stream<Arguments> deletedRoomScenarios() {
            return Stream.of(
                    Arguments.of("리스너가 삭제한 채팅방에 리스너 접근", true, true, false),
                    Arguments.of("스피커가 삭제한 채팅방에 스피커 접근", false, false, true)
            );
        }

        @ParameterizedTest
        @DisplayName("메시지 로드 및 리뷰 작성 상태 확인")
        @MethodSource("messageLoadScenarios")
        void getInitialMessages_Scenarios(
                String description,
                boolean isListener,
                long lastReadMessageId,
                long totalMessages,
                boolean hasLatestMessage,
                boolean hasWrittenReview,
                int expectedMessageCount) {
            // given
            when(mockChatRoom.isListener(mockUser)).thenReturn(isListener);
            when(mockChatRoom.isDeletedByListener()).thenReturn(false);
            when(mockChatRoom.isDeletedBySpeaker()).thenReturn(false);

            if (isListener) {
                when(mockChatRoom.getListenerLastReadMessageId()).thenReturn(lastReadMessageId);
            } else {
                when(mockChatRoom.getSpeakerLastReadMessageId()).thenReturn(lastReadMessageId);
            }

            when(chatMessageService.countMessagesByChatRoomId(ROOM_ID)).thenReturn(totalMessages);

            if (hasLatestMessage && totalMessages > 0) {
                ChatMessage latestMessage = createMockChatMessage(totalMessages);
                when(chatMessageService.findLatestMessageByChatRoomId(ROOM_ID)).thenReturn(Optional.of(latestMessage));
            }

            when(reviewRepository.existsByChatRoomAndReviewer(mockChatRoom, mockUser))
                    .thenReturn(hasWrittenReview);

            setupMessageMocking(lastReadMessageId, totalMessages);

            // when
            ChatRoomDetailResponse result = chatRoomService.getInitialMessages(USER_ID, ROOM_ID, 10);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMessages()).hasSize(expectedMessageCount);
            assertThat(result.isWriteReview()).isEqualTo(hasWrittenReview);

            if (totalMessages > 0 && expectedMessageCount > 0) {
                verify(mockChatRoom).markAsRead(mockUser, totalMessages);
                verify(chatRoomRepository).save(mockChatRoom);
            }

            verify(reviewRepository).existsByChatRoomAndReviewer(mockChatRoom, mockUser);
        }

        static Stream<Arguments> messageLoadScenarios() {
            return Stream.of(
                    Arguments.of("메시지가 없는 경우 - 리뷰 미작성", true, 0L, 0L, false, false, 0),
                    Arguments.of("메시지가 없는 경우 - 리뷰 작성됨", true, 0L, 0L, false, true, 0),
                    Arguments.of("첫 입장 - 모든 메시지 로드 - 리뷰 미작성", true, 0L, 5L, false, false, 5),
                    Arguments.of("첫 입장 - 모든 메시지 로드 - 리뷰 작성됨", true, 0L, 5L, false, true, 5),
                    Arguments.of("재접속 - 읽지 않은 메시지 존재 - 리뷰 미작성", true, 5L, 10L, true, false, 10),
                    Arguments.of("재접속 - 읽지 않은 메시지 존재 - 리뷰 작성됨", true, 5L, 10L, true, true, 10),
                    Arguments.of("재접속 - 모든 메시지 읽음 - 리뷰 미작성", false, 10L, 10L, true, false, 10),
                    Arguments.of("재접속 - 모든 메시지 읽음 - 리뷰 작성됨", false, 10L, 10L, true, true, 10)
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
                when(chatMessageService.findAllByChatRoomIdOrderByIdAsc(ROOM_ID)).thenReturn(allMessages);
            } else if (lastReadMessageId < totalMessages) {
                // 읽지 않은 메시지가 있는 케이스
                List<ChatMessage> previousMessages = IntStream.range(1, (int) lastReadMessageId + 1)
                        .mapToObj(i -> createMockChatMessage((long) i))
                        .collect(Collectors.toList());
                when(chatMessageService.findMessagesBeforeId(eq(ROOM_ID), eq(lastReadMessageId), eq(10)))
                        .thenReturn(previousMessages);

                List<ChatMessage> newMessages = IntStream.rangeClosed((int) lastReadMessageId + 1, (int) totalMessages)
                        .mapToObj(i -> createMockChatMessage((long) i))
                        .collect(Collectors.toList());
                when(chatMessageService.findMessagesAfterOrEqualId(ROOM_ID, lastReadMessageId))
                        .thenReturn(newMessages);
            } else {
                // 모든 메시지를 읽은 케이스
                List<ChatMessage> recentMessages = IntStream.range(1, (int) totalMessages + 1)
                        .mapToObj(i -> createMockChatMessage((long) i))
                        .collect(Collectors.toList());
                when(chatMessageService.findRecentMessages(ROOM_ID, 10)).thenReturn(recentMessages);
            }
        }
    }

    @Nested
    @DisplayName("이전 메시지 조회")
    class GetPreviousMessagesTest {

        @Test
        @DisplayName("이전 메시지 조회 성공")
        void getPreviousMessages_Success() {
            // given
            Long messageId = 10L;
            int size = 5;
            when(mockChatRoom.isListener(mockUser)).thenReturn(true);
            when(mockChatRoom.isSpeaker(mockUser)).thenReturn(false);
            when(mockChatRoom.isDeletedByListener()).thenReturn(false);
            when(mockChatRoom.isDeletedBySpeaker()).thenReturn(false);

            List<ChatMessage> messages = IntStream.range(5, 10)
                    .mapToObj(i -> createMockChatMessage((long) i))
                    .collect(Collectors.toList());
            when(chatMessageService.findPreviousMessages(ROOM_ID, messageId, size)).thenReturn(messages);

            // when
            List<ChatMessageResponse> result = chatRoomService.getPreviousMessages(ROOM_ID, messageId, USER_ID, size);

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(5);
            verify(chatMessageService).findPreviousMessages(ROOM_ID, messageId, size);
        }

        @Test
        @DisplayName("이전 메시지 조회 - 권한 없음")
        void getPreviousMessages_Unauthorized() {
            // given
            when(mockChatRoom.isListener(mockUser)).thenReturn(false);
            when(mockChatRoom.isSpeaker(mockUser)).thenReturn(false);

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> chatRoomService.getPreviousMessages(ROOM_ID, 10L, USER_ID, 5));
            assertThat(exception.getErrorCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("채팅방 종료 관리")
    class ChatRoomCloseTest {

        @Test
        @DisplayName("채팅방 종료 요청 성공")
        void closeChatRoom_Success() {
            // given
            when(mockChatRoom.isListener(mockUser)).thenReturn(true);
            when(mockChatRoom.isSpeaker(mockUser)).thenReturn(false);

            // when
            chatRoomService.closeChatRoom(USER_ID, ROOM_ID);

            // then
            verify(mockChatRoom).requestClosure(mockUser);
            verify(chatRoomRepository).save(mockChatRoom);
            verify(notificationService).processNotification(any(ChatRoomNotificationEvent.class));
        }

        @Test
        @DisplayName("채팅방 종료 요청 수락 성공")
        void acceptCloseChatRoom_Success() {
            // given
            when(mockChatRoom.getChatRoomStatus()).thenReturn(ChatRoomStatus.CLOSE_REQUEST);
            when(mockChatRoom.isClosureRequester(mockUser)).thenReturn(false);
            when(mockChatRoom.getClosedAt()).thenReturn(LocalDateTime.now());

            // 트랜잭션 동기화 활성화
            TransactionSynchronizationManager.initSynchronization();

            try {
                // when
                chatRoomService.acceptCloseChatRoom(USER_ID, ROOM_ID);

                // then
                verify(mockChatRoom).acceptClosure();
                verify(chatRoomRepository).save(mockChatRoom);

                // afterCommit 실행하여 이벤트 발행 확인
                TransactionSynchronizationManager.getSynchronizations()
                        .forEach(TransactionSynchronization::afterCommit);

                verify(eventPublisher).publishEvent(eq("chat-room-close-topic"), anyString(), any(ChatRoomCloseEvent.class));
                verify(redisMatchingService).decrementUserActiveMatchingCount(SPEAKER_ID);
                verify(redisMatchingService).decrementUserActiveMatchingCount(LISTENER_ID);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("채팅방 종료 요청 거절 성공")
        void rejectCloseChatRoom_Success() {
            // given
            when(mockChatRoom.getChatRoomStatus()).thenReturn(ChatRoomStatus.CLOSE_REQUEST);
            when(mockChatRoom.isClosureRequester(mockUser)).thenReturn(false);
            when(mockChatRoom.isClosureRequester(mockListener)).thenReturn(true);

            // when
            chatRoomService.rejectCloseChatRoom(USER_ID, ROOM_ID);

            // then
            verify(mockChatRoom).rejectClosure();
            verify(chatRoomRepository).save(mockChatRoom);
            verify(notificationService).processNotification(any(ChatRoomNotificationEvent.class));
        }

        @ParameterizedTest
        @DisplayName("채팅방 종료 처리 예외 시나리오")
        @MethodSource("closeExceptionScenarios")
        void handleCloseChatRoom_ExceptionScenarios(
                String description,
                ChatRoomStatus status,
                boolean isRequester,
                ChatErrorCode expectedError) {
            // given
            when(mockChatRoom.getChatRoomStatus()).thenReturn(status);
            when(mockChatRoom.isClosureRequester(mockUser)).thenReturn(isRequester);

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> chatRoomService.acceptCloseChatRoom(USER_ID, ROOM_ID));
            assertThat(exception.getErrorCode()).isEqualTo(expectedError);
        }

        static Stream<Arguments> closeExceptionScenarios() {
            return Stream.of(
                    Arguments.of("종료 요청 상태가 아님", ChatRoomStatus.ACTIVE, false, ChatErrorCode.CHAT_ROOM_NOT_REQUESTED_CLOSE),
                    Arguments.of("자신의 요청을 처리하려 함", ChatRoomStatus.CLOSE_REQUEST, true, ChatErrorCode.CHAT_ROOM_CANNOT_ACCEPT_OWN)
            );
        }
    }

    @Nested
    @DisplayName("채팅방 삭제")
    class DeleteChatRoomTest {

        @Test
        @DisplayName("리스너가 채팅방 삭제 성공")
        void deleteChatRoomForUser_ListenerSuccess() {
            // given
            when(mockChatRoom.getChatRoomStatus()).thenReturn(ChatRoomStatus.CLOSED);
            when(mockChatRoom.isListener(mockUser)).thenReturn(true);
            when(mockChatRoom.isSpeaker(mockUser)).thenReturn(false);
            when(mockChatRoom.isDeletedByListener()).thenReturn(false);
            when(mockChatRoom.isDeletedBySpeaker()).thenReturn(false);

            // when
            chatRoomService.deleteChatRoomForUser(USER_ID, ROOM_ID);

            // then
            verify(mockChatRoom).markDeletedBy(InitiatorType.LISTENER);
            verify(chatRoomRepository).save(mockChatRoom);
        }

        @Test
        @DisplayName("스피커가 채팅방 삭제 성공")
        void deleteChatRoomForUser_SpeakerSuccess() {
            // given
            when(mockChatRoom.getChatRoomStatus()).thenReturn(ChatRoomStatus.CLOSED);
            when(mockChatRoom.isListener(mockUser)).thenReturn(false);
            when(mockChatRoom.isSpeaker(mockUser)).thenReturn(true);
            when(mockChatRoom.isDeletedByListener()).thenReturn(false);
            when(mockChatRoom.isDeletedBySpeaker()).thenReturn(false);

            // when
            chatRoomService.deleteChatRoomForUser(USER_ID, ROOM_ID);

            // then
            verify(mockChatRoom).markDeletedBy(InitiatorType.SPEAKER);
            verify(chatRoomRepository).save(mockChatRoom);
        }

        @Test
        @DisplayName("양쪽 모두 삭제 시 상태 변경")
        void deleteChatRoomForUser_BothDeleted() {
            // given
            when(mockChatRoom.getChatRoomStatus()).thenReturn(ChatRoomStatus.CLOSED);
            when(mockChatRoom.isListener(mockUser)).thenReturn(true);
            when(mockChatRoom.isSpeaker(mockUser)).thenReturn(false);
            // 이미 삭제된 상태로 설정
            when(mockChatRoom.isDeletedByListener()).thenReturn(false); // 아직 삭제 안됨
            when(mockChatRoom.isDeletedBySpeaker()).thenReturn(true);    // 스피커는 이미 삭제함

            // markDeletedBy 호출 후 상태 변경 시뮬레이션
            doAnswer(invocation -> {
                when(mockChatRoom.isDeletedByListener()).thenReturn(true);
                return null;
            }).when(mockChatRoom).markDeletedBy(InitiatorType.LISTENER);

            // when
            chatRoomService.deleteChatRoomForUser(USER_ID, ROOM_ID);

            // then
            verify(mockChatRoom).markDeletedBy(InitiatorType.LISTENER);
            verify(mockChatRoom).updateChatRoomStatus(ChatRoomStatus.DELETED);
            verify(chatRoomRepository).save(mockChatRoom);
        }

        @ParameterizedTest
        @DisplayName("채팅방 삭제 예외 시나리오")
        @MethodSource("deleteExceptionScenarios")
        void deleteChatRoomForUser_ExceptionScenarios(
                String description,
                ChatRoomStatus status,
                boolean isParticipant,
                ChatErrorCode expectedError) {
            // given
            when(mockChatRoom.getChatRoomStatus()).thenReturn(status);
            when(mockChatRoom.isListener(mockUser)).thenReturn(isParticipant);
            when(mockChatRoom.isSpeaker(mockUser)).thenReturn(isParticipant);

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> chatRoomService.deleteChatRoomForUser(USER_ID, ROOM_ID));
            assertThat(exception.getErrorCode()).isEqualTo(expectedError);
        }

        static Stream<Arguments> deleteExceptionScenarios() {
            return Stream.of(
                    Arguments.of("종료되지 않은 채팅방", ChatRoomStatus.ACTIVE, true, ChatErrorCode.CHAT_ROOM_NOT_CLOSED),
                    Arguments.of("권한 없는 사용자", ChatRoomStatus.CLOSED, false, ChatErrorCode.CHAT_ROOM_ACCESS_DENIED)
            );
        }
    }

    @Nested
    @DisplayName("채팅방 검증")
    class ValidationTest {

        @ParameterizedTest
        @DisplayName("채팅방 활동 검증 시나리오")
        @MethodSource("chatActivityScenarios")
        void validateChatActivity_Scenarios(
                String description,
                ChatRoomStatus status,
                boolean isParticipant,
                boolean shouldThrowException) {
            // given
            when(mockChatRoom.getChatRoomStatus()).thenReturn(status);
            when(mockChatRoom.isListener(mockUser)).thenReturn(isParticipant);
            when(mockChatRoom.isSpeaker(mockUser)).thenReturn(isParticipant);

            // when & then
            if (shouldThrowException) {
                CustomException exception = assertThrows(CustomException.class,
                        () -> chatRoomService.validateChatActivity(USER_ID, ROOM_ID));
                assertThat(exception.getErrorCode()).isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
            } else {
                assertDoesNotThrow(() -> chatRoomService.validateChatActivity(USER_ID, ROOM_ID));
            }
        }

        static Stream<Arguments> chatActivityScenarios() {
            return Stream.of(
                    Arguments.of("정상 케이스", ChatRoomStatus.ACTIVE, true, false),
                    Arguments.of("비활성화된 채팅방", ChatRoomStatus.CLOSED, true, true),
                    Arguments.of("권한 없는 사용자", ChatRoomStatus.ACTIVE, false, true)
            );
        }

        @ParameterizedTest
        @DisplayName("채팅방 읽기 검증 시나리오")
        @MethodSource("chatReadScenarios")
        void validateChatRead_Scenarios(
                String description,
                boolean isListener,
                boolean isSpeaker,
                boolean isDeletedByListener,
                boolean isDeletedBySpeaker,
                boolean shouldThrowException,
                ChatErrorCode expectedError) {
            // given
            when(mockChatRoom.isListener(mockUser)).thenReturn(isListener);
            when(mockChatRoom.isSpeaker(mockUser)).thenReturn(isSpeaker);
            when(mockChatRoom.isDeletedByListener()).thenReturn(isDeletedByListener);
            when(mockChatRoom.isDeletedBySpeaker()).thenReturn(isDeletedBySpeaker);

            // when & then
            if (shouldThrowException) {
                CustomException exception = assertThrows(CustomException.class,
                        () -> chatRoomService.validateChatRead(USER_ID, ROOM_ID));
                assertThat(exception.getErrorCode()).isEqualTo(expectedError);
            } else {
                assertDoesNotThrow(() -> chatRoomService.validateChatRead(USER_ID, ROOM_ID));
            }
        }

        static Stream<Arguments> chatReadScenarios() {
            return Stream.of(
                    Arguments.of("정상 케이스 - 리스너", true, false, false, false, false, null),
                    Arguments.of("정상 케이스 - 스피커", false, true, false, false, false, null),
                    Arguments.of("권한 없는 사용자", false, false, false, false, true, ChatErrorCode.CHAT_ROOM_ACCESS_DENIED),
                    Arguments.of("리스너가 삭제한 채팅방에 리스너 접근", true, false, true, false, true, ChatErrorCode.CHAT_ROOM_NOT_FOUND),
                    Arguments.of("스피커가 삭제한 채팅방에 스피커 접근", false, true, false, true, true, ChatErrorCode.CHAT_ROOM_NOT_FOUND)
            );
        }
    }

    @Nested
    @DisplayName("채팅방 생성 및 저장")
    class CreateAndSaveTest {

        @Test
        @DisplayName("채팅방 생성 성공")
        void createChatRoom_Success() {
            // given
            when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(mockChatRoom);

            // when
            ChatRoom result = chatRoomService.createChatRoom(mockMatching);

            // then
            assertThat(result).isNotNull();
            verify(chatRoomRepository).save(any(ChatRoom.class));
        }

        @Test
        @DisplayName("채팅방 저장 성공")
        void save_Success() {
            // given
            when(chatRoomRepository.save(mockChatRoom)).thenReturn(mockChatRoom);

            // when
            ChatRoom result = chatRoomService.save(mockChatRoom);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(mockChatRoom);
            verify(chatRoomRepository).save(mockChatRoom);
        }
    }
}
