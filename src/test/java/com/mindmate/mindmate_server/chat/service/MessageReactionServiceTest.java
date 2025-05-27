package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.MessageReaction;
import com.mindmate.mindmate_server.chat.domain.ReactionType;
import com.mindmate.mindmate_server.chat.dto.MessageReactionResponse;
import com.mindmate.mindmate_server.chat.repository.MessageReactionRepository;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageReactionServiceTest {
    @Mock private MessageReactionRepository messageReactionRepository;
    @Mock private UserService userService;
    @Mock private ChatMessageService chatMessageService;
    @Mock private ChatRoomService chatRoomService;

    @InjectMocks
    private MessageReactionService messageReactionService;

    private Long userId;
    private Long messageId;
    private Long chatRoomId;
    private User mockUser;
    private ChatMessage mockMessage;
    private ChatRoom mockChatRoom;
    private MessageReaction mockReaction;
    private Profile mockProfile;

    @BeforeEach
    void setUp() {
        userId = 1L;
        messageId = 100L;
        chatRoomId = 10L;

        // Mock 객체 생성
        mockUser = mock(User.class);
        mockMessage = mock(ChatMessage.class);
        mockChatRoom = mock(ChatRoom.class);
        mockReaction = mock(MessageReaction.class);
        mockProfile = mock(Profile.class);

        // 기본 설정
        when(mockUser.getId()).thenReturn(userId);
        when(mockUser.getProfile()).thenReturn(mockProfile);
        when(mockProfile.getNickname()).thenReturn("TestUser");

        when(mockMessage.getId()).thenReturn(messageId);
        when(mockMessage.getChatRoom()).thenReturn(mockChatRoom);

        when(mockChatRoom.getId()).thenReturn(chatRoomId);

        when(mockReaction.getId()).thenReturn(1L);
        when(mockReaction.getMessage()).thenReturn(mockMessage);
        when(mockReaction.getUser()).thenReturn(mockUser);
        when(mockReaction.getReactionType()).thenReturn(ReactionType.LIKE);

        // 서비스 모킹
        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(chatMessageService.findChatMessageById(messageId)).thenReturn(mockMessage);
    }


    @Nested
    @DisplayName("리액션 추가 테스트")
    class AddReactionTest {
        @Test
        @DisplayName("새 리액션 추가 성공")
        void addReaction_NewReaction_Success() {
            // given
            when(messageReactionRepository.findByMessageIdAndUserId(messageId, userId))
                    .thenReturn(Optional.empty());
            when(messageReactionRepository.save(any(MessageReaction.class))).thenReturn(mockReaction);

            // when
            MessageReactionResponse response = messageReactionService.addReaction(userId, messageId, ReactionType.LIKE);

            // then
            assertNotNull(response);
            assertEquals(messageId, response.getMessageId());
            assertEquals(userId, response.getUserId());
            assertEquals(ReactionType.LIKE, response.getReactionType());
            verify(chatRoomService).validateChatActivity(userId, chatRoomId);
            verify(messageReactionRepository).save(any(MessageReaction.class));
        }

        @Test
        @DisplayName("기존 리액션과 동일한 타입 -> 리액션 삭제")
        void addReaction_SameType_RemoveReaction() {
            // given
            when(messageReactionRepository.findByMessageIdAndUserId(messageId, userId))
                    .thenReturn(Optional.of(mockReaction));
            when(mockReaction.getReactionType()).thenReturn(ReactionType.LIKE);

            // when
            MessageReactionResponse response = messageReactionService.addReaction(userId, messageId, ReactionType.LIKE);

            // then
            assertNotNull(response);
            assertNull(response.getId());
            assertEquals(messageId, response.getMessageId());
            assertEquals(userId, response.getUserId());
            assertEquals(ReactionType.LIKE, response.getReactionType());
            verify(chatRoomService).validateChatActivity(userId, chatRoomId);
            verify(messageReactionRepository).delete(mockReaction);
        }

        @Test
        @DisplayName("채팅방 접근 권한 없음")
        void addReaction_AccessDenied() {
            // given
            doThrow(new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
                    .when(chatRoomService).validateChatActivity(userId, chatRoomId);

            // when & then
            assertThrows(CustomException.class, () ->
                    messageReactionService.addReaction(userId, messageId, ReactionType.LIKE));
            verify(messageReactionRepository, never()).save(any(MessageReaction.class));
        }
    }

    @Test
    @DisplayName("메시지별 리액션 조회")
    void getReactions_Success() {
        // given
        List<MessageReaction> reactions = Arrays.asList(mockReaction);
        when(messageReactionRepository.findAllByMessageId(messageId)).thenReturn(reactions);

        // when
        List<MessageReactionResponse> result = messageReactionService.getReactions(messageId);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(messageId, result.get(0).getMessageId());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals(ReactionType.LIKE, result.get(0).getReactionType());
    }

}