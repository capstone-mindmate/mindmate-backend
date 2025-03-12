package com.mindmate.mindmate_server.matching.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchingStatus {
    REQUESTED("요청됨"),
    ACCEPTED("수락됨"),
    REJECTED("거절됨"),
    COMPLETED("완료됨"),
    CANCELED("취소됨");

    private final String title;
}

