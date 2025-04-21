package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다"),
    EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "이메일 인증이 필요합니다"),
    ADMIN_SUSPENSION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "관리자를 정지할 수 없습니다." ),
    USER_ALREADY_NOT_SUSPENDED(HttpStatus.BAD_REQUEST, "해당 사용자는 정지되지 않은 상태입니다." );

    private final HttpStatus status;
    private final String message;
}
