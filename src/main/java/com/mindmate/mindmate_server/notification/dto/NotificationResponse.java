package com.mindmate.mindmate_server.notification.dto;

import com.mindmate.mindmate_server.notification.domain.Notification;
import com.mindmate.mindmate_server.notification.domain.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String content;
    private NotificationType type;
    private Long relatedEntityId;
    private boolean readNotification;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .relatedEntityId(notification.getRelatedEntityId())
                .readNotification(notification.isReadNotification())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}