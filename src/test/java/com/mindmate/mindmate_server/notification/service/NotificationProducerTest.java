package com.mindmate.mindmate_server.notification.service;

import com.mindmate.mindmate_server.notification.domain.NotificationType;
import com.mindmate.mindmate_server.notification.dto.MatchingAcceptedNotificationEvent;
import com.mindmate.mindmate_server.notification.dto.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationProducerTest {

    @Mock
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @InjectMocks
    private NotificationProducer notificationProducer;

    private NotificationEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = MatchingAcceptedNotificationEvent.builder()
                .recipientId(1L)
                .matchingId(2L)
                .matchingTitle("테스트 매칭")
                .build();
    }

    @Test
    @DisplayName("알림 이벤트 카프카 발행 성공")
    void send_shouldPublishEventToKafka() {
        // given
        when(kafkaTemplate.send(eq("notifications"), eq("1"), eq(testEvent)))
                .thenReturn(null);

        // when
        notificationProducer.send(testEvent);

        // then
        verify(kafkaTemplate).send(eq("notifications"), eq("1"), eq(testEvent));
    }

    @Test
    @DisplayName("알림 이벤트 카프카 발행 실패")
    void send_shouldThrowExceptionOnFailure() {
        // given
        when(kafkaTemplate.send(anyString(), anyString(), any(NotificationEvent.class)))
                .thenThrow(new RuntimeException("카프카 전송 실패"));

        // then
        assertThrows(RuntimeException.class, () -> {
            // when
            notificationProducer.send(testEvent);
        });
    }
}