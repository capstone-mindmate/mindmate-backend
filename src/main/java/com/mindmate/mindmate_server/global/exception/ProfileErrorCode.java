package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProfileErrorCode implements ErrorCode {
    DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "이미 존재하는 닉네임입니다."),
    INVALID_AVAILABLE_TIME(HttpStatus.BAD_REQUEST, "상담 가능 시간이 올바르지 않습니다."),
    SAME_ROLE_TRANSITION(HttpStatus.BAD_REQUEST, "현재 역할과 동일한 역할로 전환할 수 없습니다."),
    INVALID_ROLE_TRANSITION(HttpStatus.BAD_REQUEST, "잘못된 역할 전환입니다.");

    private final HttpStatus status;
    private final String message;
}
