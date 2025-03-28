package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;

import java.util.List;
import java.util.Optional;

public interface ChatMessageService {
    ChatMessage findChatMessageById(Long messageId);

    long countMessagesByChatRoomId(Long roomId);

    List<ChatMessage> findAllByChatRoomIdOrderByIdAsc(Long roomId);

    Optional<ChatMessage> findLatestMessageByChatRoomId(Long roomId);

    List<ChatMessage> findMessagesBeforeId(Long roomId, Long messageId, int size);

    List<ChatMessage> findMessagesAfterOrEqualId(Long roomId, Long messageId);

    List<ChatMessage> findRecentMessages(Long roomId, int size);

    List<ChatMessage> findPreviousMessages(Long roomId, Long messageId, int size);

    ChatMessage save(ChatMessage chatMessage);
}
