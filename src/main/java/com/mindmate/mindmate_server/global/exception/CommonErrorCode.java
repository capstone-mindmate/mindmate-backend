package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "서버 내부 오류가 발생했습니다"),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 파일 형식입니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기가 너무 큽니다."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "파일이 비어있습니다.");

    private final HttpStatus status;
    private final String message;
}
