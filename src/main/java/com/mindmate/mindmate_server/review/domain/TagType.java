package com.mindmate.mindmate_server.review.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TagType {
    LISTENER_POSITIVE("리스너 좋았던 점"),
    LISTENER_NEGATIVE("리스너 개선이 필요한 점"),
    SPEAKER_POSITIVE("스피커 좋았던 점"),
    SPEAKER_NEGATIVE("스피커 개선이 필요한 점");

    private final String description;
}
