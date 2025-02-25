package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;

public interface ChatRoomService {

    ChatRoom findChatRoomById(Long roomId);
}
