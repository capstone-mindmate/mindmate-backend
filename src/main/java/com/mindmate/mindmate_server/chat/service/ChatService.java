package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.dto.ChatMessageRequest;
import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;

public interface ChatService {
    ChatMessageResponse sendMessage(Long userId, ChatMessageRequest request);

    int markAsRead(Long userId, Long roomId);

    void publishMessageEvent(ChatMessage savedMessage, Long recipientId, boolean recipientActive, String plainContent);
}
