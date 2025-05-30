package com.mindmate.mindmate_server.matching.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchingType {
    AUTO_RANDOM("랜덤 매칭"),
    MANUAL("수동 매칭");

    private final String title;
}