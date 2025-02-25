package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;

public interface ChatMessageService {
    ChatMessage findChatMessageById(Long messageId);
}
