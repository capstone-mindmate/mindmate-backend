package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatRoomDetailResponse;
import com.mindmate.mindmate_server.chat.dto.ChatRoomResponse;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
import com.mindmate.mindmate_server.chat.repository.ChatRoomRepository;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    private final UserService userService;

    @Override
    public ChatRoom findChatRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    @Override
    public Page<ChatRoomResponse> getChatRoomsForUser(Long userId, PageRequest pageRequest) {
        return chatRoomRepository.findAllByParticipant(userId, pageRequest)
                .map(chatRoom -> ChatRoomResponse.from(chatRoom, userId));
    }

    @Override
    public Page<ChatRoomResponse> getChatRoomsByUserRole(Long userId, PageRequest pageRequest, RoleType roleType) {
        return chatRoomRepository.findAllByParticipantAndRole(userId, roleType, pageRequest)
                .map(chatRoom -> ChatRoomResponse.from(chatRoom, userId));
    }

    @Override
    public ChatRoomDetailResponse getChatRoomWithMessages(Long userId, Long roomId, PageRequest pageRequest) {
        ChatRoom chatRoom = findChatRoomById(roomId);
        boolean isListener = chatRoom.getListener().getUser().getId().equals(userId);

        Long lastReadMessage = isListener
                ? chatRoom.getListenerLastReadMessageId()
                : chatRoom.getSpeakerLastReadMessageId();

        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdAndIdGreaterThan(roomId, lastReadMessage);

        return ChatRoomDetailResponse.from(chatRoom, messages);
    }

    @Override
    @Transactional
    public void closeChatRoom(Long userId, Long roomId) {
        ChatRoom chatRoom = findChatRoomById(roomId);
        if (!chatRoom.getListener().getUser().getId().equals(userId) &&
                !chatRoom.getSpeaker().getUser().getId().equals(userId)) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
        chatRoom.close();
        log.info("Closed chat room {}", roomId);
    }
}
