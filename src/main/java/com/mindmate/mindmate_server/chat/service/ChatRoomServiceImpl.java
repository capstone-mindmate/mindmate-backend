package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.ChatRoomCloseType;
import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.chat.dto.*;
import com.mindmate.mindmate_server.chat.repository.ChatRoomRepository;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;

    private final ChatMessageService chatMessageService;
    private final UserService userService;
    private final NotificationService notificationService;

    private final KafkaTemplate<String ,ChatRoomCloseEvent> kafkaTemplate;

    @Override
    public ChatRoom findChatRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    @Override
    public Page<ChatRoomResponse> getChatRoomsForUser(Long userId, PageRequest pageRequest) {
        return chatRoomRepository.findAllByUserId(userId, pageRequest);
    }

    @Override
    public Page<ChatRoomResponse> getChatRoomsByUserRole(Long userId, PageRequest pageRequest, String role) {
        return chatRoomRepository.findAllByUserIdAndRole(userId, role, pageRequest);
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

        return ChatRoomDetailResponse.from(chatRoom, messages, user);
    }

    @Override
    public List<ChatMessageResponse> getPreviousMessages(Long roomId, Long messageId, Long userId, int size) {
        List<ChatMessage> messages = chatMessageService.findPreviousMessages(roomId, messageId, size);

        return messages.stream()
                .map(message -> ChatMessageResponse.from(message, userId))
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public void closeChatRoom(Long userId, Long roomId) {
        validateChatActivity(userId, roomId); // active일 때만 close 요청 가능하지..맞지

        User user = userService.findUserById(userId);
        ChatRoom chatRoom = findChatRoomById(roomId);
        chatRoom.requestClosure(user);
        save(chatRoom);

        User recipient = chatRoom.isListener(user) ? chatRoom.getSpeaker() : chatRoom.getListener();
        ChatRoomNotificationEvent event = ChatRoomNotificationEvent.builder()
                .recipientId(recipient.getId())
                .chatRoomId(chatRoom.getId())
                .closeType(ChatRoomCloseType.REQUEST)
                .build();

        notificationService.processNotification(event);
    }

    @Override
    public void validateChatActivity(Long userId, Long roomId) {
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
    public void validateChatRead(Long userId, Long roomId) {
        User user = userService.findUserById(userId);
        ChatRoom chatRoom = findChatRoomById(roomId);

        if (!chatRoom.isListener(user) && !chatRoom.isSpeaker(user)) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    @Override
    public Page<ChatRoomResponse> getChatRoomsByUserAndStatus(Long userId, PageRequest pageRequest, ChatRoomStatus status) {
        return chatRoomRepository.findAllByUserIdAndStatus(userId, status, pageRequest);
    }

    @Override
    @Transactional
    public void rejectCloseChatRoom(Long userId, Long roomId) {
        User user = userService.findUserById(userId);
        ChatRoom chatRoom = findChatRoomById(roomId);

        if (chatRoom.getChatRoomStatus() != ChatRoomStatus.CLOSE_REQUEST) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_NOT_REQUESTED_CLOSE);
        }

        if (chatRoom.isClosureRequester(user)) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_CANNOT_ACCEPT_OWN);
        }

        chatRoom.rejectClosure();
        save(chatRoom);

        User requester = chatRoom.isClosureRequester(chatRoom.getListener()) ? chatRoom.getListener() : chatRoom.getSpeaker();

        ChatRoomNotificationEvent event = ChatRoomNotificationEvent.builder()
                .recipientId(requester.getId())
                .chatRoomId(chatRoom.getId())
                .closeType(ChatRoomCloseType.REJECT)
                .build();
        notificationService.processNotification(event);
    }

    @Override
    @Transactional
    public void acceptCloseChatRoom(Long userId, Long roomId) {
        User user = userService.findUserById(userId);
        ChatRoom chatRoom = findChatRoomById(roomId);

        if (chatRoom.getChatRoomStatus() != ChatRoomStatus.CLOSE_REQUEST) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_NOT_REQUESTED_CLOSE);
        }

        if (chatRoom.isClosureRequester(user)) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_CANNOT_ACCEPT_OWN);
        }
        chatRoom.acceptClosure();
        User speaker = chatRoom.getSpeaker();
        User listener = chatRoom.getListener();
        save(chatRoom);

        ChatRoomCloseEvent event = ChatRoomCloseEvent.builder()
                .chatRoomId(chatRoom.getId())
                .speakerId(speaker.getId())
                .listenerId(listener.getId())
                .closedAt(chatRoom.getClosedAt())
                .matchingId(chatRoom.getMatching().getId())
                .build();

        publishChatRoomCloseEvent(event);
    }

    @Override
    @Transactional
    public ChatRoom save(ChatRoom chatRoom) {
        return chatRoomRepository.save(chatRoom);
    }

    @Override
    public ChatRoom createChatRoom(Matching matching) {
        ChatRoom chatRoom = ChatRoom.builder()
                .matching(matching)
                .build();

        return chatRoomRepository.save(chatRoom);
    }

    private List<ChatMessage> fetchMessages(Long roomId, Long lastReadMessageId, int size) {
        long totalMessages = chatMessageService.countMessagesByChatRoomId(roomId);

        if (totalMessages == 0) {
            return new ArrayList<>();
        }

        if (lastReadMessageId == 0) {
            // 첫 접속: 가장 오래된 메시지부터 표시
            return chatMessageService.findAllByChatRoomIdOrderByIdAsc(roomId);
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

    private void publishChatRoomCloseEvent(ChatRoomCloseEvent event) {
        kafkaTemplate.send("chat-room-close-topic", event.getChatRoomId().toString(), event);
    }

}
