package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MagazineErrorCode implements ErrorCode {
    MAGAZINE_NOT_FOUND(HttpStatus.NOT_FOUND, "매거진을 찾을 수 없습니다"),
    MAGAZINE_ACCESS_DENIED(HttpStatus.BAD_REQUEST, "해당 매거진 접근 권한이 없습니다" );

    private final HttpStatus status;
    private final String message;
}
