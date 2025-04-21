package com.mindmate.mindmate_server.notification.dto;

import com.mindmate.mindmate_server.notification.domain.NotificationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewCreatedNotificationEvent implements NotificationEvent {
    private final Long recipientId;
    private final Long reviewId;
    private final String reviewerName;

    @Override
    public String getTitle() {
        return "새로운 리뷰";
    }

    @Override
    public String getContent() {
        return String.format("'%s'님이 리뷰를 작성했습니다.", reviewerName);
    }

    @Override
    public NotificationType getType() {
        return NotificationType.REVIEW_CREATED;
    }

    @Override
    public Long getRelatedEntityId() {
        return reviewId;
    }
}