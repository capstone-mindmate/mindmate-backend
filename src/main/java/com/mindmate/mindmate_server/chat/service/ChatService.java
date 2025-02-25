package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.dto.ChatMessageRequest;

public interface ChatService {
    void sendMessage(Long userId, ChatMessageRequest request);

    void markAsRead(Long userId, Long roomId);

    void updateUserStatus(Long userId, boolean isOnline, Long activeRoomId);

}
