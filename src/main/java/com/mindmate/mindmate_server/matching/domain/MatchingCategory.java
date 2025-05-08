package com.mindmate.mindmate_server.matching.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchingCategory {

    ACADEMIC("학업"),
    CAREER("진로"),
    RELATIONSHIP("인간관계"),
    FINANCIAL("경제"),
    EMPLOYMENT("취업"),
    OTHER("기타");

    private final String title;
}
