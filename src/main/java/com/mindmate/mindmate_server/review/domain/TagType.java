package com.mindmate.mindmate_server.review.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TagType {
    LISTENER("리스너에 대한 평가"),
    SPEAKER("스피커에 대한 평가");

    private final String description;
}
