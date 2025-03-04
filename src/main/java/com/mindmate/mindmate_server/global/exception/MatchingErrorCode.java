package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MatchingErrorCode implements ErrorCode {
    MATCHING_NOT_FOUND(HttpStatus.NOT_FOUND, "매칭을 찾을 수 없습니다."),
    INVALID_MATCHING_STATUS(HttpStatus.BAD_REQUEST, "해당 상태에서 진행할 수 없습니다."),
    INVALID_WAITING_QUEUE(HttpStatus.BAD_REQUEST, "하나의 프로필로만 대기 가능합니다."),
    USER_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "해당 사용자와의 상담이 불가능합니다."),
    USER_NOT_AUTHORIZED(HttpStatus.BAD_REQUEST, "해당 매칭의 사용자가 아닙니다."),
    LIMIT_EXCEED(HttpStatus.BAD_REQUEST, "하루 최대 최소/거절 횟수를 초과하였습니다.");

    private final HttpStatus status;
    private final String message;
}
