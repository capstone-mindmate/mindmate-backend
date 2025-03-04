package com.mindmate.mindmate_server.matching.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ListenerStatus {
    AVAILABLE("상담 가능"),
    BUSY("상담 중"),
    AWAY("자리 비움"),
    OFFLINE("오프라인");

    private final String title;
}