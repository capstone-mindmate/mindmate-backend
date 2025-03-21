package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;
import com.mindmate.mindmate_server.chat.dto.ChatRoomDetailResponse;
import com.mindmate.mindmate_server.chat.dto.ChatRoomResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public interface ChatRoomService {
    // 채팅방 조회

    ChatRoom findChatRoomById(Long roomId);
    Page<ChatRoomResponse> getChatRoomsForUser(Long userId, PageRequest pageRequest);
    Page<ChatRoomResponse> getChatRoomsByUserRole(Long userId, PageRequest pageRequest, String role);

    ChatRoomDetailResponse getInitialMessages(Long userId, Long roomId, int size);
    List<ChatMessageResponse> getPreviousMessages(Long roomId, Long messageId, Long userId, int size);

    void closeChatRoom(Long userId, Long roomId);

    void validateChatActivity(Long userId, Long roomId);

    Page<ChatRoomResponse> getChatRoomsByUserAndStatus(Long userId, PageRequest lastMessageTime, ChatRoomStatus status);

    void rejectCloseChatRoom(Long userId, Long roomId);

    void acceptCloseChatRoom(Long userId, Long roomId);

}
