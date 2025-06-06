package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProfileErrorCode implements ErrorCode {
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 존재하는 닉네임입니다."),
    INVALID_AVAILABLE_TIME(HttpStatus.BAD_REQUEST, "상담 가능 시간이 올바르지 않습니다."),
    SAME_ROLE_TRANSITION(HttpStatus.BAD_REQUEST, "현재 역할과 동일한 역할로 전환할 수 없습니다."),
    INVALID_ROLE_TRANSITION(HttpStatus.BAD_REQUEST, "잘못된 역할 전환입니다."),
    PROFILE_ALREADY_EXIST(HttpStatus.CONFLICT, "이미 해당 프로필 정보가 등록되었습니다." ),
    INVALID_ROLE_TYPE(HttpStatus.BAD_REQUEST, "잘못된 역할입니다." ),
    INVALID_ENTRANCE_TIME(HttpStatus.BAD_REQUEST, "유효하지 않은 입학 연도입니다." ),
    IMAGE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드 중 오류가 발생했습니다."),
    PROFILE_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "프로필 이미지를 찾을 수 없습니다."),
    PROFILE_IMAGE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "프로필 이미지에 접근 권한이 없습니다."),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "프로필이 존재하지 않습니다."),
    COUNT_NOT_FOUND(HttpStatus.NOT_FOUND,"삭제할 상담 기록이 없습니다." ),
    INVALID_RATING(HttpStatus.NOT_FOUND,"올바르지 않은 평점입니다" );

    private final HttpStatus status;
    private final String message;
}
