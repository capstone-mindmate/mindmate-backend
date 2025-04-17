package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
    private final UserService userService;

    @KafkaListener(
            topics = "chat-message-topic",
            groupId = "unread-count-group",
            containerFactory = "chatMessageListenerContainerFactory"
    )
    public void updateUnreadCount(ConsumerRecord<String, ChatMessageEvent> record) {
        ChatMessageEvent event = record.value();

        if (event.isFiltered() || event.getMessageId() == null) {
            return;
        }

        try {
            ChatRoom chatRoom = chatRoomService.findChatRoomById(event.getRoomId());
            User sender = userService.findUserById(event.getSenderId());
            User recipient = userService.findUserById(event.getRecipientId());

            // 1. 발신자의 메시지 읽음 처리
            chatRoom.markAsRead(sender, event.getMessageId());

            // 2. 수신자 상태 확인
            if (event.isRecipientActive()) {
                // 수신자가 채팅방에 있는 경우 -> 읽음 처리
                chatService.markAsRead(recipient.getId(), event.getRoomId());
                log.info("Marked message as read for recipient {} in room {}", recipient.getId(), event.getRoomId());
            } else {
                // 수신자가 채팅방에 없는 경우 -> 미읽음 카운트 증가
                chatPresenceService.incrementUnreadCountInRedis(event.getRoomId(), recipient.getId());

                if (chatRoom.isListener(recipient)) {
                    chatRoom.increaseUnreadCountForListener();
                } else {
                    chatRoom.increaseUnreadCountForSpeaker();
                }

                chatRoomService.save(chatRoom);
                log.info("Incremented unread count for recipient {} in room {}", recipient.getId(), event.getRoomId());
            }
        } catch (Exception e) {
            log.error("Error processing message event", e);
        }
    }
}
