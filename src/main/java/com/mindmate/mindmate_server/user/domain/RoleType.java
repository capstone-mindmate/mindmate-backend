package com.mindmate.mindmate_server.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleType {
    ROLE_UNVERIFIED("ROLE_UNVERIFIED", "이메일 인증 안한 사용자"),
    ROLE_USER("ROLE_USER", "프로필 입력 안한 사용자"),
    ROLE_PROFILE("ROLE_PROFILE", "프로필 입력 한 사용자"),
    ROLE_ADMIN("ROLE_MANGER", "관리자");

    private final String key;
    private final String title;
}
