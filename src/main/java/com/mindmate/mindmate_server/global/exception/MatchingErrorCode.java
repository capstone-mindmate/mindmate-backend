package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MatchingErrorCode implements ErrorCode {
    MATCHING_NOT_FOUND(HttpStatus.NOT_FOUND, "매칭을 찾을 수 없습니다."),
    INVALID_MATCHING_STATUS(HttpStatus.FORBIDDEN, "해당 상태에서 진행할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
