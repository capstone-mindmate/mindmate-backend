package com.mindmate.mindmate_server.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmate.mindmate_server.chat.dto.ChatEvent;
import com.mindmate.mindmate_server.chat.dto.ChatEventType;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatEventPublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisKeyManager redisKeyManager;

    public <T> void publishChatRoomEvent(Long roomId, ChatEventType type, T data) {
        String channel = redisKeyManager.getChatRoomChannel(roomId);
        ChatEvent<T> event = ChatEvent.of(type, data);

        try {
            String eventJson = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(channel, eventJson);
        } catch (JsonProcessingException e) {
            // todo: 재시도 로직
            log.error("Error publishing event to Redis: {}", e.getMessage());
        }
    }

    public <T> void publishUserEvent(Long userId, ChatEventType type, T data) {
        String channel = redisKeyManager.getUserStatusChannel(userId);
        ChatEvent<T> event = ChatEvent.of(type, data);

        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.error("Error publishing event to Redis: {}", e.getMessage());
        }
    }
}
