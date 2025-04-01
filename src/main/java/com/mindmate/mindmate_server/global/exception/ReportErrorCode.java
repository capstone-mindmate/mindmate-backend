package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements ErrorCode {
    DUPLICATE_REPORT(HttpStatus.BAD_REQUEST, "이미 신고한 내용입니다"),
    REPORT_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 신고가 존재하지 않습니다");

    private final HttpStatus status;
    private final String message;
}
