package com.mindmate.mindmate_server.notification.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchingAppliedNotificationEvent implements NotificationEvent {
    private final Long recipientId;
    private final Long matchingId;
    private final String matchingTitle;
    private final String applicantNickname;

    @Override
    public String getTitle() {
        return "새로운 매칭 신청";
    }

    @Override
    public String getContent() {
        return String.format("%s님이 '%s' 매칭에 신청했습니다.", applicantNickname, matchingTitle);
    }

    @Override
    public String getType() {
        return "MATCHING_APPLIED";
    }

    @Override
    public Long getRelatedEntityId() {
        return matchingId;
    }
}