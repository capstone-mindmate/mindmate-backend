package com.mindmate.mindmate_server.notification.service;

import com.mindmate.mindmate_server.notification.domain.Notification;
import com.mindmate.mindmate_server.notification.domain.NotificationType;
import com.mindmate.mindmate_server.notification.dto.MatchingAcceptedNotificationEvent;
import com.mindmate.mindmate_server.notification.dto.NotificationEvent;
import com.mindmate.mindmate_server.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private FCMService fcmService;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

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
    @DisplayName("알림 이벤트 소비 - DB 저장 및 FCM 전송")
    void consume_shouldProcessNotificationEvent() {
        // given
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            try {
                java.lang.reflect.Field idField = Notification.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(notification, 1L);
            } catch (Exception e) {
                // 예외 무시
            }
            return notification;
        });

        // when
        notificationConsumer.consume(testEvent);

        // then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        verify(fcmService).sendNotification(eq(1L), eq(testEvent));

        Notification savedNotification = notificationCaptor.getValue();
        assertEquals(1L, savedNotification.getUserId());
        assertEquals(NotificationType.MATCHING_ACCEPTED, savedNotification.getType());
        assertEquals(2L, savedNotification.getRelatedEntityId());
        assertEquals("매칭 수락", savedNotification.getTitle());
        assertEquals("'테스트 매칭' 매칭이 수락되었습니다.", savedNotification.getContent());
        assertEquals(false, savedNotification.isReadNotification());
    }

    @Test
    @DisplayName("DB 저장만 하고 FCM 전송은 하지 않는 이벤트")
    void consume_shouldOnlySaveToDatabase() {
        // given
        NotificationEvent eventWithoutFCM = new NotificationEvent() {
            @Override
            public Long getRecipientId() {
                return 1L;
            }

            @Override
            public String getTitle() {
                return "테스트 제목";
            }

            @Override
            public String getContent() {
                return "테스트 내용";
            }

            @Override
            public NotificationType getType() {
                return NotificationType.ANNOUNCEMENT;
            }

            @Override
            public Long getRelatedEntityId() {
                return 3L;
            }

            @Override
            public boolean saveToDatabase() {
                return true;
            }

            @Override
            public boolean sendFCM() {
                return false;
            }
        };

        // when
        notificationConsumer.consume(eventWithoutFCM);

        // then
        verify(notificationRepository).save(any(Notification.class));
        verify(fcmService, never()).sendNotification(anyLong(), any(NotificationEvent.class));
    }

    @Test
    @DisplayName("FCM 전송만 하고 DB 저장은 하지 않는 이벤트")
    void consume_shouldOnlySendFCM() {
        // given
        NotificationEvent eventWithoutDB = new NotificationEvent() {
            @Override
            public Long getRecipientId() {
                return 1L;
            }

            @Override
            public String getTitle() {
                return "테스트 제목";
            }

            @Override
            public String getContent() {
                return "테스트 내용";
            }

            @Override
            public NotificationType getType() {
                return NotificationType.CHAT_MESSAGE;
            }

            @Override
            public Long getRelatedEntityId() {
                return 3L;
            }

            @Override
            public boolean saveToDatabase() {
                return false;
            }

            @Override
            public boolean sendFCM() {
                return true;
            }
        };

        // when
        notificationConsumer.consume(eventWithoutDB);

        // then
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(fcmService).sendNotification(eq(1L), eq(eventWithoutDB));
    }

    @Test
    @DisplayName("알림 처리 중 예외 발생")
    void consume_shouldPropagateExceptions() {
        // given
        doThrow(new RuntimeException("데이터베이스 오류"))
                .when(notificationRepository).save(any(Notification.class));

        // then
        assertThrows(RuntimeException.class, () -> {
            // when
            notificationConsumer.consume(testEvent);
        });
    }
}