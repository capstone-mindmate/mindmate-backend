package com.mindmate.mindmate_server.chat.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
/**
 * Redis 채널 구독 -> Pub/Sub 채널을 구독하여 메시지를 수신
 * Redis 채널에 발행된 이벤트를 수신하여 WebSocket으로 전달
 */
public class ChatMessageListener implements MessageListener {
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final WebSocketDestinationResolver webSocketDestinationResolver;

    /**
     * Redis 채널에서 메시지 수신
     * 채널 유형에 따라 적절한 처리
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String payload = new String(message.getBody());
            log.debug("Received Redis message on channel: {}", channel);

            if (channel.startsWith("chat:room:")) {
                handleChatRoomMessage(channel, payload);
            } else if (channel.startsWith("user:status:")) {
                handleUserStatusMessage(channel, payload);
            }
        } catch (Exception e) {
            log.error("Error processing Redis message", e);
        }
    }

    /**
     * 채팅방 메시지: WebSocket을 통해 클라이언트에게 전달
     * 이벤트 타입에 따라 적절한 목적지로 라우팅 실행
     */
    private void handleChatRoomMessage(String channel, String payload) throws JsonProcessingException {
        String roomId = channel.split(":")[2];

        try {
            JsonNode eventNode = parseJsonSafely(payload);

            if (eventNode.has("type")) {
                String eventType = eventNode.get("type").asText();
                String destination = webSocketDestinationResolver.getDestinationByEventType(roomId, eventType);
                String data = objectMapper.writeValueAsString(eventNode.get("data"));
                messagingTemplate.convertAndSend(destination, data);

            } else {
                log.info("No event type found, sending to main channel: /topic/chat.room.{}", roomId);
                messagingTemplate.convertAndSend("/topic/chat.room." + roomId, payload);
            }
        } catch (Exception e) {
            log.error("Error handling chat room message: {}", e.getMessage());
        }
    }

    private JsonNode parseJsonSafely(String payload) throws JsonProcessingException {
        JsonNode eventNode = objectMapper.readTree(payload);

        // 이중 직렬화 문제 해결을 위한 추가 처리
        if (eventNode.isTextual()) {
            try {
                return objectMapper.readTree(eventNode.asText());
            } catch (Exception e) {
                log.warn("Failed to parse as nested JSON: {}", e.getMessage());
            }
        }

        return eventNode;
    }


    /**
     * 사용자 상태 메시지: 상태 변경 알림 전송
     */
    private void handleUserStatusMessage(String channel, String payload) {
        try {
            String userId = channel.split(":")[2];
            messagingTemplate.convertAndSend("/topic/user/" + userId + "/status", payload);
        } catch (Exception e) {
            log.error("Error handling user status message: {}", e.getMessage());
        }
    }
}
