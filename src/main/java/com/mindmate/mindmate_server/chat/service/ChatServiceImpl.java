package com.mindmate.mindmate_server.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.chat.dto.ChatMessageRequest;
import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
import com.mindmate.mindmate_server.chat.repository.ChatRoomRepository;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final KafkaTemplate<String, ChatMessageEvent> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisKeyManager redisKeyManager;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomService chatRoomService;
    private final UserService userService;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatPresenceService chatPresenceService;

    // todo: 채팅 관련 전체적으로 채팅방 상태에 따른 처리 추가해야함. 메시지 보내기 +

    @Override
//    @Transactional(propagation = Propagation.REQUIRED)
    public ChatMessageResponse sendMessage(Long userId, ChatMessageRequest request) {
        ChatRoom chatRoom = chatRoomService.findChatRoomById(request.getRoomId());
        User sender = userService.findUserById(userId);

        chatRoomService.validateChatActivity(sender, chatRoom);

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(request.getContent())
                .type(request.getType())
                .build();
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        chatRoom.updateLastMessageTime();

        ChatMessageEvent event = ChatMessageEvent.builder()
                .messageId(savedMessage.getId())
                .roomId(chatRoom.getId())
                .senderId(userId)
                .content(request.getContent())
                .type(request.getType())
                .timestamp(savedMessage.getCreatedAt())
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send("chat-message-topic", event.getRoomId().toString(), event);
            }
        });

        ChatMessageResponse chatMessageResponse = ChatMessageResponse.from(savedMessage, userId);

        String channel = redisKeyManager.getChatRoomChannel(chatRoom.getId());
        try {
            // Redis의 Pub/Sub 기능을 사용하여 지정한 채널에 메시지를 publish
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.error("Error serializing message", e);
        }

        return chatMessageResponse;
    }

    @Override
//    @Transactional(propagation = Propagation.REQUIRED)
    public int markAsRead(Long userId, Long roomId) {
        ChatRoom chatRoom = chatRoomService.findChatRoomById(roomId);
        User user = userService.findUserById(userId);

        validateChatRoomAccess(chatRoom, user);
        Long lastMessageId = chatMessageRepository.findTopByChatRoomIdOrderByIdDesc(roomId)
                .map(ChatMessage::getId)
                .orElse(0L);

//        boolean isListener = isUserListener(chatRoom, user);
//        if (isListener) {
//            chatRoom.markAsReadForListener(lastMessageId);
//        } else {
//            chatRoom.markAsReadForSpeaker(lastMessageId);
//        }

        chatRoom.markAsRead(user, lastMessageId);
        chatRoomRepository.saveAndFlush(chatRoom);

        chatPresenceService.resetUnreadCount(roomId, userId);
        String key = redisKeyManager.getReadStatusKey(roomId, userId);
        redisTemplate.opsForValue().set(key, LocalDateTime.now().toString());
        redisTemplate.expire(key, 1, TimeUnit.DAYS);

        Map<String, Object> readEvent = new HashMap<>();
        readEvent.put("type", "READ_STATUS");
        readEvent.put("roomId", roomId);
        readEvent.put("userId", userId);
        readEvent.put("timestamp", LocalDateTime.now());

        String channel = redisKeyManager.getChatRoomChannel(roomId);
        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(readEvent));
        } catch (JsonProcessingException e) {
            log.error("Error serializing read event", e);
        }
        return 0;
    }

    private void validateChatRoomAccess(ChatRoom chatRoom, User user) {
        boolean hasAccess = chatRoom.getListener().getId().equals(user.getId()) ||
                chatRoom.getSpeaker().getId().equals(user.getId());
        log.info("listener id: {} / speaker id: {}", chatRoom.getListener().getId(), chatRoom.getSpeaker().getId());

        if (!hasAccess) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }
}
