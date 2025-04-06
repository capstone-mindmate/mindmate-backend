package com.mindmate.mindmate_server.notification.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.NotificationErrorCode;
import com.mindmate.mindmate_server.notification.domain.Notification;
import com.mindmate.mindmate_server.notification.dto.NotificationEvent;
import com.mindmate.mindmate_server.notification.dto.NotificationResponse;
import com.mindmate.mindmate_server.notification.repository.NotificationRepository;
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
    private final FCMService fcmService;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void processNotification(NotificationEvent event) {
        Notification notification = Notification.builder()
                .userId(event.getRecipientId())
                .title(event.getTitle())
                .content(event.getContent())
                .type(event.getType())
                .relatedEntityId(event.getRelatedEntityId())
                .read(false)
                .build();

        notificationRepository.save(notification);

        fcmService.sendNotification(event.getRecipientId(), event);
    }

    public Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(NotificationResponse::from);
    }

    public List<NotificationResponse> getUserUnreadNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndReadIsFalseOrderByCreatedAtDesc(userId);
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
                notificationRepository.findByUserIdAndReadIsFalseOrderByCreatedAtDesc(userId);

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

}