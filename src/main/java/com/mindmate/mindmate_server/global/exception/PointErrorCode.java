package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PointErrorCode implements ErrorCode {
    INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST,"포인트가 부족합니다"),
    INVALID_POINT_AMOUNT(HttpStatus.BAD_REQUEST, "유효하지 않은 포인트 금액입니다"),
    POINT_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "포인트 거래 내역을 찾을 수 없습니다"),
    INVALID_TRANSACTION_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 거래 유형입니다"),
    POINT_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "포인트 작업 처리 중 오류가 발생했습니다"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "동시에 포인트 작업이 발생했습니다"),
    INVALID_REASON_TYPE(HttpStatus.BAD_REQUEST,  "유효하지 않은 포인트 사유 유형입니다");

    private final HttpStatus status;
    private final String message;
}
