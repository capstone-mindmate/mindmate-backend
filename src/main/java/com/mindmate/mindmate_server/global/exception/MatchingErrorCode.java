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
    REFUSE_LIMIT_EXCEED(HttpStatus.BAD_REQUEST, "하루 최대 최소/거절 횟수를 초과하였습니다."),
    MATCHING_LIMIT_EXCEED(HttpStatus.BAD_REQUEST, "활성화된 매칭/채팅 수를 초과하였습니다."),
    DAILY_LIMIT_CANCEL_EXCEED(HttpStatus.BAD_REQUEST, "하루 최대 취소 횟수를 초과하였습니다."),
    CANNOT_APPLY_TO_OWN_MATCHING(HttpStatus.BAD_REQUEST, "자신의 매칭방에는 신청할 수 없습니다."),
    MATCHING_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "이미 닫혀있거나 매칭 완료된 매칭방입니다."),
    NO_MATCHING_AVAILABLE(HttpStatus.BAD_REQUEST, "조건에 맞는 매칭이 없습니다."),
    NOT_MATCHING_OWNER(HttpStatus.FORBIDDEN, "매칭방의 소유자가 아닙니다."),
    NOT_WAITING_OWNER(HttpStatus.FORBIDDEN, "매칭방의 신청자가 아닙니다."),
    WAITING_NOT_FOUND(HttpStatus.NOT_FOUND, "매칭 신청을 찾을 수 없습니다."),
    AUTO_MATCHING_FAILED(HttpStatus.NOT_FOUND, "자동 매칭에 실패했습니다."),
    CANNOT_CANCEL_PROCESSED_WAITING(HttpStatus.NOT_FOUND, "처리된 신청에 대해 취소할 수 없습니다."),
    INVALID_MATCHING_WAITING(HttpStatus.BAD_REQUEST, "유효하지 않은 매칭 신청입니다."),
    INSUFFICIENT_POINTS_FOR_MATCHING(HttpStatus.BAD_REQUEST, "매칭 성사에 필요한 포인트가 부족합니다."),


    ALREADY_APPLIED_TO_MATCHING(HttpStatus.BAD_REQUEST, "이미 해당 매칭방에 신청하였습니다."),;

    private final HttpStatus status;
    private final String message;
}
