package com.mindmate.mindmate_server.notification.dto;

import com.mindmate.mindmate_server.notification.domain.NotificationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MagazineApprovedNotificationEvent implements NotificationEvent {
    private final Long recipientId;
    private final Long magazineId;
    private final String magazineTitle;

    @Override
    public String getTitle() {
        return "매거진 승인";
    }

    @Override
    public String getContent() {
        return String.format("'%s' 매거진이 승인되었습니다.", magazineTitle);
    }

    @Override
    public NotificationType getType() {
        return NotificationType.MAGAZINE_APPROVED;
    }

    @Override
    public Long getRelatedEntityId() {
        return magazineId;
    }
}