package com.mindmate.mindmate_server.notification.dto;

import com.mindmate.mindmate_server.notification.domain.NotificationType;

public interface NotificationEvent {
    Long getRecipientId();
    String getTitle();
    String getContent();
    NotificationType getType();
    Long getRelatedEntityId();

    default boolean saveToDatabase() {
        return true;
    }

    default boolean sendFCM() {
        return true;
    }
}