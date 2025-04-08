package com.mindmate.mindmate_server.notification.dto;

public interface NotificationEvent {
    Long getRecipientId();
    String getTitle();
    String getContent();
    String getType();
    Long getRelatedEntityId();
}