package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EmoticonErrorCode implements ErrorCode {
    EMOTICON_NOT_FOUND(HttpStatus.NOT_FOUND, "이모티콘을 찾을 수 없습니다."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "파일이 비어있습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 파일 형식입니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기가 너무 큽니다."),
    ALREADY_PURCHASED(HttpStatus.BAD_REQUEST, "이미 구매한 이모티콘입니다."),
    INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "포인트가 부족합니다."),
    EMOTICON_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "이모티콘에 접근 권한이 없습니다" ), 
    EMOTICON_SEND_FAILED(HttpStatus.BAD_REQUEST, "이모티콘 전송에 실패했습니다");

    private final HttpStatus status;
    private final String message;
}
