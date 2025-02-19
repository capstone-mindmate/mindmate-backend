package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    TOKEN_SAVE_FAILED(HttpStatus.BAD_REQUEST, "토큰 저장을 실패하였습니다"),
    TOKEN_GET_FAILED(HttpStatus.NOT_FOUND, "해당 토큰이 존재하지 않습니다");

    private final HttpStatus status;
    private final String message;


}
