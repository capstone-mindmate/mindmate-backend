package com.mindmate.mindmate_server.matching.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InitiatorType {
    SPEAKER("스피커"),
    LISTENER("리스너");

    private final String title;
}