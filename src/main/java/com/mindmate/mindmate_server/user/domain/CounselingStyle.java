package com.mindmate.mindmate_server.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CounselingStyle {
    SOLUTION_FOCUSED("해결중심적"),
    EMPHATHETIC("공감형"),
    ACTIVE_LISTENING("경청형");

    private final String title;
}
