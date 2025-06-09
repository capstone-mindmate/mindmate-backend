package com.mindmate.mindmate_server.notification.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.NotificationErrorCode;
import com.mindmate.mindmate_server.notification.domain.Notification;
import com.mindmate.mindmate_server.notification.domain.NotificationType;
import com.mindmate.mindmate_server.notification.dto.NotificationEvent;
import com.mindmate.mindmate_server.notification.dto.NotificationResponse;
import com.mindmate.mindmate_server.notification.repository.NotificationRepository;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final FCMService fcmService;
    private final UserService userService;

    @Transactional
    public void processNotification(NotificationEvent event) {
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
        }

        if (event.sendFCM() && userService.isPushNotificationEnabled(event.getRecipientId())) {
            fcmService.sendNotification(event.getRecipientId(), event);
        }
    }

    public Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(NotificationResponse::from);
    }

    public List<NotificationResponse> getUserUnreadNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndReadNotificationIsFalseOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        notification.readNotification();
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications =
                notificationRepository.findByUserIdAndReadNotificationIsFalseOrderByCreatedAtDesc(userId);

        unreadNotifications.forEach(Notification::readNotification);
        notificationRepository.saveAll(unreadNotifications);
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
        }

        notificationRepository.deleteById(notificationId);
    }

    @Transactional
    public void deleteAllNotifications(Long userId) {
        notificationRepository.deleteByUserId(userId);
    }

    @Transactional
    public void sendNotificationToAllUsers(NotificationEvent eventTemplate, List<Long> userIds) {
        for (Long userId : userIds) {
            NotificationEvent personalizedEvent = createPersonalEvent(eventTemplate, userId);
            processNotification(personalizedEvent);
        }
    } // admin이 공지사항 보낼 수 있음

    private NotificationEvent createPersonalEvent(NotificationEvent template, Long userId) {
        return new NotificationEvent() {
            @Override
            public Long getRecipientId() {
                return userId;
            }

            @Override
            public String getTitle() {
                return template.getTitle();
            }

            @Override
            public String getContent() {
                return template.getContent();
            }

            @Override
            public NotificationType getType() {
                return template.getType();
            }

            @Override
            public Long getRelatedEntityId() {
                return template.getRelatedEntityId();
            }

            @Override
            public boolean saveToDatabase() {
                return template.saveToDatabase();
            }

            @Override
            public boolean sendFCM() {
                return template.sendFCM();
            }
        };
    }

}