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
        log.info("Processing message for room {}: {}", roomId, payload);

        try {
            JsonNode eventNode = objectMapper.readTree(payload);
            // 이중 직렬화 문제 해결을 위한 추가 처리
            if (eventNode.isTextual()) {
                // 문자열인 경우 한 번 더 파싱 시도
                try {
                    eventNode = objectMapper.readTree(eventNode.asText());
                    log.info("Successfully parsed nested JSON: {}", eventNode);
                } catch (Exception e) {
                    log.warn("Failed to parse as nested JSON, proceeding with original: {}", e.getMessage());
                }
            }

            if (eventNode.has("type")) {
                String eventType = eventNode.get("type").asText();
                log.info("Event type detected: {}", eventType);

                switch (eventType) {
                    case "READ_STATUS":
                        messagingTemplate.convertAndSend(
                                "/topic/chat.room." + roomId + ".read",
                                objectMapper.writeValueAsString(eventNode.get("data"))
                        );
                        break;
                    case "REACTION":
                        messagingTemplate.convertAndSend(
                                "/topic/chat.room." + roomId + ".reaction",
                                objectMapper.writeValueAsString(eventNode.get("data"))
                        );
                        break;
                    case "CUSTOM_FORM":
                    case "CUSTOM_FORM_RESPONSE":
                        messagingTemplate.convertAndSend(
                                "/topic/chat.room." + roomId + ".customform",
                                objectMapper.writeValueAsString(eventNode.get("data"))
                        );
                        break;
                    case "MESSAGE":
                        messagingTemplate.convertAndSend(
                                "/topic/chat.room." + roomId,
                                objectMapper.writeValueAsString(eventNode.get("data"))
                        );
                        break;
                    default:
                        messagingTemplate.convertAndSend("/topic/chat.room." + roomId, payload);
                        break;
                }
            } else {
                log.info("No event type found, sending to main channel: /topic/chat.room.{}", roomId);
                messagingTemplate.convertAndSend("/topic/chat.room." + roomId, payload);
            }
        } catch (Exception e) {
            log.error("Error parsing message: {}", e.getMessage());
            messagingTemplate.convertAndSend("/topic/chat.room." + roomId, payload);
        }
    }




    /**
     * 사용자 상태 메시지: 상태 변경 알림 전송
     */
    private void handleUserStatusMessage(String channel, String payload) throws JsonProcessingException {
        String userId = channel.split(":")[2];

        // 사용자 상태 변경 알림
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/status", payload);
    }
}
