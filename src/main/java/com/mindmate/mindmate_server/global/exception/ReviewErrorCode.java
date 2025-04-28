package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    SELF_REVIEW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자신을 리뷰할 수 없습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 리뷰를 등록하셨습니다."),
    REPLY_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 답글을 등록하셨습니다."),
    INVALID_RATING_VALUE(HttpStatus.BAD_REQUEST, "유효하지 않은 별점입니다."),
    INVALID_REVIEW_TAGS(HttpStatus.BAD_REQUEST, "유효하지 않은 태그입니다."),
    NOT_AUTHORIZED_TO_REPLY(HttpStatus.BAD_REQUEST, "답글 작성 권한이 없습니다."),
    REVIEW_SUBMISSION_CONFLICT(HttpStatus.BAD_REQUEST, "리뷰 제츨 도중 문제가 생겼습니다.");

    private final HttpStatus status;
    private final String message;
}
