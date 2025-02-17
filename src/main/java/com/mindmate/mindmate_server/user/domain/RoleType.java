package com.mindmate.mindmate_server.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleType {
    ROLE_LISTENER("ROLE_LISTENER", "리스너"),
    ROLE_SPEAKER("ROLE_SPEAKER", "스피커"),
    ROLE_ADMIN("ROLE_MANGER", "관리자");

    private final String key;
    private final String title;
}
