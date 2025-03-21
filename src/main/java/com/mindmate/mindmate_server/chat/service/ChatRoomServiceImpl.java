package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;
import com.mindmate.mindmate_server.chat.dto.ChatRoomDetailResponse;
import com.mindmate.mindmate_server.chat.dto.ChatRoomResponse;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
import com.mindmate.mindmate_server.chat.repository.ChatRoomRepository;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;

    private final ChatMessageService chatMessageService;
    private final UserService userService;

    @Override
    public ChatRoom findChatRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    @Override
    public Page<ChatRoomResponse> getChatRoomsForUser(Long userId, PageRequest pageRequest) {
        return chatRoomRepository.findAllByUserId(userId, pageRequest);
//        return chatRoomRepository.findAllByParticipant(userId, pageRequest)
//                .map(chatRoom -> ChatRoomResponse.from(chatRoom, userId));
    }

    @Override
    public Page<ChatRoomResponse> getChatRoomsByUserRole(Long userId, PageRequest pageRequest, String role) {
        return chatRoomRepository.findAllByUserIdAndRole(userId, role, pageRequest);
//        return chatRoomRepository.findAllByParticipantAndRole(userId, role, pageRequest)
//                .map(chatRoom -> ChatRoomResponse.from(chatRoom, userId));
    }

    @Override
    public ChatRoomDetailResponse getInitialMessages(Long userId, Long roomId, int size) {
        ChatRoom chatRoom = findChatRoomById(roomId);
        User user = userService.findUserById(userId);

        boolean isListener = chatRoom.isListener(user);

        Long lastReadMessageId = isListener
                ? chatRoom.getListenerLastReadMessageId()
                : chatRoom.getSpeakerLastReadMessageId();

        List<ChatMessage> messages = fetchMessages(roomId, lastReadMessageId, size);

        if (!messages.isEmpty()) {
            Long lastMessageId = messages.get(messages.size() - 1).getId();
            chatRoom.markAsRead(user, lastMessageId);
            chatRoomRepository.save(chatRoom);
        }

        return ChatRoomDetailResponse.from(chatRoom, messages, user.getId());
    }

    private List<ChatMessage> fetchMessages(Long roomId, Long lastReadMessageId, int size) {
        long totalMessages = chatMessageService.countMessagesByChatRoomId(roomId);

        if (totalMessages == 0) {
            return new ArrayList<>();
        }

        if (lastReadMessageId == 0) {
            // 첫 접속: 가장 오래된 메시지부터 표시
            return chatMessageService.findByChatRoomIdOrderByIdAsc(roomId, size);
        } else {
            // 재접속: 안읽은 메시지 처리
            Optional<ChatMessage> latestMessageOpt = chatMessageService.findLatestMessageByChatRoomId(roomId);

            if (latestMessageOpt.isPresent() && latestMessageOpt.get().getId() > lastReadMessageId) {
                List<ChatMessage> previousMessages = new ArrayList<>(
                        chatMessageService.findMessagesBeforeId(roomId, lastReadMessageId, 10)
                );
                List<ChatMessage> newMessages = chatMessageService
                        .findMessagesAfterOrEqualId(roomId, lastReadMessageId);

                Collections.reverse(previousMessages);

                List<ChatMessage> messages = new ArrayList<>();
                messages.addAll(previousMessages);
                messages.addAll(newMessages);
                return messages;
            } else {
                // 안읽은 메시지 없음: 최신 메시지 표시
                return chatMessageService.findRecentMessages(roomId, size);
            }
        }
    }

    @Override
    public List<ChatMessageResponse> getPreviousMessages(Long roomId, Long messageId, Long userId, int size) {
        // 4. 이전 메시지 페이지네이션 (스크롤 업)
        List<ChatMessage> messages = chatMessageService.findPreviousMessages(roomId, messageId, size);

        return messages.stream()
                .map(message -> ChatMessageResponse.from(message, userId))
                .collect(Collectors.toList());
    }   


    @Override
    @Transactional
    public void closeChatRoom(Long userId, Long roomId) {
        ChatRoom chatRoom = findChatRoomById(roomId);
        if (!chatRoom.getListener().getId().equals(userId) &&
                !chatRoom.getSpeaker().getId().equals(userId)) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
        chatRoom.close();
        log.info("Closed chat room {}", roomId);
    }

    @Override
    public void validateChatActivity(Long userId, Long roomId) {
         /*
          유효성 검사
          1. 해당 채팅방 참가 여부
          2. 채팅방 상태 확인
         */
        User user = userService.findUserById(userId);
        ChatRoom chatRoom = findChatRoomById(roomId);

        if (chatRoom.getChatRoomStatus() != ChatRoomStatus.ACTIVE) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        if (!chatRoom.isListener(user) && !chatRoom.isSpeaker(user)) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    @Override
    public Page<ChatRoomResponse> getChatRoomsByUserAndStatus(Long userId, PageRequest pageRequest, ChatRoomStatus status) {
        return chatRoomRepository.findAllByUserIdAndStatus(userId, status, pageRequest);
    }


    // todo: 채팅방 만들기 로직 추가
}
