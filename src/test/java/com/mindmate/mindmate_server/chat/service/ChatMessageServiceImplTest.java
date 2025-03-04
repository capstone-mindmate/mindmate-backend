package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
import com.mindmate.mindmate_server.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceImplTest {
    @Mock private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatMessageServiceImpl chatMessageService;

    @Nested
    @DisplayName("메시지 ID로 조회")
    class FindChatMessageByIdTest {
        @Test
        @DisplayName("메시지 ID로 조회 성공")
        void findChatMessageById_Success() {
            // given
            Long messageId = 1L;
            ChatMessage mockMessage = mock(ChatMessage.class);
            when(mockMessage.getId()).thenReturn(messageId);
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(mockMessage));

            // when
            ChatMessage result = chatMessageService.findChatMessageById(messageId);

            // then
            assertNotNull(result);
            assertEquals(messageId, result.getId());
        }

        @Test
        @DisplayName("메시지 ID로 조회 실패")
        void findChatMessageById_NoutFount() {
            // given
            Long messageId = 999L;
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(CustomException.class, () -> chatMessageService.findChatMessageById(messageId));
        }
    }
}
