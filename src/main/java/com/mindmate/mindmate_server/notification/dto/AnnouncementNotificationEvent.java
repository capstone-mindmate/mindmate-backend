package com.mindmate.mindmate_server.notification.dto;

import com.mindmate.mindmate_server.notification.domain.NotificationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnnouncementNotificationEvent implements NotificationEvent {
    private final Long recipientId;
    private final String announcementTitle;
    private final String announcementContent;

    @Override
    public String getTitle() {
        return announcementTitle;
    }

    @Override
    public String getContent() {
        return announcementContent;
    }

    @Override
    public NotificationType getType() {
        return NotificationType.ANNOUNCEMENT;
    }

    @Override
    public Long getRelatedEntityId() {
        return 0L;
    }
}