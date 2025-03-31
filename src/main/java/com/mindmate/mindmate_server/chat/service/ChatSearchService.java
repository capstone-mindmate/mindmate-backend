package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;
import com.mindmate.mindmate_server.chat.dto.ChatSearchResponse;
import com.mindmate.mindmate_server.chat.dto.SearchNavigationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatSearchService {
    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;

    @Transactional(readOnly = true)
    public ChatSearchResponse searchMessages(
            Long userId, Long roomId, String keyword,
            Long oldestLoadedMessageId, Long newestLoadedMessageId) {
        chatRoomService.validateChatActivity(userId, roomId);

        List<Long> matchedMessageIds = chatMessageService.findMessageIdsByKeyword(roomId, keyword);

        Long firstVisibleMatchId = null;
        if (oldestLoadedMessageId != null && newestLoadedMessageId != null) {
            for (Long id : matchedMessageIds) {
                if (id >= oldestLoadedMessageId && id <= newestLoadedMessageId) {
                    firstVisibleMatchId = id;
                    break;
                }
            }
        }

        return ChatSearchResponse.builder()
                .matchedMessageIds(matchedMessageIds)
                .totalMatches(matchedMessageIds.size())
                .firstVisibleMatchId(firstVisibleMatchId)
                .build();
    }

    @Transactional(readOnly = true)
    public SearchNavigationResponse navigateToSearchResult(
            Long userId, Long roomId, String keyword, Long targetMessageId, Long oldestLoadedMessageId) {
        chatRoomService.validateChatActivity(userId, roomId);
        List<ChatMessageResponse> additionalMessages = Collections.emptyList();

        // 현재 로드된 메시지들보다 이전에 존재
        if (targetMessageId < oldestLoadedMessageId) {
            List<ChatMessage> messages = chatMessageService.findByRoomIdAndIdBetween(
                    roomId, targetMessageId, oldestLoadedMessageId - 1);

            additionalMessages = messages.stream()
                    .map(message -> ChatMessageResponse.from(message, userId))
                    .collect(Collectors.toList());
        }

        List<Long> matchedIds = chatMessageService.findMessageIdsByKeyword(roomId, keyword);
        int currentIdx = matchedIds.indexOf(targetMessageId);

        return SearchNavigationResponse.builder()
                .targetMessageId(targetMessageId)
                .additionalMessages(additionalMessages)
                .hasMoreResults(currentIdx > 0)
                .currentMatchIndex(currentIdx)
                .totalMatches(matchedIds.size())
                .build();
    }

}
