package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.MessageType;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.chat.dto.ChatMessageNotificationEvent;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatNotificationConsumer {
    private final NotificationService notificationService;
    private final UserService userService;

    @KafkaListener(
            topics = "chat-message-topic",
            groupId = "notification-group",
            containerFactory = "chatMessageListenerContainerFactory"
    )
    public void sendNotification(ConsumerRecord<String, ChatMessageEvent> record, Acknowledgment ack) {
        ChatMessageEvent event = record.value();

        // 필터링된 메시지 처리 x
        if (event.isFiltered() || event.getMessageId() == null) {
            ack.acknowledge();
            return;
        }

        if (event.isRecipientActive()) {
            ack.acknowledge();
            return;
        }

        try {
            User sender = userService.findUserById(event.getSenderId());
            User recipient = userService.findUserById(event.getRecipientId());
            String notificationContent;

            // todo: 이모티콘은 그냥 이미지만 못 보여주나? 그리고 암호화 여부에 따라 여기서 해당 내용을 보여줄 지 아니면 그냥 일반적인 문구를 보여줄 지 결정
            if (event.getType() == MessageType.TEXT) {
                notificationContent = event.getPlainContent();
            } else if (event.getType() == MessageType.CUSTOM_FORM) {
                notificationContent = "커스텀폼 메시지가 도착했습니다.";
            } else if (event.getType() == MessageType.EMOTICON){
                notificationContent = "이모티콘이 도착했습니다.";
            } else {
                notificationContent = "새 메시지가 도착했습니다.";
            }

            ChatMessageNotificationEvent notificationEvent  = ChatMessageNotificationEvent.builder()
                    .recipientId(recipient.getId())
                    .senderId(sender.getId())
                    .senderName(sender.getProfile().getNickname())
                    .roomId(event.getRoomId())
                    .messageContent(notificationContent)
                    .messageId(event.getMessageId())
                    .build();

            notificationService.processNotification(notificationEvent);
            log.info("Notification sent to recipient {} for message {}", recipient.getId(), event.getMessageId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error sending notification", e);

            // todo: 이거를 예외처리에서 바로 하는게 맞는가?
            ack.acknowledge();
        }
    }
}
