package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;
import com.mindmate.mindmate_server.chat.dto.ChatRoomDetailResponse;
import com.mindmate.mindmate_server.chat.dto.ChatRoomResponse;
import com.mindmate.mindmate_server.chat.repository.ChatRoomRepository;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.matching.domain.Matching;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatRoomServiceTest {
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMessageService chatMessageService;
    @Mock private UserService userService;

    @InjectMocks
    private ChatRoomServiceImpl chatRoomService;

    private Long userId;
    private Long roomId;
    private ChatRoom mockChatRoom;
    private User mockUser;

    @BeforeEach
    void setup() {
        userId = 1L;
        roomId = 100L;

        // Mock 객체 생성
        mockUser = mock(User.class);
        mockChatRoom = mock(ChatRoom.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockChatRoom.getId()).thenReturn(roomId);

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mockChatRoom));
        when(userService.findUserById(userId)).thenReturn(mockUser);

        Matching mockMatching = mock(Matching.class);
        when(mockMatching.getId()).thenReturn(1L);
        when(mockChatRoom.getMatching()).thenReturn(mockMatching);

        Profile mockProfile = mock(Profile.class);
        when(mockProfile.getNickname()).thenReturn("Test User");
        when(mockUser.getProfile()).thenReturn(mockProfile);

        when(chatRoomRepository.findAllByParticipant(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(mockChatRoom)));
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

        // when
        chatRoomService.closeChatRoom(userId, roomId);

        // then
        verify(mockChatRoom).requestClosure(mockUser);
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
        @Test
        @DisplayName("첫 입장 - 메시지가 없는 경우")
        void getInitialMessages_NoMessages() {
            // given
            when(mockChatRoom.isListener(mockUser)).thenReturn(true);
            when(mockChatRoom.getListenerLastReadMessageId()).thenReturn(0L);
            when(chatMessageService.countMessagesByChatRoomId(roomId)).thenReturn(0L);

            // when
            ChatRoomDetailResponse result = chatRoomService.getInitialMessages(userId, roomId, 10);

            // then
            assertNotNull(result);
            assertTrue(result.getMessages().isEmpty());
        }

        @Test
        @DisplayName("첫 입장 - 모든 메시지 로드")
        void getInitialMessages_FirstAccess() {
            // given
            when(mockChatRoom.isListener(mockUser)).thenReturn(true);
            when(mockChatRoom.getListenerLastReadMessageId()).thenReturn(0L);
            when(chatMessageService.countMessagesByChatRoomId(roomId)).thenReturn(5L);

            List<ChatMessage> messages = IntStream.range(1, 6)
                    .mapToObj(i -> createMockChatMessage((long) i))
                    .collect(Collectors.toList());
            when(chatMessageService.findAllByChatRoomIdOrderByIdAsc(roomId)).thenReturn(messages);

            // when
            ChatRoomDetailResponse result = chatRoomService.getInitialMessages(userId, roomId, 10);

            // then
            assertNotNull(result);
            assertEquals(5, result.getMessages().size());
            verify(mockChatRoom).markAsRead(mockUser, 5L);
            verify(chatRoomRepository).save(mockChatRoom);
        }

        @Test
        @DisplayName("재접속 - 읽지 않은 메시지가 존재")
        void getInitialMessages_WithUnreadMessages() {
            // given
            when(mockChatRoom.isListener(mockUser)).thenReturn(true);
            when(mockChatRoom.getListenerLastReadMessageId()).thenReturn(5L);
            when(chatMessageService.countMessagesByChatRoomId(roomId)).thenReturn(10L);

            ChatMessage latestMessage = createMockChatMessage(10L);
            when(chatMessageService.findLatestMessageByChatRoomId(roomId)).thenReturn(Optional.of(latestMessage));

            List<ChatMessage> previousMessages = IntStream.range(1, 6)
                    .mapToObj(i -> createMockChatMessage((long) i))
                    .collect(Collectors.toList());
            when(chatMessageService.findMessagesBeforeId(eq(roomId), eq(5L), eq(10))).thenReturn(previousMessages);

            List<ChatMessage> newMessages = IntStream.range(6, 11)
                    .mapToObj(i -> createMockChatMessage((long) i))
                    .collect(Collectors.toList());
            when(chatMessageService.findMessagesAfterOrEqualId(roomId, 5L)).thenReturn(newMessages);

            // when
            ChatRoomDetailResponse result = chatRoomService.getInitialMessages(userId, roomId, 10);

            // then
            assertNotNull(result);
            assertEquals(10, result.getMessages().size());
            verify(mockChatRoom).markAsRead(mockUser, 10L);
            verify(chatRoomRepository).save(mockChatRoom);
        }


        @Test
        @DisplayName("재접속 - 읽지 않은 메시지가 존재하지 않음")
        void getInitialMessages_AllRead() {
            // given
            when(mockChatRoom.isListener(mockUser)).thenReturn(true);
            when(mockChatRoom.getListenerLastReadMessageId()).thenReturn(10L);
            when(chatMessageService.countMessagesByChatRoomId(roomId)).thenReturn(10L);

            ChatMessage latestMessage = createMockChatMessage(10L);
            when(chatMessageService.findLatestMessageByChatRoomId(roomId)).thenReturn(Optional.of(latestMessage));

            List<ChatMessage> recentMessages = IntStream.range(1, 11)
                    .mapToObj(i -> createMockChatMessage((long) i))
                    .collect(Collectors.toList());
            when(chatMessageService.findRecentMessages(roomId, 10)).thenReturn(recentMessages);

            // when
            ChatRoomDetailResponse result = chatRoomService.getInitialMessages(userId, roomId, 10);

            // then
            assertNotNull(result);
            assertEquals(10, result.getMessages().size());
            verify(mockChatRoom).markAsRead(mockUser, 10L);
            verify(chatRoomRepository).save(mockChatRoom);
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
        @Test
        @DisplayName("유효한 채팅방 접근")
        void validateChatActivity_Success() {
            // given
            when(mockChatRoom.getChatRoomStatus()).thenReturn(ChatRoomStatus.ACTIVE);
            when(mockChatRoom.isListener(mockUser)).thenReturn(true);

            // when & then
            assertDoesNotThrow(() -> chatRoomService.validateChatActivity(userId, roomId));
        }

        @Test
        @DisplayName("비활성화된 채팅방 접근")
        void validateChatActivity_InactiveRoom() {
            // given
            when(mockChatRoom.getChatRoomStatus()).thenReturn(ChatRoomStatus.CLOSED);
            when(mockChatRoom.isListener(mockUser)).thenReturn(true);

            // when & then
            assertThrows(CustomException.class, () -> chatRoomService.validateChatActivity(userId, roomId));
        }

        @Test
        @DisplayName("권한 없는 사용자 접근")
        void validateChatActivity_Unauthorized() {
            // given
            when(mockChatRoom.getChatRoomStatus()).thenReturn(ChatRoomStatus.ACTIVE);
            when(mockChatRoom.isListener(mockUser)).thenReturn(false);
            when(mockChatRoom.isSpeaker(mockUser)).thenReturn(false);

            // when & then
            assertThrows(CustomException.class, () -> chatRoomService.validateChatActivity(userId, roomId));
        }
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

    private ChatMessage createMockChatMessage(Long id) {
        ChatMessage msg = mock(ChatMessage.class);
        when(msg.getId()).thenReturn(id);
        when(msg.getChatRoom()).thenReturn(mockChatRoom);
        when(msg.getSender()).thenReturn(mockUser);
        return msg;
    }
}
