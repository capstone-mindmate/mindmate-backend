package com.mindmate.mindmate_server.notification.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
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

        try { // 실제로는 그냥 생성됨
            java.lang.reflect.Field idField = Notification.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testNotification, 1L);
        } catch (Exception e) {
            // 예외
        }
    }

    @Test
    @DisplayName("알림 처리 - DB 저장 및 FCM 전송")
    void processNotification_shouldSaveAndSendFCM() {
        // when
        notificationService.processNotification(testEvent);

        // then
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
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Arrays.asList(testNotification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, pageable, notifications.size());

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), eq(pageable)))
                .thenReturn(notificationPage);

        // when
        Page<?> result = notificationService.getUserNotifications(userId, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(eq(userId), eq(pageable));
    }

    @Test
    @DisplayName("읽지 않은 알림 조회")
    void getUserUnreadNotifications_shouldReturnUnreadNotifications() {
        // given
        List<Notification> notifications = Arrays.asList(testNotification);

        when(notificationRepository.findByUserIdAndReadNotificationIsFalseOrderByCreatedAtDesc(eq(userId)))
                .thenReturn(notifications);

        // when
        List<?> result = notificationService.getUserUnreadNotifications(userId);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationRepository).findByUserIdAndReadNotificationIsFalseOrderByCreatedAtDesc(eq(userId));
    }

    @Test
    @DisplayName("알림 읽음 표시")
    void markAsRead_shouldMarkNotificationAsRead() {
        // given
        Long notificationId = 1L;

        when(notificationRepository.findById(eq(notificationId)))
                .thenReturn(Optional.of(testNotification));

        // when
        notificationService.markAsRead(notificationId);

        // then
        assertTrue(testNotification.isReadNotification());
        verify(notificationRepository).findById(eq(notificationId));
    }

    @Test
    @DisplayName("모든 알림 읽음 표시")
    void markAllAsRead_shouldMarkAllNotificationsAsRead() {
        // given
        List<Notification> notifications = Arrays.asList(testNotification);

        when(notificationRepository.findByUserIdAndReadNotificationIsFalseOrderByCreatedAtDesc(eq(userId)))
                .thenReturn(notifications);

        // when
        notificationService.markAllAsRead(userId);

        // then
        assertTrue(testNotification.isReadNotification());
        verify(notificationRepository).findByUserIdAndReadNotificationIsFalseOrderByCreatedAtDesc(eq(userId));
        verify(notificationRepository).saveAll(eq(notifications));
    }

    @Test
    @DisplayName("모든 사용자에게 알림 전송")
    void sendNotificationToAllUsers_shouldSendToAllUsers() {
        // given
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);

        // when
        notificationService.sendNotificationToAllUsers(testEvent, userIds);

        // then
        // 각 사용자에 대해 processNotification이 호출되는지 확인
        verify(notificationRepository, times(3)).save(any(Notification.class));
        verify(fcmService, times(3)).sendNotification(any(Long.class), any(NotificationEvent.class));
    }

    @Test
    @DisplayName("알림 삭제")
    void deleteNotification_shouldDeleteNotification() {
        // given
        Long notificationId = 1L;
        when(notificationRepository.existsById(eq(notificationId))).thenReturn(true);

        // when
        notificationService.deleteNotification(notificationId);

        // then
        verify(notificationRepository).existsById(eq(notificationId));
        verify(notificationRepository).deleteById(eq(notificationId));
    }

    @Test
    @DisplayName("알림 삭제 - 존재하지 않는 알림")
    void deleteNotification_shouldThrowExceptionForNonExistingNotification() {
        // given
        Long notificationId = 999L;
        when(notificationRepository.existsById(eq(notificationId))).thenReturn(false);

        // then
        assertThrows(CustomException.class, () -> {
            // when
            notificationService.deleteNotification(notificationId);
        });

        verify(notificationRepository).existsById(eq(notificationId));
        verify(notificationRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("사용자의 모든 알림 삭제")
    void deleteAllNotifications_shouldDeleteAllUserNotifications() {
        // given
        Long userId = 1L;

        // when
        notificationService.deleteAllNotifications(userId);

        // then
        verify(notificationRepository).deleteByUserId(eq(userId));
    }
}