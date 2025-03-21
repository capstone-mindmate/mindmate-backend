package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;
import com.mindmate.mindmate_server.chat.dto.ChatSearchResponse;
import com.mindmate.mindmate_server.chat.dto.SearchNavigationResponse;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatSearchService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public ChatSearchResponse searchMessages(
            Long userId, Long roomId, String keyword,
            Long oldestLoadedMessageId, Long newestLoadedMessageId) {
        chatRoomService.validateChatActivity(userId, roomId);

        List<Long> matchedMessageIds = chatMessageRepository.findMessageIdsByKeyword(roomId, keyword);

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
        List<ChatMessageResponse> additionalMessages = Collections.emptyList();

        if (targetMessageId < oldestLoadedMessageId) {
            List<ChatMessage> messages = chatMessageRepository.findByRoomIdAndIdBetween(
                    roomId, targetMessageId, oldestLoadedMessageId - 1);

            additionalMessages = messages.stream()
                    .map(message -> ChatMessageResponse.from(message, userId))
                    .collect(Collectors.toList());
        }

        List<Long> matchedIds = chatMessageRepository.findMessageIdsByKeyword(roomId, keyword);
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
