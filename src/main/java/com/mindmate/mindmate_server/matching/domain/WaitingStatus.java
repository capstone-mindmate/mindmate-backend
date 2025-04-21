package com.mindmate.mindmate_server.matching.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WaitingStatus {

    PENDING("대기중"),
    ACCEPTED("수락됨"),
    REJECTED("거절됨");

    private final String title;
}
