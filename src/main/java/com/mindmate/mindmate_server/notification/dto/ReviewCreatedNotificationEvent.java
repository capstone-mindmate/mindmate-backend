package com.mindmate.mindmate_server.notification.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewCreatedNotificationEvent implements NotificationEvent {
    private final Long recipientId;
    private final Long reviewId;
    private final String reviewerNickname;

    @Override
    public String getTitle() {
        return "새로운 리뷰";
    }

    @Override
    public String getContent() {
        return String.format("%s님이 리뷰를 작성했습니다.", reviewerNickname);
    }

    @Override
    public String getType() {
        return "REVIEW_CREATED";
    }

    @Override
    public Long getRelatedEntityId() {
        return reviewId;
    }
}