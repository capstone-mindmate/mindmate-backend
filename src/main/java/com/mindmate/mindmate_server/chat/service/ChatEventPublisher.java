package com.mindmate.mindmate_server.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmate.mindmate_server.chat.dto.ChatEvent;
import com.mindmate.mindmate_server.chat.dto.ChatEventType;
import com.mindmate.mindmate_server.chat.util.WebSocketDestinationResolver;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatEventPublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisKeyManager redisKeyManager;
    private final WebSocketDestinationResolver webSocketDestinationResolver;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int MAX_RETRIES = 3;

    public <T> void publishChatRoomEvent(Long roomId, ChatEventType type, T data) {
        String channel = redisKeyManager.getChatRoomChannel(roomId);
        ChatEvent<T> event = ChatEvent.of(type, data);

        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                String eventJson = objectMapper.writeValueAsString(event);
                redisTemplate.convertAndSend(channel, eventJson);
                return;
            } catch (JsonProcessingException e) {
                log.error("Error publishing event to Redis (attempt {}): {}", retryCount + 1, e.getMessage());
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(1000 * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        // 모든 재시도 실패 시 WebSocket으로 직접 전송
        try {
            String destination = webSocketDestinationResolver.getDestinationByEventType(roomId.toString(), type.name());
            messagingTemplate.convertAndSend(destination, objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            log.error("Failed to deliver message via fallback mechanism: {}", e.getMessage());
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
