package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CustomFormErrorCode implements ErrorCode {
    CUSTOM_FORM_NOT_FOUND(HttpStatus.NOT_FOUND, "커스텀 폼을 찾을 수 없습니다"),
    CUSTOM_FORM_ALREADY_ANSWERED(HttpStatus.BAD_REQUEST, "이미 작성된 커스텀 폼입니다" ),
    CUSTOM_FORM_INVALID_RESPONDER(HttpStatus.BAD_REQUEST, "요청받은 사용자만 커스텀  폼을 작성할 수 있습니다");

    private final HttpStatus status;
    private final String message;
}
