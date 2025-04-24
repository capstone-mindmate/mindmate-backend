package com.mindmate.mindmate_server.emoticon.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmoticonType {
    DEFAULT("기본 제공"),
    PURCHASED("구매"),
    CREATED("사용자 제작");

    private final String description;
}
