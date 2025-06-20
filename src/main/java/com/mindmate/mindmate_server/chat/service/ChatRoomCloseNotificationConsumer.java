package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoomCloseType;
import com.mindmate.mindmate_server.chat.dto.ChatRoomCloseEvent;
import com.mindmate.mindmate_server.chat.dto.ChatRoomNotificationEvent;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomCloseNotificationConsumer {
    private final NotificationService notificationService;

//    @KafkaStandardRetry
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0,
                    maxDelay = 10000,
                    random = true
            ),
            dltTopicSuffix = "-chat-notification-group-dlt",
            retryTopicSuffix = "-chat-notification-group-retry",
            autoCreateTopics = "false"
    )
    @KafkaListener(
            topics = "chat-room-close-topic",
            groupId = "close-notification-group",
            containerFactory = "chatRoomCloseListenerContainerFactory"
    )
    public void sendCloseNotification(ConsumerRecord<String, ChatRoomCloseEvent> record, Acknowledgment ack) {
        ChatRoomCloseEvent event = record.value();

        try {
            ChatRoomNotificationEvent speakerEvent = ChatRoomNotificationEvent.builder()
                    .recipientId(event.getSpeakerId())
                    .chatRoomId(event.getChatRoomId())
                    .closeType(ChatRoomCloseType.ACCEPT)
                    .build();

            ChatRoomNotificationEvent listenerEvent = ChatRoomNotificationEvent.builder()
                    .recipientId(event.getListenerId())
                    .chatRoomId(event.getChatRoomId())
                    .closeType(ChatRoomCloseType.ACCEPT)
                    .build();

            notificationService.processNotification(speakerEvent);
            notificationService.processNotification(listenerEvent);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error sending close notifications: {}", e.getMessage(), e);
            throw e;
        }
    }
}
