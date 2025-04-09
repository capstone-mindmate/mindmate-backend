package com.mindmate.mindmate_server.notification.service;

import com.mindmate.mindmate_server.notification.domain.Notification;
import com.mindmate.mindmate_server.notification.dto.NotificationEvent;
import com.mindmate.mindmate_server.notification.repository.NotificationRepository;
import com.mindmate.mindmate_server.notification.service.FCMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {
    private final NotificationRepository notificationRepository;
    private final FCMService fcmService;

    @KafkaListener(topics = "notifications")
    @Transactional
    public void consume(NotificationEvent event) {
        log.info("알림 이벤트 수신: 수신자={}, 타입={}",
                event.getRecipientId(), event.getType().name());

        try {
            if (event.saveToDatabase()) {
                Notification notification = Notification.builder()
                        .userId(event.getRecipientId())
                        .title(event.getTitle())
                        .content(event.getContent())
                        .type(event.getType())
                        .relatedEntityId(event.getRelatedEntityId())
                        .readNotification(false)
                        .build();

                notificationRepository.save(notification);
                log.debug("알림 DB 저장 완료: 타입={}, 수신자={}",
                        event.getType().name(), event.getRecipientId());
            }

            if (event.sendFCM()) {
                fcmService.sendNotification(event.getRecipientId(), event);
                log.debug("FCM 알림 전송 완료: 수신자={}", event.getRecipientId());
            }
        } catch (Exception e) {
            log.error("알림 처리 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }
}