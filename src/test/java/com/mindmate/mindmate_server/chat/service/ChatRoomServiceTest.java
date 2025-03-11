//package com.mindmate.mindmate_server.chat.service;
//
//import com.mindmate.mindmate_server.chat.domain.ChatMessage;
//import com.mindmate.mindmate_server.chat.domain.ChatRoom;
//import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;
//import com.mindmate.mindmate_server.chat.dto.ChatRoomDetailResponse;
//import com.mindmate.mindmate_server.chat.dto.ChatRoomResponse;
//import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
//import com.mindmate.mindmate_server.chat.repository.ChatRoomRepository;
//import com.mindmate.mindmate_server.global.exception.CustomException;
//import com.mindmate.mindmate_server.user.domain.ListenerProfile;
//import com.mindmate.mindmate_server.user.domain.RoleType;
//import com.mindmate.mindmate_server.user.domain.SpeakerProfile;
//import com.mindmate.mindmate_server.user.domain.User;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//class ChatRoomServiceTest {
//    @Mock private ChatRoomRepository chatRoomRepository;
//    @Mock private ChatMessageRepository chatMessageRepository;
//
//    @InjectMocks
//    private ChatRoomServiceImpl chatRoomService;
//
//    private Long userId;
//    private Long roomId;
//    private ChatRoom mockChatRoom;
//    private User mockUser;
//    private ListenerProfile listenerProfile;
//    private SpeakerProfile speakerProfile;
//
//    @BeforeEach
//    void setup() {
//        userId = 1L;
//        roomId = 100L;
//
//        // Mock 객체 생성
//        mockUser = mock(User.class);
//        mockChatRoom = mock(ChatRoom.class);
//        listenerProfile = mock(ListenerProfile.class);
//        speakerProfile = mock(SpeakerProfile.class);
//
//        when(mockUser.getId()).thenReturn(userId);
//        when(listenerProfile.getUser()).thenReturn(mockUser);
//        User speakerUser = mock(User.class);
//        when(speakerUser.getId()).thenReturn(2L);
//        when(speakerProfile.getUser()).thenReturn(speakerUser);
//
//        when(mockChatRoom.getId()).thenReturn(roomId);
//        when(mockChatRoom.getListener()).thenReturn(listenerProfile);
//        when(mockChatRoom.getSpeaker()).thenReturn(speakerProfile);
//
//        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mockChatRoom));
//    }
//
//    @Nested
//    @DisplayName("채팅방 조회")
//    class FindChatRoomTest {
//        @Test
//        @DisplayName("채팅방 ID로 조회 성공")
//        void findChatRoomById_Success() {
//            // when
//            ChatRoom result = chatRoomService.findChatRoomById(roomId);
//
//            // then
//            assertNotNull(result);
//            assertEquals(roomId, result.getId());
//            verify(chatRoomRepository).findById(roomId);
//        }
//
//        @Test
//        @DisplayName("채팅방 ID로 조회 실패 - 존재하지 않은 채팅방")
//        void findChatRoomById_NotFound() {
//            // given
//            Long nonExistRoomId = 999L;
//            when(chatRoomRepository.findById(nonExistRoomId)).thenReturn(Optional.empty());
//
//            // when & then
//            assertThrows(CustomException.class, () -> chatRoomService.findChatRoomById(nonExistRoomId));
//        }
//    }
//
//    @Nested
//    @DisplayName("사용자로 채팅방 목록 조회")
//    class FindChatRoomByUserTest {
//        @Test
//        @DisplayName("사용자의 채팅방 목록 조회 - pagination")
//        void getChatRoomsForUser_Success() {
//            // given
//            PageRequest pageRequest = PageRequest.of(0, 10);
//            Page<ChatRoom> mockPage = new PageImpl<>(List.of(mockChatRoom));
//            when(chatRoomRepository.findAllByParticipant(userId, pageRequest)).thenReturn(mockPage);
//
//            // when
//            Page<ChatRoomResponse> result = chatRoomService.getChatRoomsForUser(userId, pageRequest);
//
//            // then
//            assertNotNull(result);
//            assertEquals(1, result.getTotalElements());
//        }
//
//        @Test
//        @DisplayName("역할별 채팅방 목록 조회")
//        void getChatRoomsByUserRole_Success() {
//            // given
//            PageRequest pageRequest = PageRequest.of(0, 10);
//            Page<ChatRoom> mockPage = new PageImpl<>(List.of(mockChatRoom));
//            when(chatRoomRepository.findAllByParticipantAndRole(eq(userId), anyString(), eq(pageRequest)))
//                    .thenReturn(mockPage);
//
//            // when
//            Page<ChatRoomResponse> result = chatRoomService.getChatRoomsByUserRole(userId, pageRequest);
//
//            // then
//            assertNotNull(result);
//            assertEquals(1, result.getTotalElements());
//        }
//    }
//
//    @Nested
//    @DisplayName("채팅방 종료")
//    class CloseChatRoomTest {
//        @Test
//        @DisplayName("채팅방 종료 성공 - 리스너")
//        void closeChatRoom_Success_Listener() {
//            // given
//            Long listenerId = 1L;
//
//            // when
//            chatRoomService.closeChatRoom(listenerId, roomId);
//
//            // then
//            verify(mockChatRoom).close();
//        }
//
//        @Test
//        @DisplayName("채팅방 종료 성공 - 스피커")
//        void closeChatRoom_Success_Speaker() {
//            // given
//            Long speakerId = 2L;
//            User speakerUser = mock(User.class);
//            when(speakerUser.getId()).thenReturn(speakerId);
//            when(speakerProfile.getUser()).thenReturn(speakerUser);
//
//            // when
//            chatRoomService.closeChatRoom(speakerId, roomId);
//
//            // then
//            verify(mockChatRoom).close();
//        }
//
//        @Test
//        @DisplayName("채팅방 종료 실패 - 권한 없음")
//        void closeChatRoom_AccessDenied() {
//            // given
//            Long unauthorizedUserId = 3L;
//
//            // when & then
//            assertThrows(CustomException.class, () -> chatRoomService.closeChatRoom(unauthorizedUserId, roomId));
//        }
//    }
//
//    @Nested
//    @DisplayName("초기 메시지 로드")
//    class GetInitialMessagesTest {
//        @Test
//        @DisplayName("처음 입장")
//        void getInitialMessages_FirstTime() {
//            // given
//            when(mockChatRoom.getListenerLastReadMessageId()).thenReturn(0L);
//            when(chatMessageRepository.countByChatRoomId(roomId)).thenReturn(5L);
//
//            List<ChatMessage> messages = IntStream.range(1, 6)
//                    .mapToObj(i -> createMockChatMessage((long) i))
//                    .collect(Collectors.toList());
//
//            Page<ChatMessage> messagePage = new PageImpl<>(messages);
//            when(chatMessageRepository.findByChatRoomIdOrderByIdAsc(eq(roomId), any(PageRequest.class)))
//                    .thenReturn(messagePage);
//
//            // when
//            ChatRoomDetailResponse result = chatRoomService.getInitialMessages(userId, roomId, 10);
//
//            // then
//            assertNotNull(result);
//            assertEquals(5, result.getMessages().size());
//        }
//
//        @Test
//        @DisplayName("읽지 않은 메시지가 존재")
//        void getInitialMessage_NotRead() {
//            // given
//            Long lastReadMessageId = 5L;
//            when(mockChatRoom.getListenerLastReadMessageId()).thenReturn(lastReadMessageId);
//            when(chatMessageRepository.countByChatRoomId(roomId)).thenReturn(10L);
//
//            List<ChatMessage> previousMessages = IntStream.range(1, 6)
//                    .map(i -> 6 - i)
//                    .mapToObj(i -> createMockChatMessage((long) i))
//                    .collect(Collectors.toList());
//            when(chatMessageRepository.findMessagesBeforeIdLimited(eq(roomId), eq(lastReadMessageId), any(PageRequest.class)))
//                    .thenReturn(previousMessages);
//
//            List<ChatMessage> newMessages = IntStream.range(6, 11)
//                    .mapToObj(i -> createMockChatMessage((long) i))
//                    .collect(Collectors.toList());
//            when(chatMessageRepository.findByChatRoomIdAndIdGreaterThanEqualOrderByIdAsc(eq(roomId), eq(lastReadMessageId)))
//                    .thenReturn(newMessages);
//
//            Optional<ChatMessage> latestMessage = Optional.of(createMockChatMessage(10L));
//            when(chatMessageRepository.findTopByChatRoomIdOrderByIdDesc(roomId)).thenReturn(latestMessage);
//
//            // when
//            ChatRoomDetailResponse result = chatRoomService.getInitialMessages(userId, roomId, 15);
//
//            // then
//            assertNotNull(result);
//            assertEquals(10, result.getMessages().size());
//            assertEquals(1L, result.getMessages().get(0).getId());
//            assertEquals(10L, result.getMessages().get(9).getId());
//        }
//
//
//        @Test
//        @DisplayName("읽지 않은 메시지가 존재하지 않음")
//        void getInitialMessage_AllRead() {
//            // given
//            Long lastReadMessageId = 10L;
//            when(mockChatRoom.getListenerLastReadMessageId()).thenReturn(lastReadMessageId);
//            when(chatMessageRepository.countByChatRoomId(roomId)).thenReturn(10L);
//
//            List<ChatMessage> messages = IntStream.range(1, 11)
//                    .mapToObj(i -> createMockChatMessage((long) i))
//                    .collect(Collectors.toList());
//            Page<ChatMessage> messagePage = new PageImpl<>(messages);
//            when(chatMessageRepository.findByChatRoomIdOrderByIdDesc(eq(roomId), any(PageRequest.class)))
//                    .thenReturn(messagePage);
//
//            Optional<ChatMessage> latestMessage = Optional.of(createMockChatMessage(10L));
//            when(chatMessageRepository.findTopByChatRoomIdOrderByIdDesc(roomId)).thenReturn(latestMessage);
//
//            // when
//            ChatRoomDetailResponse result = chatRoomService.getInitialMessages(userId, roomId, 15);
//
//            // then
//            assertNotNull(result);
//            assertEquals(10, result.getMessages().size());
//            assertEquals(10L, result.getMessages().get(0).getId());
//            assertEquals(1L, result.getMessages().get(9).getId());
//        }
//    }
//
//    @Test
//    @DisplayName("이전 메시지 조회")
//    void getPreviousMessage() {
//        // given
//        Long messageId  = 10L;
//        int size = 5;
//        List<ChatMessage> messages = IntStream.range(5, 10)
//                .mapToObj(i -> createMockChatMessage((long) i))
//                .collect(Collectors.toList());
//        Page<ChatMessage> messagePage = new PageImpl<>(messages);
//
//        when(chatMessageRepository.findByChatRoomIdAndIdLessThanOrderByIdDesc(eq(roomId), eq(messageId), any(PageRequest.class)))
//                .thenReturn(messagePage);
//
//        // when
//        List<ChatMessageResponse> result = chatRoomService.getPreviousMessages(roomId, messageId, size);
//
//        // then
//        assertNotNull(result);
//        assertEquals(5, result.size());
//        assertEquals(9L, result.get(0).getId());
//        assertEquals(5L, result.get(4).getId());
//    }
//
//    private ChatMessage createMockChatMessage(Long id) {
//        ChatMessage msg = mock(ChatMessage.class);
//        when(msg.getId()).thenReturn(id);
//        when(msg.getChatRoom()).thenReturn(mockChatRoom);
//        when(msg.getSender()).thenReturn(mockUser);
//        when(msg.getSenderRole()).thenReturn(RoleType.ROLE_LISTENER);
//        return msg;
//    }
//}
