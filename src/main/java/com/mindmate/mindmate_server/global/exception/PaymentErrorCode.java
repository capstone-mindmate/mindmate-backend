package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    FAILED_CONFIRM_PAYMENT(HttpStatus.BAD_REQUEST, "결제 승인에 실패했습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 내역을 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 상품을 찾을 수 없습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문 내역을 찾을 수 없습니다."),
    UNMATCHED_AMOUNT(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    ALREADY_PROCESSED_ORDER(HttpStatus.BAD_REQUEST, "이미 완료된 내역입니다."),
    INACTIVE_PRODUCT(HttpStatus.BAD_REQUEST, "활성화되지 않은 상품입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "조회 권한이 없습니다.");


    private final HttpStatus status;
    private final String message;
}
