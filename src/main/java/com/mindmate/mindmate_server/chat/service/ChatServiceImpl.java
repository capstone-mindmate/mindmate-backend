package com.mindmate.mindmate_server.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.chat.dto.ChatMessageRequest;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final KafkaTemplate<String, ChatMessageEvent> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private final ChatRoomService chatRoomService;
    private final UserService userService;

    @Override
    public void sendMessage(Long userId, ChatMessageRequest request) {
        ChatRoom chatRoom = chatRoomService.findChatRoomById(request.getRoomId());
        User user = userService.findUserById(userId);

        ChatMessageEvent event = ChatMessageEvent.builder()
                .roomId(chatRoom.getId())
                .senderId(userId)
                .senderRole(user.getCurrentRole())
                .content(request.getContent())
                .type(request.getType())
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send("chat-message-topic", event.getRoomId().toString(), event);
        log.info("Chat message sent to Kafka: {}", event);
    }

    @Override
    public void markAsRead(Long userId, Long roomId) {
        String key = "chat:room:" + roomId + ":read:" + userId;
        redisTemplate.opsForValue().set(key, LocalDateTime.now().toString());

        Map<String, Object> readEvent = new HashMap<>();
        readEvent.put("type", "READ_STATUS");
        readEvent.put("roomId", roomId);
        readEvent.put("userId", userId);
        readEvent.put("timestamp", LocalDateTime.now());

        String channel = "chat:room:" + roomId;
        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(readEvent));
        } catch (JsonProcessingException e) {
            log.error("Error serializing read event", e);
        }
    }

    @Override
    public void updateUserStatus(Long userId, boolean isOnline, Long activeRoomId) {
        String statusKey = "user:status:" + userId;
        Map<String, Object> status = new HashMap<>();
        status.put("online", isOnline);
        status.put("activeRoomId", activeRoomId);
        status.put("lastActive", LocalDateTime.now());

        redisTemplate.opsForHash().putAll(statusKey, status);

        if (isOnline) {
            redisTemplate.expire(statusKey, 30, TimeUnit.MINUTES);
        }

        String channel = "user:status:" + userId;
        redisTemplate.convertAndSend(channel, status);

    }
}
