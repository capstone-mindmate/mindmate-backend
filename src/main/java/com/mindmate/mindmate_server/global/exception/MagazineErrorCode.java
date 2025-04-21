package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MagazineErrorCode implements ErrorCode {
    MAGAZINE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 매거진을 찾을 수 없습니다"),
    MAGAZINE_ACCESS_DENIED(HttpStatus.BAD_REQUEST, "해당 매거진 접근 권한이 없습니다" ),
    ALREADY_PUBLISHED(HttpStatus.BAD_REQUEST, "해당 매거진은 이미 발행되었습니다" ),
    MAGAZINE_IMAGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 매거진 이미지를 찾을 수 없습니다" ),
    MAGAZINE_IMAGE_ACCESS_DENIED(HttpStatus.BAD_REQUEST, "해당 매거진 이미지 처리에 권한이 없습니다" ),
    MAGAZINE_IMAGE_ALREADY_IN_USE(HttpStatus.BAD_REQUEST, "해당 이미지는 이미 다른 매거진에서 사용 중입니다." ),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "이미지를 업로드해주세요" ),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "적절하지 않은 이미지 타입입니다." ),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "이미지 크기가 너무 큽니다" ),
    TOO_MANY_IMAGES(HttpStatus.BAD_REQUEST, "이미지 개수가 너무 많습니다" );

    private final HttpStatus status;
    private final String message;
}
