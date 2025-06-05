package com.mindmate.mindmate_server.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleType {
    ROLE_USER("ROLE_USER", "프로필 입력 안한 사용자"),
    ROLE_PROFILE("ROLE_PROFILE", "프로필 입력 한 사용자"),
    // todo: 해당 status일때 api 호출 제한
    ROLE_SUSPENDED("ROLE_SUSPENDED", "정지된 사용자"),
    ROLE_ADMIN("ROLE_ADMIN", "관리자");

    private final String key;
    private final String title;
}
