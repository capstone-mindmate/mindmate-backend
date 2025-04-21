package com.mindmate.mindmate_server.notification.dto;

import com.mindmate.mindmate_server.notification.domain.NotificationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchingAcceptedNotificationEvent implements NotificationEvent {
    private final Long recipientId;
    private final Long matchingId;
    private final String matchingTitle;

    @Override
    public String getTitle() {
        return "매칭 수락";
    }

    @Override
    public String getContent() {
        return String.format("'%s' 매칭이 수락되었습니다.", matchingTitle);
    }

    @Override
    public NotificationType getType() {
        return NotificationType.MATCHING_ACCEPTED;
    }

    @Override
    public Long getRelatedEntityId() {
        return matchingId;
    }
}