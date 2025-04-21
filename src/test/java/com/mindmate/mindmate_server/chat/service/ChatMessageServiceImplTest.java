package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
import com.mindmate.mindmate_server.global.exception.CustomException;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatMessageServiceImplTest {
    @Mock private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatMessageServiceImpl chatMessageService;

    private Long roomId;
    private Long messageId;
    private ChatMessage mockMessage;
    private List<ChatMessage> mockMessages;

    @BeforeEach
    void setup() {
        roomId = 1L;
        messageId = 1L;
        mockMessage = mock(ChatMessage.class);
        when(mockMessage.getId()).thenReturn(messageId);

        mockMessages = IntStream.range(1, 6)
                .mapToObj(i -> {
                    ChatMessage msg = mock(ChatMessage.class);
                    when(msg.getId()).thenReturn((long) i);
                    return msg;
                })
                .collect(Collectors.toList());
    }

    @Nested
    @DisplayName("메시지 ID로 조회")
    class FindChatMessageByIdTest {
        @Test
        @DisplayName("메시지 ID로 조회 성공")
        void findChatMessageById_Success() {
            // given
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(mockMessage));

            // when
            ChatMessage result = chatMessageService.findChatMessageById(messageId);

            // then
            assertNotNull(result);
            assertEquals(messageId, result.getId());
        }

        @Test
        @DisplayName("메시지 ID로 조회 실패")
        void findChatMessageById_NotFound() {
            // given
            Long messageId = 999L;
            when(chatMessageRepository.findById(messageId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(CustomException.class, () -> chatMessageService.findChatMessageById(messageId));
        }
    }

    @Nested
    @DisplayName("채팅방 메시지 수 조회")
    class CountMessageByChatRoomIdTest {
        @Test
        @DisplayName("채팅방 메시지 수 조회 성공")
        void countMessageByChatRoomId_Success() {
            // given
            when(chatMessageRepository.countByChatRoomId(roomId)).thenReturn(5L);

            // when
            long count = chatMessageService.countMessagesByChatRoomId(roomId);

            // then
            assertEquals(5L, count);
        }
    }

    @Nested
    @DisplayName("채팅방 메시지 조회 (오랜된 순)")
    class FindAllByChatRoomIdOrderByIdAscTest {
        @Test
        @DisplayName("채팅방 메시지 조회 성공")
        void findAllByChatROomIdOrderByIdAsc_Success() {
            // given
            when(chatMessageRepository.findByChatRoomIdOrderByIdAsc(roomId)).thenReturn(mockMessages);

            // when
            List<ChatMessage> result = chatMessageService.findAllByChatRoomIdOrderByIdAsc(roomId);

            // then
            assertNotNull(result);
            assertEquals(5, result.size());
        }
    }

    @Nested
    @DisplayName("최신 메시지 조회")
    class FindLatestMessageByChatRoomIdTest {
        @Test
        @DisplayName("최신 메시지 조회 성공")
        void findLatestMessageByChatRoomId_Success() {
            // given
            when(chatMessageRepository.findTopByChatRoomIdOrderByIdDesc(roomId)).thenReturn(Optional.of(mockMessage));

            // when
            Optional<ChatMessage> result = chatMessageService.findLatestMessageByChatRoomId(roomId);

            // then
            assertTrue(result.isPresent());
            assertEquals(messageId, result.get().getId());
        }

        @Test
        @DisplayName("최근 메시지 없음")
        void findLatestMessageByChatRoomId_Empty() {
            // given
            when(chatMessageRepository.findTopByChatRoomIdOrderByIdDesc(roomId)).thenReturn(Optional.empty());

            // when
            Optional<ChatMessage> result = chatMessageService.findLatestMessageByChatRoomId(roomId);

            // then
            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("특정 메시지 Id 이전 메시지 조회")
    class FindMessageBeforeIdTest {
        @Test
        @DisplayName("특정 메시지 ID 이전 메시지 조회 성공")
        void findMessagesBeforeId_Success() {
            // given
            int size = 3;
            when(chatMessageRepository.findMessagesBeforeIdLimited(roomId, messageId, PageRequest.of(0, size)))
                    .thenReturn(mockMessages.subList(0, size));

            // when
            List<ChatMessage> result = chatMessageService.findMessagesBeforeId(roomId, messageId, size);

            // then
            assertNotNull(result);
            assertEquals(size, result.size());
        }
    }

    @Nested
    @DisplayName("특정 메시지 ID 이상 메시지 조회")
    class FindMessagesAfterOrEqualIdTest {
        @Test
        @DisplayName("트정 메시지 Id 이상 메시지 조회 성공")
        void findMessagesAfterOrEqualId_Success() {
            // given
            when(chatMessageRepository.findByChatRoomIdAndIdGreaterThanEqualOrderByIdAsc(roomId, messageId))
                    .thenReturn(mockMessages);

            // when
            List<ChatMessage> result = chatMessageService.findMessagesAfterOrEqualId(roomId, messageId);

            // then
            assertNotNull(result);
            assertEquals(5, result.size());
        }
    }

    @Nested
    @DisplayName("최근 메시지 조회")
    class FindRecentMessagesTest {
        @Test
        @DisplayName("최근 메시지 조회 성공")
        void findRecentMessages_Success() {
            // given
            int size = 3;
            Page<ChatMessage> mockPage = new PageImpl<>(mockMessages.subList(0, size));
            when(chatMessageRepository.findByChatRoomIdOrderByIdDesc(roomId, PageRequest.of(0, size)))
                    .thenReturn(mockPage);

            // when
            List<ChatMessage> result = chatMessageService.findRecentMessages(roomId, size);

            // then
            assertNotNull(result);
            assertEquals(size, result.size());
        }
    }

    @Nested
    @DisplayName("이전 메시지 조회")
    class FindPreviousMessagesTest {
        @Test
        @DisplayName("이전 메시지 조회 성공")
        void findPreviousMessages_Success() {
            // given
            int size = 3;
            Page<ChatMessage> mockPage = new PageImpl<>(mockMessages.subList(0, size));
            when(chatMessageRepository.findByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, messageId, PageRequest.of(0, size)))
                    .thenReturn(mockPage);

            // when
            List<ChatMessage> result = chatMessageService.findPreviousMessages(roomId, messageId, size);

            // then
            assertNotNull(result);
            assertEquals(size, result.size());
        }
    }

    @Nested
    @DisplayName("메시지 저장")
    class SaveTest {
        @Test
        @DisplayName("메시지 저장 성공")
        void save_Success() {
            // given
            when(chatMessageRepository.save(mockMessage)).thenReturn(mockMessage);

            // when
            ChatMessage result = chatMessageService.save(mockMessage);

            // then
            assertNotNull(result);
            assertEquals(messageId, result.getId());
        }
    }

    @Nested
    @DisplayName("키워드로 메시지 ID 조회")
    class FindMessageIdsByKeywordTest {
        @Test
        @DisplayName("키워드로 메시지 ID 조회 성공")
        void findMessageIdsByKeyword_Success() {
            // given
            String keyword = "test";
            List<Long> messageIds = List.of(1L, 2L, 3L);
            when(chatMessageRepository.findMessageIdsByKeyword(roomId, keyword)).thenReturn(messageIds);

            // when
            List<Long> result = chatMessageService.findMessageIdsByKeyword(roomId, keyword);

            // then
            assertNotNull(result);
            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("키워드가 null일 때 예외 발생")
        void findMessageIdsByKeyword_NullKeyword() {
            // when & then
            assertThrows(CustomException.class, () -> chatMessageService.findMessageIdsByKeyword(roomId, null));
        }

        @Test
        @DisplayName("키워드가 빈 문자열일 때 예외 발생")
        void findMessageIdsByKeyword_EmptyKeyword() {
            // when & then
            assertThrows(CustomException.class, () -> chatMessageService.findMessageIdsByKeyword(roomId, ""));
        }

        @Test
        @DisplayName("roomId가 null일 때 예외 발생")
        void findMessageIdsByKeyword_NullRoomId() {
            // when & then
            assertThrows(CustomException.class, () -> chatMessageService.findMessageIdsByKeyword(null, "test"));
        }
    }

    @Nested
    @DisplayName("ID 범위 내 메시지 조회")
    class FindByRoomIdAndIdBetweenTest {
        @Test
        @DisplayName("ID 범위 내 메시지 조회 성공")
        void findByRoomIdAndIdBetween_Success() {
            // given
            Long fromId = 1L;
            Long toId = 3L;
            when(chatMessageRepository.findByRoomIdAndIdBetween(roomId, fromId, toId))
                    .thenReturn(mockMessages.subList(0, 3));

            // when
            List<ChatMessage> result = chatMessageService.findByRoomIdAndIdBetween(roomId, fromId, toId);

            // then
            assertNotNull(result);
            assertEquals(3, result.size());
        }
    }
}
