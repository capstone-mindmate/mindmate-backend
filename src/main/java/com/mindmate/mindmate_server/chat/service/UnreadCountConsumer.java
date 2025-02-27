package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.user.domain.RoleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class UnreadCountConsumer {
    private final ChatRoomService chatRoomService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisKeyManager redisKeyManager;
    private final ChatPresenceService chatPresenceService;

    @KafkaListener(
            topics = "chat-message-topic",
            groupId = "unread-count-group",
            containerFactory = "chatMessageListenerContainerFactory"
    )
    public void updateUnreadCount(ConsumerRecord<String, ChatMessageEvent> record) {
        ChatMessageEvent event = record.value();
        log.info("Updating unread count for message: {}", event);

        try {
            ChatRoom chatRoom = chatRoomService.findChatRoomById(event.getRoomId());

            Long recipientId;
            if (event.getSenderRole() == RoleType.ROLE_LISTENER) {
                recipientId = chatRoom.getSpeaker().getUser().getId();
            } else {
                recipientId = chatRoom.getListener().getUser().getId();
            }

            // 오프라인 + 다른 곳을 보고 있는 경우
            if (chatPresenceService.shouldIncrementUnreadCount(recipientId, event.getRoomId())) {
                chatPresenceService.incrementUnreadCount(event.getRoomId(), recipientId);

                if (event.getSenderRole() == RoleType.ROLE_LISTENER) {
                    chatRoom.increaseUnreadCountForSpeaker();
                } else {
                    chatRoom.increaseUnreadCountForListener();
                }
            }
        } catch (Exception e) {
            log.error("Error updating unread count", e);
        }
    }
}
