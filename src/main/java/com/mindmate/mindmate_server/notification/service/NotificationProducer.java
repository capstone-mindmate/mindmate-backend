package com.mindmate.mindmate_server.notification.service;

import com.mindmate.mindmate_server.notification.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public void send(NotificationEvent event) {
        try { // 수신자 id가 키
            kafkaTemplate.send("notifications",
                    event.getRecipientId().toString(), event);

            log.info("알림 이벤트 발행: 수신자={}, 타입={}",
                    event.getRecipientId(), event.getType());
        } catch (Exception e) {
            log.error("알림 이벤트 발행 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
}