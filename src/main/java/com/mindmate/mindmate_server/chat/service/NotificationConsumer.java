package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.user.domain.RoleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class NotificationConsumer {
//    private final ChatRoomService chatRoomService;
//    private final RedisTemplate<String, Object> redisTemplate;
//    private final FCMService fcmService;
//
//    // todo : 현재는 온라인/오프라인 여부에 따라 알림 결정하는데 현재 채팅을 보냐 안보냐에 따라 결정해야 할 듯
//    @KafkaListener(
//            topics = "chat-message-topic",
//            groupId = "notification-group",
//            containerFactory = "chatMessageListenerContainerFactory"
//    )
//    public void sendNotification(ConsumerRecord<String, ChatMessageEvent> record) {
//        ChatMessageEvent event = record.value();
//        log.info("Processing notification for message: {}", event);
//
//        try {
//            ChatRoom chatRoom = chatRoomService.findChatRoomById(event.getRoomId());
//            Long recipientId;
//            String recipientNickname;
//
//            // 수신자 데이터
//            if (event.getSenderRole() == RoleType.ROLE_LISTENER) {
//                recipientId = chatRoom.getSpeaker().getUser().getId();
//                recipientNickname = chatRoom.getSpeaker().getNickname();
//            } else {
//                recipientId = chatRoom.getListener().getUser().getId();
//                recipientNickname = chatRoom.getListener().getNickname();
//            }
//
//            // 수신자 상태 확인
//            String statusKey = "user:status:" + recipientId;
//            Boolean isOnline = (Boolean) redisTemplate.opsForHash().get(statusKey, "online");
//
//            if (isOnline == null || !isOnline) {
//                // todo : fcm 토큰 조회 -> 사용자 정보에서 가져오기
//                String fcmToken = getFCMToken(recipientId);
//
//                if (fcmToken != null) {
//                    Map<String, String> data = new HashMap<>();
//                    data.put("roomId", event.getRoomId().toString());
//                    data.put("senderId", event.getSenderId().toString());
//                    data.put("senderRole", event.getSenderRole().toString());
//                    data.put("type", event.getType().toString());
//
//                    String messagePreview = event.getContent().length() > 30
//                            ? event.getContent().substring(0, 27)
//                            : event.getContent();
//
//                    fcmService.sendNotification(
//                            fcmToken,
//                            "새 메시지가 도착했습니다",
//                            messagePreview,
//                            data
//                    );
//                    log.info("Push notification sent to user: {}", recipientId);
//                } else {
//                    log.warn("FCM token not found for user: {}", recipientId);
//                }
//            }
//        } catch (Exception e) {
//            log.error("Error sending notification", e);
//        }
//    }
//
//    private String getFCMToken(Long userId) {
//        String tokenKey = "user:fcm:" + userId;
//        return (String) redisTemplate.opsForValue().get(tokenKey);
//    }
//}
