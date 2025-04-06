package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰입니다.");

    private final HttpStatus status;
    private final String message;
}
