package com.mindmate.mindmate_server.notification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public enum NotificationType {
    MATCHING_ACCEPTED("매칭 수락"),
    MATCHING_APPLIED("새로운 매칭 신청"),
    MAGAZINE_APPROVED("매거진 승인"),
    MAGAZINE_REJECTED("매거진 반려"),
    REVIEW_CREATED("새로운 리뷰"),
    CHAT_MESSAGE("새 메시지"),
    ANNOUNCEMENT("공지사항"),
    CHAT_CLOSED("채팅 종료");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}