package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FilteringErrorCode implements ErrorCode{
    DUPLICATE_FILTERING_WORD(HttpStatus.BAD_REQUEST, "이미 존재하는 필터링 단어입니다."),
    FILTERING_WORD_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 필터링 단어입니다."),
    TOAST_BOX_KEYWORD_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 토스트 박스 키워드입니다."),
    DUPLICATE_TOAST_BOX_KEYWORD(HttpStatus.BAD_REQUEST, "이미 존재하는 토스트 박스 키워드입니다." );


    private final HttpStatus status;
    private final String message;
}
