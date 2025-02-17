package com.mindmate.mindmate_server.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CounselingField {
    RELATIONSHIP("대인관계"),
    FAMILY("가족"),
    CAREER("진로/직장"),
    MENTAL_HEALTH("정신건강"),
    ADDICTION("중독"),
    TRAUMA("트라우마"),
    SELF_GROWTH("자기계발");

    private final String title;
}
