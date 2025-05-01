package com.mindmate.mindmate_server.matching.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchingStatus {
    OPEN("시작됨"),
    MATCHED("매칭됨"),
    CANCELED("취소됨");

    private final String title;
}

