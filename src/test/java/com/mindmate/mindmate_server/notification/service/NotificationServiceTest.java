package com.mindmate.mindmate_server.notification.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.notification.domain.Notification;
import com.mindmate.mindmate_server.notification.domain.NotificationType;
import com.mindmate.mindmate_server.notification.dto.MatchingAcceptedNotificationEvent;
import com.mindmate.mindmate_server.notification.dto.NotificationEvent;
import com.mindmate.mindmate_server.notification.repository.NotificationRepository;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private FCMService fcmService;

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    private NotificationEvent testEvent;
    private Notification testNotification;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        testEvent = MatchingAcceptedNotificationEvent.builder()
                .recipientId(userId)
                .matchingId(2L)
                .matchingTitle("테스트 매칭")
                .build();

        testNotification = Notification.builder()
                .userId(userId)
                .title("매칭 수락")
                .content("'테스트 매칭' 매칭이 수락되었습니다.")
                .type(NotificationType.MATCHING_ACCEPTED)
                .relatedEntityId(2L)
                .readNotification(false)
                .build();

        try {
            java.lang.reflect.Field idField = Notification.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testNotification, 1L);
        } catch (Exception e) {
        }
    }

    @Test
    @DisplayName("알림 처리 - DB 저장 및 FCM 전송")
    void processNotification_shouldSaveAndSendFCM() {
        when(userService.isPushNotificationEnabled(eq(userId))).thenReturn(true);

        notificationService.processNotification(testEvent);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        verify(fcmService).sendNotification(eq(userId), eq(testEvent));

        Notification savedNotification = notificationCaptor.getValue();
        assertEquals(userId, savedNotification.getUserId());
        assertEquals(NotificationType.MATCHING_ACCEPTED, savedNotification.getType());
        assertEquals("매칭 수락", savedNotification.getTitle());
        assertEquals("'테스트 매칭' 매칭이 수락되었습니다.", savedNotification.getContent());
        assertEquals(2L, savedNotification.getRelatedEntityId());
        assertFalse(savedNotification.isReadNotification());
    }

    @Test
    @DisplayName("사용자별 알림 조회")
    void getUserNotifications_shouldReturnUserNotifications() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Arrays.asList(testNotification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, pageable, notifications.size());

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), eq(pageable)))
                .thenReturn(notificationPage);

        Page<?> result = notificationService.getUserNotifications(userId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(eq(userId), eq(pageable));
    }

    @Test
    @DisplayName("읽지 않은 알림 조회")
    void getUserUnreadNotifications_shouldReturnUnreadNotifications() {
        List<Notification> notifications = Arrays.asList(testNotification);

        when(notificationRepository.findByUserIdAndReadNotificationIsFalseOrderByCreatedAtDesc(eq(userId)))
                .thenReturn(notifications);

        List<?> result = notificationService.getUserUnreadNotifications(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationRepository).findByUserIdAndReadNotificationIsFalseOrderByCreatedAtDesc(eq(userId));
    }

    @Test
    @DisplayName("알림 읽음 표시")
    void markAsRead_shouldMarkNotificationAsRead() {
        Long notificationId = 1L;

        when(notificationRepository.findById(eq(notificationId)))
                .thenReturn(Optional.of(testNotification));

        notificationService.markAsRead(notificationId);

        assertTrue(testNotification.isReadNotification());
        verify(notificationRepository).findById(eq(notificationId));
    }

    @Test
    @DisplayName("모든 알림 읽음 표시")
    void markAllAsRead_shouldMarkAllNotificationsAsRead() {
        List<Notification> notifications = Arrays.asList(testNotification);

        when(notificationRepository.findByUserIdAndReadNotificationIsFalseOrderByCreatedAtDesc(eq(userId)))
                .thenReturn(notifications);

        notificationService.markAllAsRead(userId);

        assertTrue(testNotification.isReadNotification());
        verify(notificationRepository).findByUserIdAndReadNotificationIsFalseOrderByCreatedAtDesc(eq(userId));
        verify(notificationRepository).saveAll(eq(notifications));
    }

    @Test
    @DisplayName("모든 사용자에게 알림 전송")
    void sendNotificationToAllUsers_shouldSendToAllUsers() {
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        when(userService.isPushNotificationEnabled(any(Long.class))).thenReturn(true);

        notificationService.sendNotificationToAllUsers(testEvent, userIds);

        verify(notificationRepository, times(3)).save(any(Notification.class));
        verify(fcmService, times(3)).sendNotification(any(Long.class), any(NotificationEvent.class));
    }

    @Test
    @DisplayName("알림 삭제")
    void deleteNotification_shouldDeleteNotification() {
        Long notificationId = 1L;
        when(notificationRepository.existsById(eq(notificationId))).thenReturn(true);

        notificationService.deleteNotification(notificationId);

        verify(notificationRepository).existsById(eq(notificationId));
        verify(notificationRepository).deleteById(eq(notificationId));
    }

    @Test
    @DisplayName("알림 삭제 - 존재하지 않는 알림")
    void deleteNotification_shouldThrowExceptionForNonExistingNotification() {
        Long notificationId = 999L;
        when(notificationRepository.existsById(eq(notificationId))).thenReturn(false);

        assertThrows(CustomException.class, () -> {
            notificationService.deleteNotification(notificationId);
        });

        verify(notificationRepository).existsById(eq(notificationId));
        verify(notificationRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("사용자의 모든 알림 삭제")
    void deleteAllNotifications_shouldDeleteAllUserNotifications() {
        Long userId = 1L;

        notificationService.deleteAllNotifications(userId);

        verify(notificationRepository).deleteByUserId(eq(userId));
    }

    @Test
    @DisplayName("알림 처리 - FCM 전송하지 않고 DB만 저장하는 이벤트")
    void processNotification_shouldOnlySaveWhenSendFCMIsFalse() {
        // given
        NotificationEvent event = new NotificationEvent() {
            @Override
            public Long getRecipientId() {
                return userId;
            }

            @Override
            public String getTitle() {
                return "DB 전용 알림";
            }

            @Override
            public String getContent() {
                return "FCM 전송 안됨";
            }

            @Override
            public NotificationType getType() {
                return NotificationType.ANNOUNCEMENT;
            }

            @Override
            public Long getRelatedEntityId() {
                return 4L;
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
        notificationService.processNotification(event);

        // then
        verify(notificationRepository).save(any(Notification.class));
        verify(fcmService, never()).sendNotification(any(Long.class), any(NotificationEvent.class));
    }

    @Test
    @DisplayName("알림 처리 - 푸시 알림이 비활성화된 사용자")
    void processNotification_shouldNotSendFCMWhenPushDisabled() {
        // given
        when(userService.isPushNotificationEnabled(eq(userId))).thenReturn(false);

        // when
        notificationService.processNotification(testEvent);

        // then
        verify(notificationRepository).save(any(Notification.class));
        verify(fcmService, never()).sendNotification(eq(userId), eq(testEvent));
    }

    @Test
    @DisplayName("모든 알림 읽음 표시 - 읽지 않은 알림이 없는 경우")
    void markAllAsRead_shouldHandleNoUnreadNotifications() {
        // given
        when(notificationRepository.findByUserIdAndReadNotificationIsFalseOrderByCreatedAtDesc(eq(userId)))
                .thenReturn(Collections.emptyList());

        // when
        notificationService.markAllAsRead(userId);

        // then
        verify(notificationRepository).findByUserIdAndReadNotificationIsFalseOrderByCreatedAtDesc(eq(userId));
        verify(notificationRepository).saveAll(eq(Collections.emptyList()));
    }

    @Test
    @DisplayName("모든 사용자에게 알림 전송 - 사용자 목록이 비어있는 경우")
    void sendNotificationToAllUsers_shouldHandleEmptyUserList() {
        // given
        List<Long> emptyUserIds = Collections.emptyList();

        // when
        notificationService.sendNotificationToAllUsers(testEvent, emptyUserIds);

        // then
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(fcmService, never()).sendNotification(any(Long.class), any(NotificationEvent.class));
    }

    @Test
    @DisplayName("모든 사용자에게 알림 전송 - 개인화된 이벤트 생성 확인")
    void sendNotificationToAllUsers_shouldCreatePersonalizedEvents() {
        // given
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

        // when
        notificationService.sendNotificationToAllUsers(testEvent, userIds);

        // then
        verify(notificationRepository, times(3)).save(notificationCaptor.capture());

        List<Notification> savedNotifications = notificationCaptor.getAllValues();
        assertEquals(3, savedNotifications.size());

        assertEquals(1L, savedNotifications.get(0).getUserId());
        assertEquals(2L, savedNotifications.get(1).getUserId());
        assertEquals(3L, savedNotifications.get(2).getUserId());

        for (Notification notification : savedNotifications) {
            assertEquals(testEvent.getTitle(), notification.getTitle());
            assertEquals(testEvent.getContent(), notification.getContent());
            assertEquals(testEvent.getType(), notification.getType());
            assertEquals(testEvent.getRelatedEntityId(), notification.getRelatedEntityId());
        }
    }
}