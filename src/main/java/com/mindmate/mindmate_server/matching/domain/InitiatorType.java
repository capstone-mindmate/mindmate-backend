package com.mindmate.mindmate_server.matching.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InitiatorType {
    SPEAKER("상담자 요청"),
    LISTENER("리스너 요청");

    private final String title;
}