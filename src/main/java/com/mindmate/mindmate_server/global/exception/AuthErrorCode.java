package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    // 토큰 관리
    TOKEN_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "토큰 저장을 실패하였습니다"), // 변경됨
    TOKEN_GET_FAILED(HttpStatus.NOT_FOUND, "해당 토큰이 존재하지 않습니다"),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "존재하지 않는 토큰입니다"),
    VERIFICATION_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "인증 링크가 만료되었습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    INVALID_TOKEN_TYPE(HttpStatus.BAD_REQUEST, "잘못된 토큰 타입입니다."),
    TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "재사용된 리프레시 토큰이 감지되었습니다."),
    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "사용할 수 없는 토큰입니다" ),

    // 회원가입 유효성
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "이미 회원가입 된 이메일입니다"),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "유효하지 않은 비밀번호입니다"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "두 비밀번호가 일치하지 않습니다"),
    INVALID_ENTRANCE_TIME(HttpStatus.BAD_REQUEST, "유효하지 않은 입학 연도 정보입니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "유효하지 않은 닉네임입니다"),
    INVALID_DEPARTMENT(HttpStatus.BAD_REQUEST, "유효하지 않은 학과 정보입니다"),
    DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "중복된 닉네임입니다"),

    // 이메일 인증
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 전송을 실패하였습니다"),
    EMAIL_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "이미 인증된 이메일입니다"),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "이메일 인증이 필요합니다"),
    RESEND_TOO_FREQUENTLY(HttpStatus.TOO_MANY_REQUESTS, "이메일 재전송 요청이 너무 많습니다. 5분 뒤에 시도해주세요" ),

    // 로그인
    ACCOUNT_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "계정이 잠겼습니다. 잠금 해제까지 남은 시간: %s분"),
    REMAINING_ATTEMPTS(HttpStatus.BAD_REQUEST, "로그인 실패. 남은 시도 횟수: %d회"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "잘못된 비밀번호입니다."),

    // 인증 여부
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다");

    private final HttpStatus status;
    private final String message;
}
