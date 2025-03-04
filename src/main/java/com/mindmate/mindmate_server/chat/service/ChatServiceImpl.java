package com.mindmate.mindmate_server.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.chat.dto.ChatMessageRequest;
import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
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

    private final ChatRoomService chatRoomService;
    private final UserService userService;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatPresenceService chatPresenceService;


    @Override
    public ChatMessageResponse sendMessage(Long userId, ChatMessageRequest request) {
        ChatRoom chatRoom = chatRoomService.findChatRoomById(request.getRoomId());
        User user = userService.findUserById(userId);

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(user)
                .senderRole(user.getCurrentRole())
                .content(request.getContent())
                .type(request.getType())
                .build();
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        ChatMessageEvent event = ChatMessageEvent.builder()
                .messageId(savedMessage.getId())
                .roomId(chatRoom.getId())
                .senderId(userId)
                .senderRole(user.getCurrentRole())
                .content(request.getContent())
                .type(request.getType())
                .timestamp(savedMessage.getCreatedAt())
                .build();

        // todo : redis 채널에 메시지 발행?
        ChatMessageResponse chatMessageResponse = ChatMessageResponse.builder()
                .id(savedMessage.getId())
                .senderId(userId)
                .senderRole(user.getCurrentRole())
                .content(request.getContent())
                .type(request.getType())
                .createdAt(LocalDateTime.now())
                .roomId(chatRoom.getId())
                .build();
        String channel = redisKeyManager.getChatRoomChannel(chatRoom.getId());
        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.error("Error serializing message", e);
        }

        kafkaTemplate.send("chat-message-topic", event.getRoomId().toString(), event);
        return chatMessageResponse;
    }

    @Override
    public int markAsRead(Long userId, Long roomId) {
        ChatRoom chatRoom = chatRoomService.findChatRoomById(roomId);
        User user = userService.findUserById(userId);

        log.info("Sender user {}", user);
        log.info("ChatRoom Listener info {}", chatRoom.getListener().getUser());
        log.info("ChatRoom Speaker info {}", chatRoom.getSpeaker().getUser());

        validateChatRoomAccess(chatRoom, user);
        boolean isListener = isUserListener(chatRoom, user);
        Long lastMessageId = chatMessageRepository.findTopByChatRoomIdOrderByIdDesc(roomId)
                .map(ChatMessage::getId)
                .orElse(0L);

        if (isListener) {
            chatRoom.markAsReadForListener(lastMessageId);
        } else {
            chatRoom.markAsReadForSpeaker(lastMessageId);
        }

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
        boolean hasAccess = chatRoom.getListener().getUser().getId().equals(user.getId()) ||
                chatRoom.getSpeaker().getUser().getId().equals(user.getId());

        if (!hasAccess) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    private boolean isUserListener(ChatRoom chatRoom, User user) {
        return chatRoom.getListener().getUser().equals(user);
    }
}
