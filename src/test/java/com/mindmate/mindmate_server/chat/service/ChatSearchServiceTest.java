package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatSearchResponse;
import com.mindmate.mindmate_server.chat.dto.SearchNavigationResponse;
import com.mindmate.mindmate_server.user.domain.User;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatSearchServiceTest {
    @Mock private ChatMessageService chatMessageService;
    @Mock private ChatRoomService chatRoomService;

    @InjectMocks
    private ChatSearchService chatSearchService;

    private Long userId;
    private Long roomId;
    private String keyword;

    @BeforeEach
    void setup() {
        userId = 1L;
        roomId = 100L;
        keyword = "테스트";
    }

    @Nested
    @DisplayName("메시지 검색 테스트")
    class SearchMessagesTest {
        @Test
        @DisplayName("키워드로 메시지 검색 성공")
        void searchMessages_Success() {
            // given
            List<Long> matchedIds = Arrays.asList(5L, 10L, 15L);
            Long oldestLoaded = 4L;
            Long newestLoaded = 12L;

            doNothing().when(chatRoomService).validateChatActivity(userId, roomId);
            when(chatMessageService.findMessageIdsByKeyword(roomId, keyword)).thenReturn(matchedIds);

            // when
            ChatSearchResponse response = chatSearchService.searchMessages(userId, roomId, keyword, oldestLoaded, newestLoaded);

            // then
            assertNotNull(response);
            assertEquals(matchedIds, response.getMatchedMessageIds());
            assertEquals(3, response.getTotalMatches());
            assertEquals(5L, response.getFirstVisibleMatchId());
            verify(chatRoomService).validateChatActivity(userId, roomId);
        }

        @Test
        @DisplayName("검색 결과가 현재 로드된 메시지 범위 밖에 있는 경우")
        void searchMessages_NoVisibleMatches() {
            // given
            List<Long> matchedIds = Arrays.asList(1L, 2L, 20L);
            Long oldestLoaded = 5L;
            Long newestLoaded = 15L;

            doNothing().when(chatRoomService).validateChatActivity(userId, roomId);
            when(chatMessageService.findMessageIdsByKeyword(roomId, keyword)).thenReturn(matchedIds);

            // when
            ChatSearchResponse response = chatSearchService.searchMessages(userId, roomId, keyword, oldestLoaded, newestLoaded);

            // then
            assertNotNull(response);
            assertEquals(matchedIds, response.getMatchedMessageIds());
            assertEquals(3, response.getTotalMatches());
            assertNull(response.getFirstVisibleMatchId());
        }

        @Test
        @DisplayName("검색 결과가 없는 경우")
        void searchMessages_NoResults() {
            // given
            List<Long> matchedIds = Collections.emptyList();
            Long oldestLoaded = 5L;
            Long newestLoaded = 15L;

            doNothing().when(chatRoomService).validateChatActivity(userId, roomId);
            when(chatMessageService.findMessageIdsByKeyword(roomId, keyword)).thenReturn(matchedIds);

            // when
            ChatSearchResponse response = chatSearchService.searchMessages(userId, roomId, keyword, oldestLoaded, newestLoaded);

            // then
            assertNotNull(response);
            assertTrue(response.getMatchedMessageIds().isEmpty());
            assertEquals(0, response.getTotalMatches());
            assertNull(response.getFirstVisibleMatchId());
        }

        @Test
        @DisplayName("로드된 메시지 범위가 null인 경우")
        void searchMessages_NullMessageRange() {
            // given
            List<Long> matchedIds = Arrays.asList(5L, 10L, 15L);

            doNothing().when(chatRoomService).validateChatActivity(userId, roomId);
            when(chatMessageService.findMessageIdsByKeyword(roomId, keyword)).thenReturn(matchedIds);

            // when
            ChatSearchResponse response = chatSearchService.searchMessages(
                    userId, roomId, keyword, null, null);

            // then
            assertNotNull(response);
            assertEquals(matchedIds, response.getMatchedMessageIds());
            assertEquals(3, response.getTotalMatches());
            assertNull(response.getFirstVisibleMatchId());
        }

    }

    @Nested
    @DisplayName("검색 결과 탐색 테스트")
    class NavigateToSearchResultTest {
        @Test
        @DisplayName("이전 메시지 로드가 필요한 경우")
        void navigateToSearchResult_LoadPreviousMessages() {
            // given
            Long targetMessageId = 3L;
            Long oldestLoadedMessageId = 5L;
            List<Long> matchedIds = Arrays.asList(1L, 3L, 7L, 10L);
            List<ChatMessage> previousMessages = createMockMessages(3L, 4L);

            doNothing().when(chatRoomService).validateChatActivity(userId, roomId);
            when(chatMessageService.findByRoomIdAndIdBetween(roomId, targetMessageId, oldestLoadedMessageId - 1))
                    .thenReturn(previousMessages);
            when(chatMessageService.findMessageIdsByKeyword(roomId, keyword)).thenReturn(matchedIds);

            // when
            SearchNavigationResponse response = chatSearchService.navigateToSearchResult(
                    userId, roomId, keyword, targetMessageId, oldestLoadedMessageId);

            // then
            assertNotNull(response);
            assertEquals(targetMessageId, response.getTargetMessageId());
            assertEquals(2, response.getAdditionalMessages().size());
            assertTrue(response.isHasMoreResults());
            assertEquals(1, response.getCurrentMatchIndex());
            assertEquals(4, response.getTotalMatches());
        }

        @Test
        @DisplayName("이전 메시지 로드가 필요 없는 경우")
        void navigateToSearchResult_NoAdditionalMessagesNeeded() {
            // given
            Long targetMessageId = 7L;
            Long oldestLoadedMessageId = 5L;
            List<Long> matchedIds = Arrays.asList(1L, 3L, 7L, 10L);

            doNothing().when(chatRoomService).validateChatActivity(userId, roomId);
            when(chatMessageService.findMessageIdsByKeyword(roomId, keyword)).thenReturn(matchedIds);

            // when
            SearchNavigationResponse response = chatSearchService.navigateToSearchResult(
                    userId, roomId, keyword, targetMessageId, oldestLoadedMessageId);

            // then
            assertNotNull(response);
            assertEquals(targetMessageId, response.getTargetMessageId());
            assertTrue(response.getAdditionalMessages().isEmpty());
            assertTrue(response.isHasMoreResults());
            assertEquals(2, response.getCurrentMatchIndex());
            assertEquals(4, response.getTotalMatches());
        }

        @Test
        @DisplayName("마지막 검색 결과인 경우")
        void navigateToSearchResult_LastResult() {
            // given
            Long targetMessageId = 1L;
            Long oldestLoadedMessageId = 5L;
            List<Long> matchedIds = Arrays.asList(1L, 3L, 7L, 10L);
            List<ChatMessage> previousMessages = createMockMessages(1L, 4L);

            doNothing().when(chatRoomService).validateChatActivity(userId, roomId);
            when(chatMessageService.findByRoomIdAndIdBetween(roomId, targetMessageId, oldestLoadedMessageId - 1))
                    .thenReturn(previousMessages);
            when(chatMessageService.findMessageIdsByKeyword(roomId, keyword)).thenReturn(matchedIds);

            // when
            SearchNavigationResponse response = chatSearchService.navigateToSearchResult(
                    userId, roomId, keyword, targetMessageId, oldestLoadedMessageId);

            // then
            assertNotNull(response);
            assertEquals(targetMessageId, response.getTargetMessageId());
            assertEquals(4, response.getAdditionalMessages().size());
            assertFalse(response.isHasMoreResults());
            assertEquals(0, response.getCurrentMatchIndex());
            assertEquals(4, response.getTotalMatches());
        }

        @Test
        @DisplayName("검색 결과에 없는 메시지 ID인 경우")
        void navigateToSearchResult_InvalidTargetId() {
            // given
            Long targetMessageId = 20L;
            Long oldestLoadedMessageId = 5L;
            List<Long> matchedIds = Arrays.asList(1L, 3L, 7L, 10L);

            doNothing().when(chatRoomService).validateChatActivity(userId, roomId);
            when(chatMessageService.findMessageIdsByKeyword(roomId, keyword)).thenReturn(matchedIds);

            // when
            SearchNavigationResponse response = chatSearchService.navigateToSearchResult(
                    userId, roomId, keyword, targetMessageId, oldestLoadedMessageId);

            // then
            assertNotNull(response);
            assertEquals(targetMessageId, response.getTargetMessageId());
            assertTrue(response.getAdditionalMessages().isEmpty());
            assertFalse(response.isHasMoreResults());
            assertEquals(-1, response.getCurrentMatchIndex());
            assertEquals(4, response.getTotalMatches());
        }
    }

    private List<ChatMessage> createMockMessages(Long fromId, Long toId) {
        return LongStream.rangeClosed(fromId, toId)
                .mapToObj(id -> {
                    ChatMessage msg = mock(ChatMessage.class);
                    when(msg.getId()).thenReturn(id);
                    when(msg.getContent()).thenReturn("Message " + id);

                    ChatRoom chatRoom = mock(ChatRoom.class);
                    when(chatRoom.getId()).thenReturn(roomId);
                    when(msg.getChatRoom()).thenReturn(chatRoom);

                    User sender = mock(User.class);
                    when(sender.getId()).thenReturn(2L);
                    when(msg.getSender()).thenReturn(sender);

                    return msg;
                })
                .collect(Collectors.toList());
    }
}