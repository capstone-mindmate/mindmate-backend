package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.service.UserService;
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
    private final ChatPresenceService chatPresenceService;
    private final ChatService chatService;

    @KafkaListener(
            topics = "chat-message-topic",
            groupId = "unread-count-group",
            containerFactory = "chatMessageListenerContainerFactory"
    )
    public void updateUnreadCount(ConsumerRecord<String, ChatMessageEvent> record) {
        ChatMessageEvent event = record.value();

        try {
            ChatRoom chatRoom = chatRoomService.findChatRoomById(event.getRoomId());

            // 1. 발신자의 메시지 읽음 처리
            boolean isSenderListener = event.getSenderRole() == RoleType.ROLE_LISTENER;
            if (isSenderListener) {
                chatRoom.markAsReadForListener(event.getMessageId());
            } else {
                chatRoom.markAsReadForSpeaker(event.getMessageId());
            }

            // 2. 수신자 상태 확인
            Long recipientId = isSenderListener
                    ? chatRoom.getSpeaker().getUser().getId()
                    : chatRoom.getListener().getUser().getId();

            boolean isRecipientOnline = chatPresenceService.isUserActiveInRoom(recipientId, event.getRoomId());

            if (isRecipientOnline) {
                // 수신자가 채팅방에 있는 경우 -> 읽음 처리
                chatService.markAsRead(recipientId, event.getRoomId());
                log.info("Marked message as read for recipient {} in room {}", recipientId, event.getRoomId());

            } else {
                // 수신자가 채팅방에 없는 경우 -> 미읽음 카운트 증가
                chatPresenceService.incrementUnreadCount(event.getRoomId(), recipientId, chatRoom, event.getSenderRole());
                log.info("Incremented unread count for recipient {} in room {}", recipientId, event.getRoomId());
            }
        } catch (Exception e) {
            log.error("Error processing message event", e);
        }
    }
}
