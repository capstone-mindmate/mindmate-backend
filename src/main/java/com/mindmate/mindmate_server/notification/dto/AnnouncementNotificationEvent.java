package com.mindmate.mindmate_server.notification.dto;

import com.mindmate.mindmate_server.notification.domain.NotificationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnnouncementNotificationEvent implements NotificationEvent {
    private final Long recipientId;
    private final Long announcementId;
    private final String announcementTitle;

    @Override
    public String getTitle() {
        return "새 공지사항";
    }

    @Override
    public String getContent() {
        return String.format("새 공지사항: %s", announcementTitle);
    }

    @Override
    public NotificationType getType() {
        return NotificationType.ANNOUNCEMENT;
    }

    @Override
    public Long getRelatedEntityId() {
        return announcementId;
    }
}