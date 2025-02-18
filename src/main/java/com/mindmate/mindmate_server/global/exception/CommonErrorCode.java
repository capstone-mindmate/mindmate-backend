package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    INVALID_INPUT(400, "잘못된 입력입니다"),
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다");

    private final int status;
    private final String message;
}
